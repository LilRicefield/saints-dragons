package com.leon.saintsdragons.common.animation;

import net.minecraft.util.Mth;

/**
 * Minimal interpolation helpers inspired by Astemirlib's InterpolationType.
 */
public enum AnimationInterpolation {
    LINEAR,
    CATMULLROM;

    public float interpolate(float a, float b, float alpha) {
        float t = Mth.clamp(alpha, 0.0f, 1.0f);
        return switch (this) {
            case LINEAR -> Mth.lerp(t, a, b);
            case CATMULLROM -> catmullRom(a, a, b, b, t);
        };
    }

    public double interpolate(double a, double b, double alpha) {
        double t = Mth.clamp(alpha, 0.0d, 1.0d);
        return switch (this) {
            case LINEAR -> Mth.lerp(t, a, b);
            case CATMULLROM -> catmullRom(a, a, b, b, t);
        };
    }

    private static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5f * ((2.0f * p1)
                + (-p0 + p2) * t
                + (2.0f * p0 - 5.0f * p1 + 4.0f * p2 - p3) * t2
                + (-p0 + 3.0f * p1 - 3.0f * p2 + p3) * t3);
    }

    private static double catmullRom(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5d * ((2.0d * p1)
                + (-p0 + p2) * t
                + (2.0d * p0 - 5.0d * p1 + 4.0d * p2 - p3) * t2
                + (-p0 + 3.0d * p1 - 3.0d * p2 + p3) * t3);
    }
}


