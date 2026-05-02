package com.leon.saintsdragons.fabric.client.camera;

import net.minecraft.util.Mth;

public final class CameraLeanData {
    private static final double BASELINE_FRAME_SECONDS = 1.0D / 60.0D;
    private static final double MAX_FRAME_SCALE = 4.0D;
    private static final double LEAN_SMOOTHING = 0.03D;

    private static double targetLeanX = 0.0;
    private static double targetLeanY = 0.0;
    private static double targetLeanZ = 0.0;
    private static double currentLeanX = 0.0;
    private static double currentLeanY = 0.0;
    private static double currentLeanZ = 0.0;
    private static double targetCameraTilt = 0.0;
    private static double currentCameraTilt = 0.0;
    private static long lastUpdateNanos = 0L;

    private CameraLeanData() {
    }

    public static void updateTarget(float rollDegrees, float pitchDegrees, float yawSpeed, float yawLeanScale) {
        double rollMagnitude = Math.min(Math.abs(rollDegrees) / 45.0, 1.0);
        double scaledRollIntensity = 0.25 * rollMagnitude;
        double rollLean = Math.sin(Math.toRadians(rollDegrees)) * scaledRollIntensity;
        double yawLean = yawSpeed * -0.25 * yawLeanScale;

        targetLeanX = Mth.clamp(rollLean + yawLean, -1.0, 1.0);
        targetLeanY = Mth.clamp(Math.sin(Math.toRadians(pitchDegrees)) * -0.2, -0.8, 0.8);
        targetLeanZ = Mth.clamp(-Math.sin(Math.toRadians(pitchDegrees)) * -0.3, -1.0, 1.0);
        targetCameraTilt = Mth.clamp(yawSpeed * 2.0 * yawLeanScale, -12.0, 12.0);
    }

    public static void update() {
        double smoothing = frameAdjustedSmoothing(LEAN_SMOOTHING, consumeFrameScale());
        currentLeanX = Mth.lerp(smoothing, currentLeanX, targetLeanX);
        currentLeanY = Mth.lerp(smoothing, currentLeanY, targetLeanY);
        currentLeanZ = Mth.lerp(smoothing, currentLeanZ, targetLeanZ);
        currentCameraTilt = Mth.lerp(smoothing, currentCameraTilt, targetCameraTilt);
    }

    public static void reset() {
        targetLeanX = 0.0;
        targetLeanY = 0.0;
        targetLeanZ = 0.0;
        currentLeanX = 0.0;
        currentLeanY = 0.0;
        currentLeanZ = 0.0;
        targetCameraTilt = 0.0;
        currentCameraTilt = 0.0;
        lastUpdateNanos = 0L;
    }

    public static double getLeanX() {
        return currentLeanX;
    }

    public static double getLeanY() {
        return currentLeanY;
    }

    public static double getLeanZ() {
        return currentLeanZ;
    }

    public static double getCameraTilt() {
        return currentCameraTilt;
    }

    private static double consumeFrameScale() {
        long now = System.nanoTime();
        if (lastUpdateNanos == 0L) {
            lastUpdateNanos = now;
            return 1.0D;
        }

        double elapsedSeconds = (now - lastUpdateNanos) / 1_000_000_000.0D;
        lastUpdateNanos = now;
        return Math.min(Math.max(elapsedSeconds / BASELINE_FRAME_SECONDS, 0.0D), MAX_FRAME_SCALE);
    }

    private static double frameAdjustedSmoothing(double smoothing, double frameScale) {
        if (smoothing <= 0.0D) {
            return 0.0D;
        }
        if (smoothing >= 1.0D) {
            return 1.0D;
        }
        return 1.0D - Math.pow(1.0D - smoothing, frameScale);
    }
}
