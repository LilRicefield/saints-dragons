package com.leon.saintsdragons.server.ai;

import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonCombatFlightState;
import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonWaterCombatProfile;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public interface DragonAirCombatSettingsProvider {
    DragonAirCombatSettings getAiAirCombatSettings();

    default @Nullable DragonWaterCombatProfile getWaterCombatProfile() {
        return null;
    }

    default @Nullable DragonCombatFlightState getCombatFlightState() {
        return null;
    }

    /** Species-specific movement locks, shared with the planner and landing handoffs. */
    default @Nullable String getAiAirCombatBlockReason() {
        return null;
    }

    default double getAiTargetAirborneHeight(LivingEntity target) {
        return getAiAirCombatSettings().targetAirborneHeight();
    }
}
