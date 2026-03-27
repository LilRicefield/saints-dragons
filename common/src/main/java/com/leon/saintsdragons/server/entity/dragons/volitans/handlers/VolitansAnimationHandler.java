package com.leon.saintsdragons.server.entity.dragons.volitans.handlers;

import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Dedicated animation handler for Volitans.
 * First pass focuses on stable locomotion loops: idle/walk/run.
 */
public final class VolitansAnimationHandler {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.volitans.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.volitans.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.volitans.run");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.volitans.sit");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.volitans.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.volitans.up");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.volitans.flap");
    private static final RawAnimation SPRINT_FLAP = RawAnimation.begin().thenLoop("animation.volitans.sprint_flap");
    private static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("animation.volitans.takeoff");
    private static final RawAnimation FLY_IDLE = RawAnimation.begin().thenLoop("animation.volitans.fly_idle");
    private static final RawAnimation FLY_GLIDE = RawAnimation.begin().thenLoop("animation.volitans.fly_glide");
    private static final RawAnimation GLIDE_DOWN = RawAnimation.begin().thenLoop("animation.volitans.glide_down");
    private static final RawAnimation LANDING = RawAnimation.begin().thenPlay("animation.volitans.landing");
    private static final RawAnimation LANDED = RawAnimation.begin().thenPlay("animation.volitans.landed");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.volitans.swim");
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.volitans.swim_idle");

    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.volitans.bite");
    private static final RawAnimation HORN_GORE = RawAnimation.begin().thenPlay("animation.volitans.horn_gore");
    private static final RawAnimation SWIPE_LEFT = RawAnimation.begin().thenPlay("animation.volitans.swipe_left");
    private static final RawAnimation SWIPE_RIGHT = RawAnimation.begin().thenPlay("animation.volitans.swipe_right");
    private static final RawAnimation ROAR = RawAnimation.begin().thenPlay("animation.volitans.roar");
    private static final RawAnimation ROAR_AIR_WATER = RawAnimation.begin().thenPlay("animation.volitans.roar_air_water");
    private static final RawAnimation BREATH_START = RawAnimation.begin().thenPlay("animation.volitans.breath_start");
    private static final RawAnimation BREATHING = RawAnimation.begin().thenLoop("animation.volitans.breathing");
    private static final RawAnimation BREATH_END = RawAnimation.begin().thenPlay("animation.volitans.breath_end");
    private static final RawAnimation POISON_BALL_READY = RawAnimation.begin().thenPlay("animation.volitans.poison_ball_ready");
    private static final RawAnimation POISON_BALL_HOLD = RawAnimation.begin().thenLoop("animation.volitans.poison_ball_hold");
    private static final RawAnimation POISON_BALL_SHOOT = RawAnimation.begin().thenPlay("animation.volitans.poison_ball_shoot");
    private static final RawAnimation DASH_BACKWARDS = RawAnimation.begin().thenPlay("animation.volitans.dash_backwards");
    private static final RawAnimation DASH_FORWARD = RawAnimation.begin().thenPlay("animation.volitans.dash_forward");
    private static final RawAnimation DODGE_LEFT = RawAnimation.begin().thenPlay("animation.volitans.dodge_left");
    private static final RawAnimation DODGE_RIGHT = RawAnimation.begin().thenPlay("animation.volitans.dodge_right");
    private static final RawAnimation ENTER_BURROW = RawAnimation.begin().thenPlay("animation.volitans.enter_burrow");
    private static final RawAnimation BURROW_IDLE = RawAnimation.begin().thenLoop("animation.volitans.burrow_idle");
    private static final RawAnimation BURROW_MOVE = RawAnimation.begin().thenLoop("animation.volitans.burrow_move");
    private static final RawAnimation BURROW_EXIT = RawAnimation.begin().thenPlay("animation.volitans.burrow_exit");
    private static final RawAnimation SLAMMING = RawAnimation.begin().thenPlay("animation.volitans.slamming");
    private static final RawAnimation SLAMMED = RawAnimation.begin().thenPlay("animation.volitans.slammed");

    private final Volitans dragon;

    public VolitansAnimationHandler(Volitans dragon) {
        this.dragon = dragon;
    }

    public PlayState movementPredicate(AnimationState<Volitans> state) {
        if (dragon.isDying() || dragon.areRiderControlsLocked()) {
            return PlayState.STOP;
        }

        var controller = state.getController();
        controller.transitionLength(6);

        if (dragon.isSleepTransitioning() || dragon.isSleeping()) {
            state.setAndContinue(IDLE);
            return PlayState.CONTINUE;
        }

        float sitProgress = dragon.getSitProgress();
        float maxSit = dragon.maxSitTicks();
        if (sitProgress >= maxSit) {
            state.setAndContinue(SIT);
            return PlayState.CONTINUE;
        } else if (sitProgress > 0f) {
            // Let action-triggered down/up transitions play without movement override.
            return PlayState.STOP;
        }

        if (dragon.isBurrowing() && !dragon.isFlying()) {
            int groundState = dragon.getEffectiveGroundState();
            if (groundState > 0 || state.isMoving()) {
                state.setAndContinue(BURROW_MOVE);
            } else {
                state.setAndContinue(BURROW_IDLE);
            }
            return PlayState.CONTINUE;
        }

        if (dragon.isTakeoff()) {
            state.setAndContinue(TAKEOFF);
            return PlayState.CONTINUE;
        }

        if (dragon.isLanding()) {
            state.setAndContinue(LANDING);
            return PlayState.CONTINUE;
        }

        if (dragon.isInWaterOrBubble() && !dragon.isFlying()) {
            if (state.isMoving()) {
                state.setAndContinue(SWIM);
            } else {
                state.setAndContinue(SWIM_IDLE);
            }
            return PlayState.CONTINUE;
        }

        if (dragon.isFlying()) {
            int mode = dragon.getSyncedFlightMode();

            if (mode == 3) {
                state.setAndContinue(TAKEOFF);
                return PlayState.CONTINUE;
            }

            float pitchDegrees = (float) Math.toDegrees(dragon.getFlightPitchRadians(state.getPartialTick()));
            if (dragon.isRiddenByOwner() && pitchDegrees > 10.0f) {
                state.setAndContinue(GLIDE_DOWN);
                return PlayState.CONTINUE;
            }

            if (mode == 5) {
                state.setAndContinue(FLY_IDLE);
                return PlayState.CONTINUE;
            }

            if (mode == 4) {
                state.setAndContinue(SPRINT_FLAP);
                return PlayState.CONTINUE;
            }

            if (mode == 2 || mode == 1) {
                state.setAndContinue(FLAP);
                return PlayState.CONTINUE;
            }

            if (mode == 0) {
                state.setAndContinue(FLY_GLIDE);
                return PlayState.CONTINUE;
            }

            state.setAndContinue(FLY_GLIDE);
            return PlayState.CONTINUE;
        }

        int groundState = dragon.getEffectiveGroundState();
        if (groundState == 2 || dragon.isRunning()) {
            state.setAndContinue(RUN);
        } else if (groundState == 1 || state.isMoving()) {
            state.setAndContinue(WALK);
        } else {
            state.setAndContinue(IDLE);
        }

        return PlayState.CONTINUE;
    }

    public PlayState actionPredicate(AnimationState<Volitans> state) {
        state.getController().transitionLength(4);
        return PlayState.STOP;
    }

    public PlayState instantActionPredicate(AnimationState<Volitans> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public void triggerSitDownAnimation() {
        dragon.triggerAnim("actions", "sit_down");
    }

    public void triggerSitUpAnimation() {
        dragon.triggerAnim("actions", "sit_up");
    }

    public void setupActionController(AnimationController<Volitans> controller) {
        controller.triggerableAnim("bite", BITE);
        controller.triggerableAnim("horn_gore", HORN_GORE);
        controller.triggerableAnim("swipe_left", SWIPE_LEFT);
        controller.triggerableAnim("swipe_right", SWIPE_RIGHT);
        controller.triggerableAnim("sit_down", SIT_DOWN);
        controller.triggerableAnim("sit_up", SIT_UP);
        controller.triggerableAnim("roar", ROAR);
        controller.triggerableAnim("breathing", BREATHING);
        controller.triggerableAnim("breath_start", BREATH_START);
        controller.triggerableAnim("breath_end", BREATH_END);
        controller.triggerableAnim("poison_ball_ready", POISON_BALL_READY);
        controller.triggerableAnim("poison_ball_hold", POISON_BALL_HOLD);
        controller.triggerableAnim("poison_ball_shoot", POISON_BALL_SHOOT);
        controller.triggerableAnim("burrow_idle", BURROW_IDLE);
        controller.triggerableAnim("burrow_move", BURROW_MOVE);
        controller.triggerableAnim("burrow_exit", BURROW_EXIT);
        controller.triggerableAnim("landed", LANDED);
    }

    public void setupInstantActionController(AnimationController<Volitans> controller) {
        controller.triggerableAnim("dash_backwards", DASH_BACKWARDS);
        controller.triggerableAnim("dash_forward", DASH_FORWARD);
        controller.triggerableAnim("dodge_left", DODGE_LEFT);
        controller.triggerableAnim("dodge_right", DODGE_RIGHT);
        controller.triggerableAnim("enter_burrow", ENTER_BURROW);
        controller.triggerableAnim("roar_air_water", ROAR_AIR_WATER);
        controller.triggerableAnim("slamming", SLAMMING);
        controller.triggerableAnim("slammed", SLAMMED);
        controller.triggerableAnim("volitans_hurt",
                RawAnimation.begin().thenPlay("animation.volitans.hurt"));
        controller.triggerableAnim("volitans_die",
                RawAnimation.begin().thenPlay("animation.volitans.die"));
    }
}
