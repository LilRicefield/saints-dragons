package com.leon.saintsdragons.util.math;

/**
 * Interpolation types for smooth value transitions.
 */
public enum InterpolationType {
    LINEAR((a, b, t) -> (1.0 - t) * a + t * b),
    CATMULLROM((a, b, t) -> 0.5 * (2.0 * a + (b - a) * t + (2.0 * a - 5.0 * a + 4.0 * b - b) * t * t + (3.0 * a - a - 3.0 * b + b) * t * t * t));

    private final InterpolationFunc function;

    InterpolationType(InterpolationFunc function) {
        this.function = function;
    }

    public double interpolate(double a, double b, double t) {
        return this.function.interpolate(a, b, t);
    }

    /**
     * Interpolate rotation angles, handling wrapping around 360 degrees.
     */
    public double interpolateRot(double a, double b, double t) {
        double f2 = b % 360.0;
        double f1 = a % 360.0;
        if (Math.abs(f2 - f1) > 180.0) {
            if (f2 > f1) {
                f1 += 360.0;
            } else {
                f2 += 360.0;
            }
        }
        return this.function.interpolate(f1, f2, t);
    }

    @FunctionalInterface
    interface InterpolationFunc {
        double interpolate(double a, double b, double t);
    }
}
