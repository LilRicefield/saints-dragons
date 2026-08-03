package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class DragonAiCombatPacingComponent {
    private static final int DEFAULT_GLOBAL_ACTION_LOCK_TICKS = 10;

    private int cadenceCooldownTicks = 0;
    private int majorAbilityCooldownTicks = 0;
    private int globalActionLockTicks = 0;
    private final Map<DragonAbilityType<?, ?>, Integer> abilityCooldowns = new HashMap<>();
    private final Map<DragonAbilityType<?, ?>, Integer> repeatLockouts = new HashMap<>();

    public void tick() {
        if (cadenceCooldownTicks > 0) {
            cadenceCooldownTicks--;
        }
        if (majorAbilityCooldownTicks > 0) {
            majorAbilityCooldownTicks--;
        }
        if (globalActionLockTicks > 0) {
            globalActionLockTicks--;
        }
        tickCooldownMap(abilityCooldowns);
        tickCooldownMap(repeatLockouts);
    }

    public void reset() {
        cadenceCooldownTicks = 0;
        majorAbilityCooldownTicks = 0;
        globalActionLockTicks = 0;
        abilityCooldowns.clear();
        repeatLockouts.clear();
    }

    public boolean canUse(DragonAbilityType<?, ?> abilityType) {
        return canUse(abilityType, false);
    }

    public boolean canUse(DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        if (abilityType == null) {
            return false;
        }
        if (globalActionLockTicks > 0) {
            return false;
        }
        if (cadenceCooldownTicks > 0) {
            return false;
        }
        if (majorAbility && majorAbilityCooldownTicks > 0) {
            return false;
        }
        if (abilityCooldowns.getOrDefault(abilityType, 0) > 0) {
            return false;
        }
        return repeatLockouts.getOrDefault(abilityType, 0) <= 0;
    }

    public boolean canUseMajorFollowup(DragonAbilityType<?, ?> abilityType) {
        if (abilityType == null
                || globalActionLockTicks > 0
                || cadenceCooldownTicks > 0
                || abilityCooldowns.getOrDefault(abilityType, 0) > 0) {
            return false;
        }
        return repeatLockouts.getOrDefault(abilityType, 0) <= 0;
    }

    public void recordUse(DragonAbilityType<?, ?> abilityType, int cadenceTicks, int abilityCooldownTicks) {
        recordUse(abilityType, cadenceTicks, abilityCooldownTicks, false, 0, 0);
    }

    public void recordUse(DragonAbilityType<?, ?> abilityType,
                          int cadenceTicks,
                          int abilityCooldownTicks,
                          boolean majorAbility,
                          int majorCooldownTicks,
                          int repeatLockoutTicks) {
        if (abilityType == null) {
            return;
        }
        this.cadenceCooldownTicks = Math.max(this.cadenceCooldownTicks, cadenceTicks);
        this.globalActionLockTicks = Math.max(this.globalActionLockTicks, DEFAULT_GLOBAL_ACTION_LOCK_TICKS);
        if (abilityCooldownTicks > 0) {
            this.abilityCooldowns.put(abilityType, Math.max(this.abilityCooldowns.getOrDefault(abilityType, 0), abilityCooldownTicks));
        }
        if (majorAbility) {
            this.majorAbilityCooldownTicks = Math.max(this.majorAbilityCooldownTicks, majorCooldownTicks);
        }
        if (repeatLockoutTicks > 0) {
            this.repeatLockouts.put(abilityType, Math.max(this.repeatLockouts.getOrDefault(abilityType, 0), repeatLockoutTicks));
        }
    }

    public void setCadenceCooldownMin(int ticks) {
        this.cadenceCooldownTicks = Math.max(this.cadenceCooldownTicks, ticks);
    }

    public void setGlobalActionLock(int ticks) {
        this.globalActionLockTicks = Math.max(this.globalActionLockTicks, ticks);
    }

    public int getCadenceCooldownTicks() {
        return cadenceCooldownTicks;
    }

    private static void tickCooldownMap(Map<DragonAbilityType<?, ?>, Integer> map) {
        Iterator<Map.Entry<DragonAbilityType<?, ?>, Integer>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DragonAbilityType<?, ?>, Integer> entry = iterator.next();
            int next = entry.getValue() - 1;
            if (next <= 0) {
                iterator.remove();
            } else {
                entry.setValue(next);
            }
        }
    }
}
