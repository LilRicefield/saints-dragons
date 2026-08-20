package com.leon.saintsdragons.fabric.client.camera;

public final class DragonCameraState {
    private static float currentDragonRoll = 0.0f;
    private static boolean hasStableAngles = false;
    private static float stableYaw = 0.0f;
    private static float stablePitch = 0.0f;
    private static float stableRoll = 0.0f;
    private static float currentDiveRoll = 0.0f;

    private DragonCameraState() {
    }

    public static void setCurrentRoll(float roll) {
        currentDragonRoll = roll;
    }

    public static float getCurrentRoll() {
        return hasStableAngles ? stableRoll : currentDragonRoll;
    }

    public static void setDiveRoll(float roll) {
        currentDiveRoll = roll;
    }

    public static float getDiveRoll() {
        return currentDiveRoll;
    }

    public static void clearDiveRoll() {
        currentDiveRoll = 0.0f;
    }

    public static void setStableAngles(float yaw, float pitch, float roll) {
        hasStableAngles = true;
        stableYaw = yaw;
        stablePitch = pitch;
        stableRoll = roll;
        currentDragonRoll = roll;
    }

    public static void clearRoll() {
        currentDragonRoll = 0.0f;
        hasStableAngles = false;
    }
}
