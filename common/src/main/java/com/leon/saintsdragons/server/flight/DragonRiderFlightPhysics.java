package com.leon.saintsdragons.server.flight;

import net.minecraft.util.Mth;

public final class DragonRiderFlightPhysics {
    private static final float DIVE_START_ANGLE_DEG = 25.0f;
    private static final float DIVE_MAX_ANGLE_DEG = 90.0f;
    private static final double DIVE_MIN_SPEED_MULT = 1.0D;
    private static final double DIVE_MAX_SPEED_MULT = 2.0D;
    private static final double DIVE_MIN_ACCEL = 0.35D;
    private static final double DIVE_MAX_ACCEL = 0.40D;
    private static final double DIVE_MIN_DRAG = 0.08D;
    private static final double DIVE_MAX_DRAG = 0.03D;
    private static final float DIVE_CURVE_POWER = 2.0f;

    private DragonRiderFlightPhysics() {
    }

    public static DiveResponse computeDiveResponse(float pitchDegrees, boolean keyPitchMode) {
        if (keyPitchMode || pitchDegrees < DIVE_START_ANGLE_DEG) {
            return new DiveResponse(0.0f, DIVE_MIN_SPEED_MULT, DIVE_MIN_ACCEL, DIVE_MIN_DRAG);
        }

        float normalizedPitch = (pitchDegrees - DIVE_START_ANGLE_DEG) / (DIVE_MAX_ANGLE_DEG - DIVE_START_ANGLE_DEG);
        normalizedPitch = Mth.clamp(normalizedPitch, 0.0f, 1.0f);
        float diveIntensity = (float) Math.pow(normalizedPitch, DIVE_CURVE_POWER);

        return new DiveResponse(
                diveIntensity,
                Mth.lerp(diveIntensity, DIVE_MIN_SPEED_MULT, DIVE_MAX_SPEED_MULT),
                Mth.lerp(diveIntensity, DIVE_MIN_ACCEL, DIVE_MAX_ACCEL),
                Mth.lerp(diveIntensity, DIVE_MIN_DRAG, DIVE_MAX_DRAG)
        );
    }

    public record DiveResponse(float intensity, double speedMultiplier, double acceleration, double drag) {
    }
}
