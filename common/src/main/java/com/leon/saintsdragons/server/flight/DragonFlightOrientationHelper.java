package com.leon.saintsdragons.server.flight;

import net.minecraft.util.Mth;

public final class DragonFlightOrientationHelper {
    private DragonFlightOrientationHelper() {
    }

    public static float normalizeRoll(float rollRadians) {
        return Mth.wrapDegrees(rollRadians * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }
}
