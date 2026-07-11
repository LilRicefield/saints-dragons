package com.leon.saintsdragons.server.ai;

import net.minecraft.world.entity.LivingEntity;

public interface DragonAirCombatSettingsProvider {
    DragonAirCombatSettings getAiAirCombatSettings();

    default double getAiTargetAirborneHeight(LivingEntity target) {
        return getAiAirCombatSettings().targetAirborneHeight();
    }
}
