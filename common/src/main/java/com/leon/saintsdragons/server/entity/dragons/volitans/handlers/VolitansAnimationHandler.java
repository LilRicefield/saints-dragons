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
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.volitans.flap");
    private static final RawAnimation SPRINT_FLAP = RawAnimation.begin().thenLoop("animation.volitans.sprint_flap");
    private static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("animation.volitans.takeoff");
    private static final RawAnimation FLY_IDLE = RawAnimation.begin().thenLoop("animation.volitans.fly_idle");
    private static final RawAnimation FLY_GLIDE = RawAnimation.begin().thenLoop("animation.volitans.fly_glide");
    private static final RawAnimation GLIDE_DOWN = RawAnimation.begin().thenLoop("animation.volitans.glide_down");
    private static final RawAnimation LANDING = RawAnimation.begin().thenPlay("animation.volitans.landing");
    private static final RawAnimation LANDED = RawAnimation.begin().thenPlay("animation.volitans.landed");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.volitans.swim");

    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.volitans.bite");
    private static final RawAnimation HORN_GORE = RawAnimation.begin().thenPlay("animation.volitans.horn_gore");
    private static final RawAnimation SWIPE_LEFT = RawAnimation.begin().thenPlay("animation.volitans.swipe_left");
    private static final RawAnimation SWIPE_RIGHT = RawAnimation.begin().thenPlay("animation.volitans.swipe_right");
    private static final RawAnimation ROAR = RawAnimation.begin().thenPlay("animation.volitans.roar");
    private static final RawAnimation BREATH_START = RawAnimation.begin().thenPlay("animation.volitans.breath_start");
    private static final RawAnimation BREATHING = RawAnimation.begin().thenLoop("animation.volitans.breathing");
    private static final RawAnimation BREATH_END = RawAnimation.begin().thenPlay("animation.volitans.breath_end");
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

        if (dragon.getSitProgress() > 0.5f || dragon.isSleepTransitioning() || dragon.isSleeping()) {
            state.setAndContinue(IDLE);
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
            state.setAndContinue(SWIM);
            return PlayState.CONTINUE;
        }

        if (dragon.isFlying()) {
            int mode = dragon.getSyncedFlightMode();

            if (mode == 3) {
                state.setAndContinue(TAKEOFF);
                return PlayState.CONTINUE;
            }

            float pitchDegrees = (float) Math.toDegrees(dragon.getFlightPitchRadians(state.getPartialTick()));
            if (pitchDegrees > 10.0f) {
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

    public void setupActionController(AnimationController<Volitans> controller) {
        controller.triggerableAnim("bite", BITE);
        controller.triggerableAnim("horn_gore", HORN_GORE);
        controller.triggerableAnim("swipe_left", SWIPE_LEFT);
        controller.triggerableAnim("swipe_right", SWIPE_RIGHT);
        controller.triggerableAnim("roar", ROAR);
        controller.triggerableAnim("breath_start", BREATH_START);
        controller.triggerableAnim("breathing", BREATHING);
        controller.triggerableAnim("breath_end", BREATH_END);
        controller.triggerableAnim("landed", LANDED);
    }

    public void setupInstantActionController(AnimationController<Volitans> controller) {
        controller.triggerableAnim("slamming", SLAMMING);
        controller.triggerableAnim("slammed", SLAMMED);
        controller.triggerableAnim("volitans_hurt",
                RawAnimation.begin().thenPlay("animation.volitans.hurt"));
        controller.triggerableAnim("volitans_die",
                RawAnimation.begin().thenPlay("animation.volitans.die"));
    }
}
