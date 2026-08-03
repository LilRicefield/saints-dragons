package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.util.Mth;

public record DragonScentProfile(
        double horizontalRange,
        double verticalRange,
        int minAssessmentTicks,
        int maxAssessmentTicks,
        int minCooldownTicks,
        int maxCooldownTicks
) {
    public static DragonScentProfile forDragon(DragonEntity dragon) {
        double horizontalRange = Mth.clamp(18.0D + dragon.getBbWidth() * 2.0D, 20.0D, 30.0D);
        double verticalRange = Mth.clamp(6.0D + dragon.getBbHeight() * 0.75D, 8.0D, 14.0D);
        return new DragonScentProfile(horizontalRange, verticalRange, 24, 36, 20 * 12, 20 * 20);
    }

    public double uncertainty(double distance) {
        double rangeFactor = Mth.clamp(distance / horizontalRange, 0.0D, 1.0D);
        return 1.5D + rangeFactor * 4.5D;
    }

    public float confidence(double distance) {
        double rangeFactor = Mth.clamp(distance / horizontalRange, 0.0D, 1.0D);
        return (float)(0.85D - rangeFactor * 0.5D);
    }
}
