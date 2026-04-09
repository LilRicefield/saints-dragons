package com.leon.saintsdragons.forge.client.camera;

public final class DragonCameraState {
    private static float currentDragonRoll = 0.0f;
    private static boolean hasStableAngles = false;
    private static float stableYaw = 0.0f;
    private static float stablePitch = 0.0f;
    private static float stableRoll = 0.0f;

    private DragonCameraState() {
    }

    public static void setCurrentRoll(float roll) {
        currentDragonRoll = roll;
    }

    public static float getCurrentRoll() {
        return hasStableAngles ? stableRoll : currentDragonRoll;
    }

    public static void setStableAngles(float yaw, float pitch, float roll) {
        hasStableAngles = true;
        stableYaw = yaw;
        stablePitch = pitch;
        stableRoll = roll;
        currentDragonRoll = roll;
    }

    public static boolean hasStableAngles() {
        return hasStableAngles;
    }

    public static float getStableYaw() {
        return stableYaw;
    }

    public static float getStablePitch() {
        return stablePitch;
    }

    public static float getStableRoll() {
        return stableRoll;
    }

    public static void clearRoll() {
        currentDragonRoll = 0.0f;
        hasStableAngles = false;
    }
}
