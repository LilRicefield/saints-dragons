package com.leon.saintsdragons.server.entity.dragons.volitans.handlers;

import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public final class VolitansAnimationHandler {
    public static final String MOVEMENT_CONTROLLER = AnimationHelper.MOVEMENT_CONTROLLER;
    public static final String FAST_ACTION_CONTROLLER = "volitansFastAction";
    public static final String ACTION_CONTROLLER = "volitansAction";
    public static final String AIR_ACTION_CONTROLLER = "volitansAirAction";
    public static final int MOVEMENT_TRIGGER_TRANSITION_TICKS = 2;

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
    private static final RawAnimation FALLING = RawAnimation.begin().thenLoop("animation.volitans.falling");
    private static final RawAnimation LANDING = RawAnimation.begin().thenPlay("animation.volitans.landing");
    private static final RawAnimation LANDED = RawAnimation.begin().thenPlay("animation.volitans.landed");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.volitans.swim");
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.volitans.swim_idle");
    private static final RawAnimation STUNNED = RawAnimation.begin().thenLoop("animation.volitans.stunned");
    private static final RawAnimation UNDERWATER_STUNNED = RawAnimation.begin().thenLoop("animation.volitans.underwater_stunned");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.volitans.sleep");
    private static final RawAnimation SLEEP_UNDERWATER = RawAnimation.begin().thenLoop("animation.volitans.sleep_underwater");
    private static final RawAnimation FALL_ASLEEP = RawAnimation.begin().thenPlay("animation.volitans.fall_asleep");
    private static final RawAnimation FALL_ASLEEP_UNDERWATER = RawAnimation.begin().thenPlay("animation.volitans.fall_asleep_underwater");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("animation.volitans.wake_up");
    private static final RawAnimation WAKE_UP_UNDERWATER = RawAnimation.begin().thenPlay("animation.volitans.wake_up_underwater");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.volitans.hurt");
    private static final RawAnimation DIE = RawAnimation.begin().thenPlay("animation.volitans.die");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.volitans.bite");
    private static final RawAnimation HORN_GORE = RawAnimation.begin().thenPlay("animation.volitans.horn_gore");
    private static final RawAnimation SWIPE_LEFT = RawAnimation.begin().thenPlay("animation.volitans.swipe_left");
    private static final RawAnimation SWIPE_RIGHT = RawAnimation.begin().thenPlay("animation.volitans.swipe_right");
    private static final RawAnimation EAT = RawAnimation.begin().thenPlay("animation.volitans.eat");
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
    private static final AnimationHelper.Animations GROUND_ANIMATIONS =
            new AnimationHelper.Animations(IDLE, WALK, RUN, SIT, SIT_DOWN, SIT_UP, FALL_ASLEEP, SLEEP, WAKE_UP, SWIM, STUNNED, FALLING);
    private static final AnimationHelper.Transitions GROUND_TRANSITIONS =
            new AnimationHelper.Transitions(4, 3, 4, 4, 4, 4, 4, 4);
    private static final AnimationHelper.FlightAnimations FLIGHT_ANIMATIONS =
            new AnimationHelper.FlightAnimations(TAKEOFF, null, null, FLY_GLIDE, GLIDE_DOWN, FLY_IDLE, FLAP, SPRINT_FLAP);
    private static final AnimationHelper.FlightTransitions FLIGHT_TRANSITIONS =
            new AnimationHelper.FlightTransitions(2, 6, 6, 3, 6, 6, 6, 2);
    private static final int ACTION_TRANSITION_TICKS = 4;
    private static final int FAST_ACTION_TRANSITION_TICKS = 1;
    private static final int AIR_ACTION_TRANSITION_TICKS = 1;

    private final Volitans dragon;

    public VolitansAnimationHandler(Volitans dragon) {
        this.dragon = dragon;
    }

    public void triggerSitDownAnimation() {
        dragon.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, AnimationHelper.SIT_DOWN);
    }

    public void triggerSitUpAnimation() {
        dragon.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, AnimationHelper.SIT_UP);
    }

    public void setupFlightController(AnimationController<Volitans> controller) {
        AnimationHelper.registerFlightStandard(controller, TAKEOFF, null, null);
        controller.triggerableAnim("slamming", SLAMMING);
        controller.triggerableAnim("slammed", SLAMMED);
    }

    public void setupMovementController(AnimationController<Volitans> controller) {
        AnimationHelper.register(controller, GROUND_ANIMATIONS);
        AnimationHelper.register(controller, AnimationHelper.LANDED, LANDED);
        AnimationHelper.register(controller, "sleep_underwater", SLEEP_UNDERWATER);
        AnimationHelper.register(controller, "dash_backwards", DASH_BACKWARDS);
        AnimationHelper.register(controller, "dash_forward", DASH_FORWARD);
        AnimationHelper.register(controller, "dodge_left", DODGE_LEFT);
        AnimationHelper.register(controller, "dodge_right", DODGE_RIGHT);
        AnimationHelper.register(controller, "roar", ROAR);
    }

    public void setupTransitionController(AnimationController<Volitans> controller) {
        AnimationHelper.registerTransitions(controller, GROUND_ANIMATIONS);
        AnimationHelper.register(controller, "fall_asleep_underwater", FALL_ASLEEP_UNDERWATER);
        AnimationHelper.register(controller, "wake_up_underwater", WAKE_UP_UNDERWATER);
    }

    public void setupActionController(AnimationController<Volitans> controller) {
        controller.triggerableAnim("horn_gore", HORN_GORE);
        controller.triggerableAnim("swipe_left", SWIPE_LEFT);
        controller.triggerableAnim("swipe_right", SWIPE_RIGHT);
        controller.triggerableAnim("breathing", BREATHING);
        controller.triggerableAnim("breath_start", BREATH_START);
        controller.triggerableAnim("breath_end", BREATH_END);
        controller.triggerableAnim("poison_ball_ready", POISON_BALL_READY);
        controller.triggerableAnim("poison_ball_hold", POISON_BALL_HOLD);
        controller.triggerableAnim("poison_ball_shoot", POISON_BALL_SHOOT);
        controller.triggerableAnim("burrow_idle", BURROW_IDLE);
        controller.triggerableAnim("burrow_move", BURROW_MOVE);
        controller.triggerableAnim("burrow_exit", BURROW_EXIT);
    }

    public void setupFastActionController(AnimationController<Volitans> controller) {
        controller.triggerableAnim("bite", BITE);
        controller.triggerableAnim("enter_burrow", ENTER_BURROW);
        controller.triggerableAnim("roar_air_water", ROAR_AIR_WATER);
    }

    public void setupAirActionController(AnimationController<Volitans> controller) {
        controller.triggerableAnim("swipe_left", SWIPE_LEFT);
        controller.triggerableAnim("swipe_right", SWIPE_RIGHT);
    }

    public void setupInteractionController(AnimationController<Volitans> controller) {
        controller.triggerableAnim(AnimationHelper.EAT, EAT);
        controller.triggerableAnim("volitans_hurt", HURT);
        controller.triggerableAnim("volitans_die", DIE);
    }

    public PlayState movementPredicate(AnimationState<Volitans> state) {
        if (dragon.isDying() || dragon.areRiderControlsLocked()) {
            return PlayState.STOP;
        }

        var controller = state.getController();
        boolean aerialState = dragon.isFlying() || dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering();

        if (dragon.isTamingStunned()) {
            controller.transitionLength(GROUND_TRANSITIONS.stunned());
            state.setAndContinue(dragon.isInWaterOrBubble() ? UNDERWATER_STUNNED : STUNNED);
            return PlayState.CONTINUE;
        }

        RawAnimation sleepPose = dragon.isInWaterOrBubble() ? SLEEP_UNDERWATER : SLEEP;
        PlayState restPose = AnimationHelper.tryHandleRestPose(
                state, dragon, sleepPose, SIT, GROUND_TRANSITIONS.sleep(), GROUND_TRANSITIONS.sit()
        );
        if (restPose != null) {
            return restPose;
        }

        if (dragon.isBurrowing() && !aerialState) {
            int groundState = dragon.getEffectiveGroundState();
            if (groundState > 0 || state.isMoving()) {
                controller.transitionLength(GROUND_TRANSITIONS.moving());
                state.setAndContinue(BURROW_MOVE);
            } else {
                controller.transitionLength(GROUND_TRANSITIONS.idle());
                state.setAndContinue(BURROW_IDLE);
            }
            return PlayState.CONTINUE;
        }

        if (aerialState) {
            return PlayState.STOP;
        }

        PlayState dance = AnimationHelper.tryHandleDance(state, dragon, GROUND_TRANSITIONS.idle());
        if (dance != null) {
            return dance;
        }

        if (dragon.isFallingForAnimation()) {
            controller.transitionLength(GROUND_TRANSITIONS.falling());
            state.setAndContinue(FALLING);
            return PlayState.CONTINUE;
        }

        if (dragon.isInWaterOrBubble() && !aerialState) {
            controller.transitionLength(GROUND_TRANSITIONS.water());
            if (dragon.isSwimmingMoving()) {
                state.setAndContinue(SWIM);
            } else {
                state.setAndContinue(SWIM_IDLE);
            }
            return PlayState.CONTINUE;
        }

        return AnimationHelper.handleGroundMovement(
                state, dragon, IDLE, WALK, RUN,
                GROUND_TRANSITIONS.moving(), GROUND_TRANSITIONS.idle(), true
        );
    }
    public PlayState flightPredicate(AnimationState<Volitans> state) {
        if (dragon.isDying() || dragon.isTamingStunned()) {
            return PlayState.STOP;
        }
        boolean aerialState = dragon.isFlying() || dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering();
        if (!aerialState) {
            return PlayState.STOP;
        }
        if (dragon.isTakeoff()) {
            return AnimationHelper.handleTakeoff(state, false, FLIGHT_ANIMATIONS, FLIGHT_TRANSITIONS);
        }

        DragonFlightStateEvaluator.VisualState visualState;
        if (dragon.isLanding()) {
            visualState = DragonFlightStateEvaluator.VisualState.GLIDE_DOWN;
        } else {
            int mode = dragon.getSyncedFlightMode();
            float animationPitchRad = -dragon.getFlightPitchRadians(state.getPartialTick());
            visualState = DragonFlightStateEvaluator.evaluateVisualState(
                    mode,
                    dragon.isRiddenByOwner(),
                    animationPitchRad,
                    dragon.getDeltaMovement()
            );
        }
        return AnimationHelper.handleFlightState(state, visualState, FLIGHT_ANIMATIONS, FLIGHT_TRANSITIONS);
    }
    public PlayState actionPredicate(AnimationState<Volitans> state) {
        state.getController().transitionLength(ACTION_TRANSITION_TICKS);
        return PlayState.STOP;
    }

    public PlayState fastActionPredicate(AnimationState<Volitans> state) {
        state.getController().transitionLength(FAST_ACTION_TRANSITION_TICKS);
        return PlayState.STOP;
    }

    public PlayState airActionPredicate(AnimationState<Volitans> state) {
        state.getController().transitionLength(AIR_ACTION_TRANSITION_TICKS);
        return PlayState.STOP;
    }
}
