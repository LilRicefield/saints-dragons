package com.leon.saintsdragons.server.entity.dragons.handlers;

import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public final class DragonStateAnimationHelper {
    public static final String CONTROLLER = "state";
    public static final String SIT_DOWN = "sit_down";
    public static final String SIT_UP = "sit_up";
    public static final String FALL_ASLEEP = "fall_asleep";
    public static final String SLEEP = "sleep";
    public static final String WAKE_UP = "wake_up";

    private DragonStateAnimationHelper() {
    }

    public static PlayState idle(AnimationState<?> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public static void registerStandard(AnimationController<?> controller, String dragonName) {
        register(controller, SIT_DOWN, oneShot(dragonName, "down"));
        register(controller, SIT_UP, oneShot(dragonName, "up"));
        register(controller, FALL_ASLEEP, oneShot(dragonName, "fall_asleep"));
        register(controller, SLEEP, loop(dragonName, "sleep"));
        register(controller, WAKE_UP, oneShot(dragonName, "wake_up"));
    }

    public static void register(AnimationController<?> controller, String trigger, RawAnimation animation) {
        controller.triggerableAnim(trigger, animation);
    }

    public static RawAnimation oneShot(String dragonName, String animationName) {
        return RawAnimation.begin().thenPlay(path(dragonName, animationName));
    }

    public static RawAnimation loop(String dragonName, String animationName) {
        return RawAnimation.begin().thenLoop(path(dragonName, animationName));
    }

    private static String path(String dragonName, String animationName) {
        return "animation." + dragonName + "." + animationName;
    }
}
