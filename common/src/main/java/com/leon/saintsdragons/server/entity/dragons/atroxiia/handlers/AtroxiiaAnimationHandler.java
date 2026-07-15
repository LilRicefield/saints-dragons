package com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers;

import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public record AtroxiiaAnimationHandler(Atroxiia dragon) {
    public static final String MOVEMENT_CONTROLLER = AnimationHelper.MOVEMENT_CONTROLLER;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.atroxiia.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.atroxiia.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.atroxiia.run");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.atroxiia.sit");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.atroxiia.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.atroxiia.up");
    private static final RawAnimation FALL_ASLEEP = RawAnimation.begin().thenPlay("animation.atroxiia.fall_asleep");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.atroxiia.sleep");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("animation.atroxiia.wake_up");
    private static final RawAnimation SLAM_RIGHT = RawAnimation.begin().thenPlay("animation.atroxiia.slam_right");
    private static final RawAnimation SLAM_LEFT = RawAnimation.begin().thenPlay("animation.atroxiia.slam_left");
    private static final RawAnimation SWIPE_RIGHT = RawAnimation.begin().thenPlay("animation.atroxiia.swipe_right");
    private static final RawAnimation SWIPE_LEFT = RawAnimation.begin().thenPlay("animation.atroxiia.swipe_left");
    private static final RawAnimation PRECISE_STRIKE = RawAnimation.begin().thenPlay("animation.atroxiia.precise_strike");
    private static final RawAnimation DEVASTATING_SWEEP = RawAnimation.begin().thenPlay("animation.atroxiia.devastating_sweep");

    private static final AnimationHelper.Animations GROUND_ANIMATIONS =
            new AnimationHelper.Animations(IDLE, WALK, RUN, SIT, SIT_DOWN, SIT_UP, FALL_ASLEEP, SLEEP, WAKE_UP, null, null, null);
    private static final AnimationHelper.Transitions GROUND_TRANSITIONS =
            new AnimationHelper.Transitions(4, 4, 4, 4, 4, 4, 4, 4);

    public PlayState movementPredicate(AnimationState<Atroxiia> state) {
        PlayState restPose = AnimationHelper.tryHandleRestPose(
                state, dragon, SLEEP, SIT, GROUND_TRANSITIONS.sleep(), GROUND_TRANSITIONS.sit()
        );
        if (restPose != null) {
            return restPose;
        }

        PlayState dance = AnimationHelper.tryHandleDance(state, dragon, GROUND_TRANSITIONS.idle());
        if (dance != null) {
            return dance;
        }

        return AnimationHelper.handleGroundMovement(
                state, dragon, IDLE, WALK, RUN,
                GROUND_TRANSITIONS.moving(), GROUND_TRANSITIONS.idle()
        );
    }

    public void setupMovementController(AnimationController<Atroxiia> controller) {
        AnimationHelper.registerRestAnimations(controller, GROUND_ANIMATIONS);
        AnimationHelper.register(controller, "slam_right", SLAM_RIGHT);
        AnimationHelper.register(controller, "slam_left", SLAM_LEFT);
        AnimationHelper.register(controller, "swipe_right", SWIPE_RIGHT);
        AnimationHelper.register(controller, "swipe_left", SWIPE_LEFT);
        AnimationHelper.register(controller, "precise_strike", PRECISE_STRIKE);
        AnimationHelper.register(controller, "devastating_sweep", DEVASTATING_SWEEP);
    }

    public void triggerSitDownAnimation() {
        AnimationHelper.triggerRestAnimation(dragon, AnimationHelper.SIT_DOWN);
    }

    public void triggerSitUpAnimation() {
        AnimationHelper.triggerRestAnimation(dragon, AnimationHelper.SIT_UP);
    }

    public void triggerFallAsleepAnimation() {
        AnimationHelper.triggerRestAnimation(dragon, AnimationHelper.FALL_ASLEEP);
    }

    public void triggerWakeUpAnimation() {
        AnimationHelper.triggerRestAnimation(dragon, AnimationHelper.WAKE_UP);
    }
}
