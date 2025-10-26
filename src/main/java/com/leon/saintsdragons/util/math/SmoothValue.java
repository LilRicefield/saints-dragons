package com.leon.saintsdragons.util.math;

/**
 * Smooths value transitions over time using interpolation.
 *
 * Two-stage interpolation:
 * 1. update() - LINEAR interpolation tick-to-tick (the actual smoothing)
 * 2. get() - CATMULLROM/other for sub-tick rendering (smoother curves)
 */
public class SmoothValue {
    private final InterpolationType interpolationType;
    private double value;
    private double valueTo;
    private double valueO;
    private final boolean rotation;

    public SmoothValue(InterpolationType interpolationType, double initialValue, boolean rotation) {
        this.interpolationType = interpolationType;
        this.value = initialValue;
        this.valueO = initialValue;
        this.valueTo = initialValue;
        this.rotation = rotation;
    }

    /**
     * Update the current value toward the target using LINEAR interpolation.
     * Call this once per tick.
     *
     * @param speed Speed of interpolation (0.0-1.0), higher = faster
     */
    public void update(float speed) {
        this.valueO = this.value;
        // Always use LINEAR for tick-to-tick smoothing
        this.value = InterpolationType.LINEAR.interpolate(this.value, this.valueTo, speed);
    }

    /**
     * Set the target value to interpolate toward.
     */
    public void setTo(double target) {
        this.valueTo = target;
    }

    /**
     * Immediately snap to a value (no smoothing).
     */
    public void setValue(double value) {
        this.value = value;
        this.valueO = value;
    }

    /**
     * Get the interpolated value for rendering.
     * Uses the configured interpolation type (e.g., CATMULLROM for smooth curves).
     *
     * @param partialTick Partial tick for sub-frame interpolation (0.0-1.0)
     */
    public double get(float partialTick) {
        // If close enough, just return current value
        if (Math.abs(this.valueO - this.value) < 0.1) {
            return this.value;
        }

        // Use the configured interpolation type for rendering
        if (rotation) {
            return this.interpolationType.interpolateRot(this.valueO, this.value, partialTick);
        } else {
            return this.interpolationType.interpolate(this.valueO, this.value, partialTick);
        }
    }

    /**
     * Get the immediate (non-interpolated) current value.
     */
    public double getImmediate() {
        return this.value;
    }

    /**
     * Factory method for rotation values (uses CATMULLROM for smooth curves).
     */
    public static SmoothValue rotation(double initialValue) {
        return new SmoothValue(InterpolationType.CATMULLROM, initialValue, true);
    }

    /**
     * Factory method for regular values (uses CATMULLROM for smooth curves).
     */
    public static SmoothValue value(double initialValue) {
        return new SmoothValue(InterpolationType.CATMULLROM, initialValue, false);
    }
}
