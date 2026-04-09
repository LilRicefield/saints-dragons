package com.leon.saintsdragons.forge.client.camera;

import net.minecraft.util.Mth;

public final class CameraLeanData {
    private static double targetLeanX = 0.0;
    private static double targetLeanY = 0.0;
    private static double targetLeanZ = 0.0;
    private static double currentLeanX = 0.0;
    private static double currentLeanY = 0.0;
    private static double currentLeanZ = 0.0;
    private static double targetCameraTilt = 0.0;
    private static double currentCameraTilt = 0.0;

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
        currentLeanX = Mth.lerp(0.03f, currentLeanX, targetLeanX);
        currentLeanY = Mth.lerp(0.03f, currentLeanY, targetLeanY);
        currentLeanZ = Mth.lerp(0.03f, currentLeanZ, targetLeanZ);
        currentCameraTilt = Mth.lerp(0.03f, currentCameraTilt, targetCameraTilt);
    }

    public static void reset() {
        targetLeanZ = 0.0;
        targetLeanY = 0.0;
        targetLeanX = 0.0;
        currentLeanZ = 0.0;
        currentLeanY = 0.0;
        currentLeanX = 0.0;
        currentCameraTilt = 0.0;
        targetCameraTilt = 0.0;
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
}
