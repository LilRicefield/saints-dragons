package com.leon.saintsdragons.client.renderer.vfx;

public final class BloodTempestAfterimageRenderContext {
    private static boolean active;
    private static float alpha;

    private BloodTempestAfterimageRenderContext() {
    }

    public static void begin(float renderAlpha) {
        active = true;
        alpha = renderAlpha;
    }

    public static void end() {
        active = false;
        alpha = 0.0F;
    }

    public static boolean isActive() {
        return active;
    }

    public static float alpha() {
        return alpha;
    }

    public static float red() {
        return 1.0F;
    }

    public static float green() {
        return 0.42F;
    }

    public static float blue() {
        return 0.52F;
    }
}
