package com.leon.saintsdragons.common.config.dragon;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class DragonTamingChance {
    private DragonTamingChance() {
    }

    public static double clampPercent(double configuredValue) {
        return Mth.clamp(configuredValue, 0.0D, 100.0D);
    }

    public static double probabilityFromPercent(double configuredValue) {
        return clampPercent(configuredValue) / 100.0D;
    }

    public static boolean rollPercent(RandomSource random, double configuredValue) {
        return random.nextDouble() < probabilityFromPercent(configuredValue);
    }
}
