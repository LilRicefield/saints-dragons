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
        double horizontalRange = Mth.clamp(10.0D + dragon.getBbWidth() * 1.2D, 12.0D, 18.0D);
        double verticalRange = Mth.clamp(4.0D + dragon.getBbHeight() * 0.5D, 5.0D, 9.0D);
        return new DragonScentProfile(horizontalRange, verticalRange, 24, 36, 20 * 12, 20 * 20);
    }

    public double uncertainty(double distance) {
        double rangeFactor = Mth.clamp(distance / horizontalRange, 0.0D, 1.0D);
        return 1.5D + rangeFactor * 2.5D;
    }

    public float confidence(double distance) {
        double rangeFactor = Mth.clamp(distance / horizontalRange, 0.0D, 1.0D);
        return (float)(0.8D - rangeFactor * 0.4D);
    }
}
