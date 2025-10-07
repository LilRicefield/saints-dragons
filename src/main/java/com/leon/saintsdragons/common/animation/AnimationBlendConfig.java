package com.leon.saintsdragons.common.animation;

/**
 * Configuration for animation blending – duration (ticks) plus interpolation and easing modes.
 */
public record AnimationBlendConfig(int transitionTicks,
                                   AnimationInterpolation interpolation,
                                   AnimationEasing easing) {

    public static AnimationBlendConfig linear(int ticks) {
        return new AnimationBlendConfig(ticks, AnimationInterpolation.LINEAR, AnimationEasing.LINEAR);
    }

    public static AnimationBlendConfig smooth(int ticks) {
        return new AnimationBlendConfig(ticks, AnimationInterpolation.CATMULLROM, AnimationEasing.EASE_IN_OUT);
    }
}


