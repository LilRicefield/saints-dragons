package com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers;

import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public record AtroxiiaAnimationHandler(Atroxiia dragon) {
    public static final String MOVEMENT_CONTROLLER = AnimationHelper.MOVEMENT_CONTROLLER;
    public static final String FAST_ACTION_CONTROLLER = "atroxiiaFastAction";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.atroxiia.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.atroxiia.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.atroxiia.run");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.atroxiia.swim");
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.atroxiia.swim_idle");
    private static final RawAnimation JUMP = RawAnimation.begin().thenPlay("animation.atroxiia.jump");
    private static final RawAnimation JUMP_LANDED = RawAnimation.begin().thenPlay("animation.atroxiia.jump_landed");
    private static final RawAnimation UNDERWATER_BITE = RawAnimation.begin().thenPlay("animation.atroxiia.underwater_bite");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.atroxiia.sit");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.atroxiia.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.atroxiia.up");
    private static final RawAnimation FALL_ASLEEP = RawAnimation.begin().thenPlay("animation.atroxiia.fall_asleep");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.atroxiia.sleep");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("animation.atroxiia.wake_up");
    private static final RawAnimation STUNNED = RawAnimation.begin().thenLoop("animation.atroxiia.stunned");
    private static final RawAnimation SLAM_RIGHT = RawAnimation.begin().thenPlay("animation.atroxiia.slam_right");
    private static final RawAnimation SLAM_LEFT = RawAnimation.begin().thenPlay("animation.atroxiia.slam_left");
    private static final RawAnimation SWIPE_RIGHT = RawAnimation.begin().thenPlay("animation.atroxiia.swipe_right");
    private static final RawAnimation SWIPE_LEFT = RawAnimation.begin().thenPlay("animation.atroxiia.swipe_left");
    private static final RawAnimation PRECISE_STRIKE = RawAnimation.begin().thenPlay("animation.atroxiia.precise_strike");
    private static final RawAnimation DEVASTATING_SWEEP = RawAnimation.begin().thenPlay("animation.atroxiia.devastating_sweep");
    private static final RawAnimation HELHEIM_QUAKE_ONE = RawAnimation.begin().thenPlay("animation.atroxiia.helheim_quake1");
    private static final RawAnimation HELHEIM_QUAKE_TWO = RawAnimation.begin().thenPlay("animation.atroxiia.helheim_quake2");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.atroxiia.hurt");
    private static final RawAnimation DIE = RawAnimation.begin().thenPlay("animation.atroxiia.die");
    private static final RawAnimation EAT = RawAnimation.begin().thenPlay("animation.atroxiia.eat");
    private static final RawAnimation FLEX = RawAnimation.begin().thenPlay("animation.atroxiia.flex");
    private static final RawAnimation INVESTIGATING = RawAnimation.begin().thenPlay("animation.atroxiia.investigating");

    private static final AnimationHelper.Animations GROUND_ANIMATIONS =
            new AnimationHelper.Animations(IDLE, WALK, RUN, SIT, SIT_DOWN, SIT_UP, FALL_ASLEEP, SLEEP, WAKE_UP, null, STUNNED, null);
    private static final AnimationHelper.Transitions GROUND_TRANSITIONS =
            new AnimationHelper.Transitions(4, 4, 4, 4, 4, 4, 4, 4);

    public PlayState movementPredicate(AnimationState<Atroxiia> state) {
        if (dragon.isTamingStunned()) {
            state.getController().transitionLength(GROUND_TRANSITIONS.stunned());
            state.setAndContinue(STUNNED);
            return PlayState.CONTINUE;
        }
        if (dragon.isScentAssessing()) {
            state.getController().transitionLength(GROUND_TRANSITIONS.idle());
            state.setAndContinue(INVESTIGATING);
            return PlayState.CONTINUE;
        }
        if (dragon.isInWaterOrBubble()) {
            state.getController().transitionLength(GROUND_TRANSITIONS.water());
            boolean moving = dragon.getDeltaMovement().lengthSqr() > 0.0025D
                    || Math.abs(dragon.getLastRiderForward()) > 0.02F
                    || Math.abs(dragon.getLastRiderStrafe()) > 0.02F;
            state.setAndContinue(moving ? SWIM : SWIM_IDLE);
            return PlayState.CONTINUE;
        }

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
        AnimationHelper.register(controller, "helheim_quake1", HELHEIM_QUAKE_ONE);
        AnimationHelper.register(controller, "helheim_quake2", HELHEIM_QUAKE_TWO);
        AnimationHelper.register(controller, "atroxiia_flex", FLEX);
        AnimationHelper.register(controller, "jump", JUMP);
        AnimationHelper.register(controller, "jump_landed", JUMP_LANDED);
    }

    public PlayState fastActionPredicate(AnimationState<Atroxiia> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public void setupFastActionController(AnimationController<Atroxiia> controller) {
        AnimationHelper.register(controller, "underwater_bite", UNDERWATER_BITE);
    }

    public void triggerJumpAnimation() {
        dragon.triggerAnim(MOVEMENT_CONTROLLER, "jump");
    }

    public void triggerJumpLandedAnimation() {
        dragon.triggerAnim(MOVEMENT_CONTROLLER, "jump_landed");
    }

    public void setupInteractionController(AnimationController<Atroxiia> controller) {
        AnimationHelper.register(controller, "atroxiia_hurt", HURT);
        AnimationHelper.register(controller, "hurt", HURT);
        AnimationHelper.register(controller, AnimationHelper.DIE, DIE);
        AnimationHelper.register(controller, AnimationHelper.EAT, EAT);
    }

    public void triggerSitDownAnimation() {
        if (!dragon.isInWaterOrBubble()) {
            AnimationHelper.triggerRestAnimation(dragon, AnimationHelper.SIT_DOWN);
        }
    }

    public void triggerSitUpAnimation() {
        if (!dragon.isInWaterOrBubble()) {
            AnimationHelper.triggerRestAnimation(dragon, AnimationHelper.SIT_UP);
        }
    }

    public void triggerFallAsleepAnimation() {
        AnimationHelper.triggerRestAnimation(dragon, AnimationHelper.FALL_ASLEEP);
    }

    public void triggerWakeUpAnimation() {
        AnimationHelper.triggerRestAnimation(dragon, AnimationHelper.WAKE_UP);
    }
}
