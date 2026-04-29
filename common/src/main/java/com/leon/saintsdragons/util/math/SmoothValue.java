package com.leon.saintsdragons.util.math;


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

    public void update(float speed) {
        this.valueO = this.value;
        this.value = InterpolationType.LINEAR.interpolate(this.value, this.valueTo, speed);
    }
    public void setTo(double target) {
        this.valueTo = target;
    }
    public void setValue(double value) {
        this.value = value;
        this.valueO = value;
    }
    public double get(float partialTick) {
        if (Math.abs(this.valueO - this.value) < 0.1) {
            return this.value;
        }
        if (rotation) {
            return this.interpolationType.interpolateRot(this.valueO, this.value, partialTick);
        } else {
            return this.interpolationType.interpolate(this.valueO, this.value, partialTick);
        }
    }
    public static SmoothValue rotation(double initialValue) {
        return new SmoothValue(InterpolationType.CATMULLROM, initialValue, true);
    }
    public static SmoothValue value(double initialValue) {
        return new SmoothValue(InterpolationType.CATMULLROM, initialValue, false);
    }
}