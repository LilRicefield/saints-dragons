package com.leon.saintsdragons.common.animation;

import net.minecraft.util.Mth;

/**
 * Lightweight float smoother similar to Astemirlib's SmoothValue.
 * Call {@link #update(float)} once per tick, and sample via {@link #get(float)}.
 */
public final class SmoothFloat {
    private final AnimationInterpolation interpolation;
    private final AnimationEasing easing;
    private float value;
    private float previous;
    private float target;
    private final boolean wrap;
    private float speed = 1.0f;

    public SmoothFloat(AnimationInterpolation interpolation, AnimationEasing easing, float initial) {
        this(interpolation, easing, initial, initial, false);
    }

    public SmoothFloat(AnimationInterpolation interpolation, AnimationEasing easing, float initial, boolean wrapAngle) {
        this(interpolation, easing, initial, initial, wrapAngle);
    }

    public SmoothFloat(AnimationInterpolation interpolation, AnimationEasing easing, float initial, float target, boolean wrapAngle) {
        this.interpolation = interpolation;
        this.easing = easing;
        this.value = initial;
        this.previous = initial;
        this.target = target;
        this.wrap = wrapAngle;
    }

    public void update(float step) {
        previous = value;
        float eased = easing.ease(Mth.clamp(step * speed, 0.0f, 1.0f));
        if (wrap) {
            float wrappedTarget = wrapDegrees(target, value);
            value = interpolation.interpolate(value, wrappedTarget, eased);
            value = Mth.wrapDegrees(value);
        } else {
            value = interpolation.interpolate(value, target, eased);
        }
    }

    public float get(float partialTick) {
        float alpha = Mth.clamp(partialTick, 0.0f, 1.0f);
        if (wrap) {
            float wrappedValue = wrapDegrees(value, previous);
            float wrappedPrev = wrapDegrees(previous, value);
            return interpolation.interpolate(wrappedPrev, wrappedValue, alpha);
        }
        return interpolation.interpolate(previous, value, alpha);
    }

    public void setTo(float newTarget) {
        target = wrap ? Mth.wrapDegrees(newTarget) : newTarget;
    }

    public void setSpeed(float blendSpeed) {
        this.speed = blendSpeed;
    }

    public float getCurrent() {
        return value;
    }

    public float getTarget() {
        return target;
    }

    public void force(float newValue) {
        value = previous = target = wrap ? Mth.wrapDegrees(newValue) : newValue;
    }

    private static float wrapDegrees(float degrees, float reference) {
        float wrapped = Mth.wrapDegrees(degrees);
        float ref = Mth.wrapDegrees(reference);
        float delta = wrapped - ref;
        if (delta > 180.0f) {
            wrapped -= 360.0f;
        } else if (delta < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }
}


