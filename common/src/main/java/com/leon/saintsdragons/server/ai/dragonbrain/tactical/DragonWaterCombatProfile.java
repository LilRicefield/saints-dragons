package com.leon.saintsdragons.server.ai.dragonbrain.tactical;

import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public record DragonWaterCombatProfile(boolean aquatic, boolean favorsRanged,
                                       double attackRadius, double attackHeight,
                                       double biteRange, double passLength,
                                       int biteIntervalTicks) {
    public static @Nullable DragonWaterCombatProfile get(DragonEntity dragon) {
        return dragon instanceof DragonAirCombatSettingsProvider settings
                ? settings.getWaterCombatProfile() : null;
    }

    public static boolean avoidsSwimming(DragonEntity dragon) {
        DragonWaterCombatProfile profile = get(dragon);
        return profile != null && !profile.aquatic() && dragon.canFly() && !dragon.isBaby();
    }

    public static boolean prefersSwimming(DragonEntity dragon, LivingEntity target) {
        DragonWaterCombatProfile profile = get(dragon);
        return profile != null && profile.aquatic() && dragon.canSwim()
                && DragonTargetingHelper.isMovementAnchorInWater(target);
    }

    public static boolean prefersFlight(DragonEntity dragon, LivingEntity target) {
        DragonWaterCombatProfile profile = get(dragon);
        return profile != null && dragon.canFly() && !dragon.isBaby()
                && (dragon.isAerial() || !profile.aquatic())
                && DragonTargetingHelper.isMovementAnchorInWater(target);
    }
}
