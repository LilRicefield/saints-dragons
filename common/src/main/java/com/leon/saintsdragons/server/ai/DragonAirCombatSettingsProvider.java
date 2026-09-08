package com.leon.saintsdragons.server.ai;

import net.minecraft.world.entity.LivingEntity;

public interface DragonAirCombatSettingsProvider {
    DragonAirCombatSettings getAiAirCombatSettings();

    /** Species-specific movement locks, shared with the planner and landing handoffs. */
    default @org.jetbrains.annotations.Nullable String getAiAirCombatBlockReason() {
        return null;
    }

    default double getAiTargetAirborneHeight(LivingEntity target) {
        return getAiAirCombatSettings().targetAirborneHeight();
    }
}
