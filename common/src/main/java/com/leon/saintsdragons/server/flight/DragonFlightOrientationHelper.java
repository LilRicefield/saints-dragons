package com.leon.saintsdragons.server.flight;

import net.minecraft.util.Mth;

public final class DragonFlightOrientationHelper {
    private DragonFlightOrientationHelper() {
    }

    public static boolean isPitchInverted(float rollRadians, float maxAutoAlignOffsetRad) {
        float uprightRoll = DragonBarrelRollHelper.getNearestUprightRoll(rollRadians);
        float uprightOffset = rollRadians - uprightRoll;
        return Math.abs(uprightOffset) > maxAutoAlignOffsetRad;
    }

    public static float orientPitch(float rawPitchRadians, float rollRadians, float maxAutoAlignOffsetRad) {
        return rawPitchRadians * getPitchOrientationFactor(rollRadians, maxAutoAlignOffsetRad);
    }

    public static float getPitchOrientationFactor(float rollRadians, float maxAutoAlignOffsetRad) {
        float uprightRoll = DragonBarrelRollHelper.getNearestUprightRoll(rollRadians);
        float uprightOffset = Math.abs(rollRadians - uprightRoll);
        float transitionHalfWidth = (float) Math.toRadians(20.0);
        float start = Math.max(0.0f, maxAutoAlignOffsetRad - transitionHalfWidth);
        float end = Math.min(Mth.PI, maxAutoAlignOffsetRad + transitionHalfWidth);

        if (uprightOffset <= start) {
            return 1.0f;
        }
        if (uprightOffset >= end) {
            return -1.0f;
        }

        float t = (uprightOffset - start) / Math.max(0.0001f, end - start);
        t = t * t * (3.0f - 2.0f * t);
        return Mth.lerp(t, 1.0f, -1.0f);
    }

    public static float normalizeRoll(float rollRadians) {
        return Mth.wrapDegrees(rollRadians * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }
}
