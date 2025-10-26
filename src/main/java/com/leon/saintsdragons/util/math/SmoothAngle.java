package com.leon.saintsdragons.util.math;

import net.minecraft.util.Mth;

/**
 * Simple helper that smooths angular values across ticks and provides
 * interpolation for partial tick rendering.
 */
public final class SmoothAngle {

    private float previous;
    private float current;
    private float target;

    public SmoothAngle(float initial) {
        snap(initial);
    }

    public void snap(float value) {
        this.previous = value;
        this.current = value;
        this.target = value;
    }

    public void setTarget(float value) {
        this.target = value;
    }

    public void update(float maxDeltaDegrees) {
        this.previous = this.current;
        this.current = Mth.approachDegrees(this.current, this.target, maxDeltaDegrees);
    }

    public float get(float partialTicks) {
        return Mth.rotLerp(partialTicks, this.previous, this.current);
    }

    public float getImmediate() {
        return this.current;
    }
}
