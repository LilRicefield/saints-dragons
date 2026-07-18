package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public class DragonCombatHandler {
    private final DragonEntity dragon;
    
    private DragonAbility<?> activeAbility;
    private DragonAbility<?> overlayAbility;
    private int globalCooldown = 0;
    private boolean processingAbility = false;
    private final Map<DragonAbilityType<?, ?>, Integer> abilityCooldowns = new HashMap<>();
    private final Map<DragonAbilityType<?, ?>, Boolean> overlayAbilityCache = new HashMap<>();
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

    public void lockGlobalCooldown(int ticks) {
        if (ticks <= 0) {
            return;
        }
        globalCooldown = Math.max(globalCooldown, ticks);
    }

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

        if (!canUseAiAbilityAgainstCurrentTarget(abilityType)) {
            return false;
        }

        if (isOverlayAbilityType(abilityType)) {
            return overlayAbility == null || !overlayAbility.isUsing();
        }

        return globalCooldown == 0
            && (activeAbility == null || !activeAbility.isUsing());
    }

    public boolean isAbilityCooldownReady(DragonAbilityType<?, ?> abilityType) {
        return abilityCooldowns.getOrDefault(abilityType, 0) <= 0;
    }

    public void setAbilityCooldown(DragonAbilityType<?, ?> abilityType, int cooldownTicks) {
        abilityCooldowns.put(abilityType, cooldownTicks);
    }

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
                && abilityType != ModAbilities.VOLITANS_ULTIMATE) {
            return false;
        }
        if (dragon.areRiderControlsLocked()) {
            return false;
        }
        if (!canUseAiAbilityAgainstCurrentTarget(abilityType)) {
            return false;
        }
        if (!canStart(abilityType)) {
            return false;
        }

        boolean overlay = isOverlayAbilityType(abilityType);

        processingAbility = true;
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
            if (clearIfStartupAborted(ability, overlay)) {
                return false;
            }
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
        if (!canUseAiAbilityAgainstCurrentTarget(abilityType) && !isInternalStateAbility(abilityType)) {
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
            clearIfStartupAborted(ability, overlay);
        } finally {
            processingAbility = false;
        }
    }

    private boolean clearIfStartupAborted(DragonAbility<?> ability, boolean overlay) {
        if (ability.isUsing()) {
            return false;
        }
        if (overlay) {
            if (overlayAbility == ability) {
                overlayAbility = null;
            }
        } else if (activeAbility == ability) {
            setActiveAbility(null);
        }
        return true;
    }

    private boolean canUseAiAbilityAgainstCurrentTarget(DragonAbilityType<?, ?> abilityType) {
        if (abilityType == null || dragon.isVehicle()) {
            return true;
        }
        LivingEntity target = dragon.getTarget();
        if (!DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)) {
            return true;
        }
        return isBiteAbility(abilityType);
    }

    private boolean isBiteAbility(DragonAbilityType<?, ?> abilityType) {
        String name = AbilityRegistry.getName(abilityType);
        return name != null && name.toLowerCase(java.util.Locale.ROOT).contains("bite");
    }

    private boolean isInternalStateAbility(DragonAbilityType<?, ?> abilityType) {
        String name = AbilityRegistry.getName(abilityType);
        if (name == null) {
            return false;
        }
        String lowerName = name.toLowerCase(java.util.Locale.ROOT);
        return lowerName.endsWith("_hurt") || lowerName.endsWith("_die");
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

    public boolean hasActiveOverlay() {
        return overlayAbility != null && overlayAbility.isUsing();
    }
    public void clearAllStates() {
        forceEndActiveAbility();
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

    public void tick() {
        if (dragon.level().isClientSide) {
            return;
        }
        if (globalCooldown > 0) {
            globalCooldown--;
        }

        abilityCooldowns.entrySet().removeIf(entry -> {
            int newValue = entry.getValue() - 1;
            if (newValue <= 0) {
                return true;
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
            if (!dragon.isVehicle()
                    && DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, dragon.getTarget())
                    && !isBiteAbility(activeAbility.getAbilityType())
                    && !isInternalStateAbility(activeAbility.getAbilityType())) {
                forceEndActiveAbility();
                return;
            }
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
