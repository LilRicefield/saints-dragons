package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.common.registry.volitans.VolitansAbilities;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

/**
 * Single responsibility: Track active ability and global cooldowns
 */
public class DragonCombatHandler {
    private final DragonEntity dragon;
    
    private DragonAbility<?> activeAbility;
    private DragonAbility<?> overlayAbility;
    private int globalCooldown = 0; // Global cooldown between any abilities
    private boolean processingAbility = false; // Prevent re-entry during ability start
    
    // Per-ability cooldown tracking
    private final Map<DragonAbilityType<?, ?>, Integer> abilityCooldowns = new HashMap<>();
    private final Map<DragonAbilityType<?, ?>, Boolean> overlayAbilityCache = new HashMap<>();

    // ===== PERSISTENCE =====
    // Persist global + per-ability cooldowns across save/load
    public void saveToNBT(CompoundTag tag) {
        tag.putInt("GlobalAbilityCooldown", Math.max(0, globalCooldown));
        CompoundTag cd = new CompoundTag();
        for (Map.Entry<DragonAbilityType<?, ?>, Integer> e : abilityCooldowns.entrySet()) {
            String name = com.leon.saintsdragons.common.registry.AbilityRegistry.getName(e.getKey());
            if (name != null && !name.isEmpty()) {
                cd.putInt(name, Math.max(0, e.getValue()));
            }
        }
        if (!cd.isEmpty()) {
            tag.put("AbilityCooldowns", cd);
        }
    }

    public void loadFromNBT(CompoundTag tag) {
        this.globalCooldown = Math.max(0, tag.getInt("GlobalAbilityCooldown"));
        this.abilityCooldowns.clear();
        if (tag.contains("AbilityCooldowns", Tag.TAG_COMPOUND)) {
            CompoundTag cd = tag.getCompound("AbilityCooldowns");
            for (String key : cd.getAllKeys()) {
                var type = AbilityRegistry.get(key);
                if (type != null) {
                    int val = Math.max(0, cd.getInt(key));
                    if (val > 0) this.abilityCooldowns.put(type, val);
                }
            }
        }
    }

    public DragonCombatHandler(DragonEntity dragon) {
        this.dragon = dragon;
    }

    public DragonAbility<?> getActiveAbility() {
        return activeAbility;
    }
    
    public void setActiveAbility(DragonAbility<?> ability) {
        this.activeAbility = ability;
        dragon.setActiveAbility(ability);
    }

    public DragonAbilityType<?, ?> getActiveAbilityType() {
        return activeAbility != null ? activeAbility.getAbilityType() : null;
    }

    public boolean canUseAbility() {
        return globalCooldown == 0
                && (activeAbility == null || !activeAbility.isUsing())
                && !processingAbility
                && !dragon.areRiderControlsLocked();
    }

    public void lockGlobalCooldown(int ticks) {
        if (ticks <= 0) {
            return;
        }
        globalCooldown = Math.max(globalCooldown, ticks);
    }

    public boolean isGlobalCooldownActive() {
        return globalCooldown > 0;
    }
    
    /**
     * Check if a specific ability type can be started (includes per-ability cooldown)
     */
    public boolean canStart(DragonAbilityType<?, ?> abilityType) {
        if (processingAbility) {
            return false;
        }

        if (globalCooldown > 0 || dragon.areRiderControlsLocked()) {
            return false;
        }

        if (!isAbilityCooldownReady(abilityType)) {
            return false;
        }

        if (isOverlayAbilityType(abilityType)) {
            return overlayAbility == null || !overlayAbility.isUsing();
        }

        return globalCooldown == 0
            && (activeAbility == null || !activeAbility.isUsing());
    }
    
    /**
     * Check if a specific ability's cooldown is ready
     */
    public boolean isAbilityCooldownReady(DragonAbilityType<?, ?> abilityType) {
        return abilityCooldowns.getOrDefault(abilityType, 0) <= 0;
    }
    
    /**
     * Set cooldown for a specific ability type
     */
    public void setAbilityCooldown(DragonAbilityType<?, ?> abilityType, int cooldownTicks) {
        abilityCooldowns.put(abilityType, cooldownTicks);
    }
    
    /**
     * Get remaining cooldown ticks for a specific ability type
     */
    public int getCooldownTicks(DragonAbilityType<?, ?> abilityType) {
        return abilityCooldowns.getOrDefault(abilityType, 0);
    }

    /**
     * Clear cooldown tracking for a specific ability type.
     */
    public void clearAbilityCooldown(DragonAbilityType<?, ?> abilityType) {
        if (abilityType == null) {
            return;
        }
        abilityCooldowns.remove(abilityType);
    }

    public boolean tryUseAbility(DragonAbilityType<?, ?> abilityType) {
        if (abilityType == null || dragon.level().isClientSide) {
            return false;
        }
        if (dragon instanceof Volitans volitans
                && (volitans.isAiSpecialCombatActive() || volitans.isAiSpecialCombatReserved())
                && abilityType != VolitansAbilities.VOLITANS_ULTIMATE) {
            return false;
        }
        if (dragon.areRiderControlsLocked()) {
            return false;
        }
        if (!canStart(abilityType)) {
            return false;
        }

        boolean overlay = isOverlayAbilityType(abilityType);

        processingAbility = true; // Guard against re-entry
        try {
            @SuppressWarnings("unchecked")
            var ability = ((DragonAbilityType<DragonEntity, ?>) abilityType).makeInstance(dragon);

            if (!ability.tryAbility()) {
                return false;
            }

            if (overlay) {
                overlayAbility = ability;
            } else {
                setActiveAbility(ability);
            }
            ability.start();
            return true;
        } finally {
            processingAbility = false;
        }
    }

    public boolean tryUseAiAbility(DragonAbilityType<?, ?> abilityType,
                                   boolean majorAbility,
                                   int cadenceTicks,
                                   int abilityCooldownTicks,
                                   int majorCooldownTicks,
                                   int repeatLockoutTicks) {
        if (!canStart(abilityType) || !dragon.getAiCombatPacing().canUse(abilityType, majorAbility)) {
            return false;
        }
        if (!tryUseAbility(abilityType)) {
            return false;
        }
        dragon.getAiCombatPacing().recordUse(
                abilityType,
                cadenceTicks,
                abilityCooldownTicks,
                majorAbility,
                majorCooldownTicks,
                repeatLockoutTicks
        );
        return true;
    }

    public void forceUseAbility(DragonAbilityType<?, ?> abilityType) {
        if (abilityType == null || dragon.level().isClientSide) {
            return;
        }

        processingAbility = true;
        try {
            forceEndActiveAbility();
            globalCooldown = 0;

            @SuppressWarnings("unchecked")
            var ability = ((DragonAbilityType<DragonEntity, ?>) abilityType).makeInstance(dragon);

            if (!ability.tryAbility()) {
                return;
            }

            boolean overlay = isOverlayAbilityType(abilityType);
            if (overlay) {
                overlayAbility = ability;
            } else {
                setActiveAbility(ability);
            }
            ability.start();
        } finally {
            processingAbility = false;
        }
    }

    public void forceEndActiveAbility() {
        if (activeAbility != null) {
            DragonAbility<?> finished = activeAbility;
            finished.interrupt();
            applyCooldownsForForcedEnd(finished, true);
            setActiveAbility(null);
        }
        if (overlayAbility != null) {
            DragonAbility<?> finishedOverlay = overlayAbility;
            finishedOverlay.interrupt();
            applyCooldownsForForcedEnd(finishedOverlay, false);
            overlayAbility = null;
        }
    }

    public void forceEndAbility(DragonAbilityType<?, ?> abilityType) {
        if (activeAbility != null && activeAbility.getAbilityType() == abilityType) {
            DragonAbility<?> finished = activeAbility;
            finished.interrupt();
            applyCooldownsForForcedEnd(finished, true);
            setActiveAbility(null);
        }
        if (overlayAbility != null && overlayAbility.getAbilityType() == abilityType) {
            DragonAbility<?> finishedOverlay = overlayAbility;
            finishedOverlay.interrupt();
            applyCooldownsForForcedEnd(finishedOverlay, false);
            overlayAbility = null;
        }
    }

    public boolean isAbilityActive(DragonAbilityType<?, ?> abilityType) {
        if (activeAbility != null && activeAbility.getAbilityType() == abilityType && activeAbility.isUsing()) {
            return true;
        }
        return overlayAbility != null && overlayAbility.getAbilityType() == abilityType && overlayAbility.isUsing();
    }

    /**
     * Check if there's an active overlay ability
     */
    public boolean hasActiveOverlay() {
        return overlayAbility != null && overlayAbility.isUsing();
    }
    
    /**
     * Clears all combat states - used when mounting or transitioning states
     */
    public void clearAllStates() {
        // End any active ability
        forceEndActiveAbility();
        
        // Clear all cooldowns
        globalCooldown = 0;
        abilityCooldowns.clear();
        processingAbility = false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean isOverlayAbilityType(DragonAbilityType<?, ?> abilityType) {
        return overlayAbilityCache.computeIfAbsent(abilityType, type -> {
            DragonAbility ability = ((DragonAbilityType) type).makeInstance(dragon);
            return ability.isOverlayAbility();
        });
    }

    private void applyCooldownsForForcedEnd(DragonAbility<?> ability, boolean applyGlobalCooldown) {
        if (ability == null) {
            return;
        }
        DragonAbilityType<?, ?> type = ability.getAbilityType();
        if (type != null) {
            setAbilityCooldown(type, ability.getCooldownTimer());
        }
        if (applyGlobalCooldown) {
            globalCooldown = Math.max(globalCooldown, ability.getInterruptRecoveryTicks());
        }
    }

    // Removed unused target validation stub

    public void tick() {
        if (dragon.level().isClientSide) {
            return;
        }
        if (globalCooldown > 0) {
            globalCooldown--;
        }
        
        // Tick down per-ability cooldowns
        abilityCooldowns.entrySet().removeIf(entry -> {
            int newValue = entry.getValue() - 1;
            if (newValue <= 0) {
                return true; // Remove from map when cooldown reaches 0
            } else {
                entry.setValue(newValue);
                return false;
            }
        });
        
        if (overlayAbility != null) {
            if (overlayAbility.isUsing()) {
                overlayAbility.tick();
            } else {
                DragonAbilityType<?, ?> overlayType = overlayAbility.getAbilityType();
                if (overlayType != null) {
                    setAbilityCooldown(overlayType, overlayAbility.getCooldownTimer());
                }
                overlayAbility = null;
            }
        }

        if (activeAbility != null) {
            if (activeAbility.isUsing()) {
                activeAbility.tick();
            } else {
                DragonAbility<?> finishedAbility = activeAbility;
                globalCooldown = Math.max(globalCooldown, finishedAbility.getRecoveryTicks());
                // Apply per-ability cooldown based on the finished ability's current cooldown
                DragonAbilityType<?, ?> finishedType = getActiveAbilityType();
                if (finishedType != null) {
                    setAbilityCooldown(finishedType, finishedAbility.getCooldownTimer());
                }
                setActiveAbility(null);
            }
        }
    }
}
