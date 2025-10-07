package com.leon.saintsdragons.common.animation;

import net.minecraft.util.Mth;

import java.util.function.Function;

/**
 * Simple easing helpers for animation blends and smoothed values.
 * The enum intentionally keeps the selection small for now – expand as needed.
 */
public enum AnimationEasing {
    LINEAR(t -> t),
    EASE_IN(t -> t * t),
    EASE_OUT(t -> 1.0f - (1.0f - t) * (1.0f - t)),
    EASE_IN_OUT(t -> t < 0.5f ? 2.0f * t * t : 1.0f - 2.0f * (t - 1.0f) * (t - 1.0f));

    private final Function<Float, Float> function;

    AnimationEasing(Function<Float, Float> function) {
        this.function = function;
    }

    /**
     * Apply the easing to the given alpha (clamped to 0..1).
     */
    public float ease(float alpha) {
        float clamped = Mth.clamp(alpha, 0.0f, 1.0f);
        return Mth.clamp(function.apply(clamped), 0.0f, 1.0f);
    }
}


