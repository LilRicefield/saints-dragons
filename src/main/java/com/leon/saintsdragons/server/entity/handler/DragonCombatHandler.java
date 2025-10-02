package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;

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
    public void saveToNBT(net.minecraft.nbt.CompoundTag tag) {
        tag.putInt("GlobalAbilityCooldown", Math.max(0, globalCooldown));
        net.minecraft.nbt.CompoundTag cd = new net.minecraft.nbt.CompoundTag();
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

    public void loadFromNBT(net.minecraft.nbt.CompoundTag tag) {
        this.globalCooldown = Math.max(0, tag.getInt("GlobalAbilityCooldown"));
        this.abilityCooldowns.clear();
        if (tag.contains("AbilityCooldowns", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            net.minecraft.nbt.CompoundTag cd = tag.getCompound("AbilityCooldowns");
            for (String key : cd.getAllKeys()) {
                var type = com.leon.saintsdragons.common.registry.AbilityRegistry.get(key);
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
    }

    public DragonAbilityType<?, ?> getActiveAbilityType() {
        return activeAbility != null ? activeAbility.getAbilityType() : null;
    }

    public boolean canUseAbility() {
        return globalCooldown == 0 && (activeAbility == null || !activeAbility.isUsing()) && !processingAbility;
    }
    
    /**
     * Check if a specific ability type can be started (includes per-ability cooldown)
     */
    public boolean canStart(DragonAbilityType<?, ?> abilityType) {
        if (processingAbility) {
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

    public void tryUseAbility(DragonAbilityType<?, ?> abilityType) {
        if (!canStart(abilityType)) {
            return;
        }

        boolean overlay = isOverlayAbilityType(abilityType);

        processingAbility = true; // Guard against re-entry
        try {
            @SuppressWarnings("unchecked")
            var ability = ((DragonAbilityType<DragonEntity, ?>) abilityType).makeInstance(dragon);

            if (!ability.tryAbility()) {
                return;
            }

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
            activeAbility.interrupt();
            activeAbility = null;
        }
        if (overlayAbility != null) {
            overlayAbility.interrupt();
            overlayAbility = null;
        }
    }

    public void forceEndAbility(DragonAbilityType<?, ?> abilityType) {
        if (activeAbility != null && activeAbility.getAbilityType() == abilityType) {
            activeAbility.interrupt();
            activeAbility = null;
        }
        if (overlayAbility != null && overlayAbility.getAbilityType() == abilityType) {
            overlayAbility.interrupt();
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

    // Removed unused target validation stub

    public void tick() {
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
                // Ability finished, set a small fixed global cooldown between abilities
                // ~0.3s between abilities
                globalCooldown = 6;
                // Apply per-ability cooldown based on the finished ability's current cooldown
                DragonAbilityType<?, ?> finishedType = getActiveAbilityType();
                if (finishedType != null) {
                    setAbilityCooldown(finishedType, activeAbility.getCooldownTimer());
                }
                activeAbility = null;
            }
        }
    }
}
