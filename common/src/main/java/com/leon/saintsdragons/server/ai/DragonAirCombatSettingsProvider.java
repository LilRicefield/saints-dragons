package com.leon.saintsdragons.server.ai;

import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonCombatFlightState;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public interface DragonAirCombatSettingsProvider {
    DragonAirCombatSettings getAiAirCombatSettings();

    default @Nullable DragonCombatFlightState getCombatFlightState() {
        return null;
    }

    /** Species-specific movement locks, shared with the planner and landing handoffs. */
    default @org.jetbrains.annotations.Nullable String getAiAirCombatBlockReason() {
        return null;
    }

    default double getAiTargetAirborneHeight(LivingEntity target) {
        return getAiAirCombatSettings().targetAirborneHeight();
    }
}
