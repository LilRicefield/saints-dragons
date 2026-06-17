package com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;


public record IgnivorusAnimationHandler(Ignivorus dragon) {
    public static final String MOVEMENT_CONTROLLER = AnimationHelper.MOVEMENT_CONTROLLER;
    public static final String FAST_ACTION_CONTROLLER = "ignivorusFastAction";
    public static final String ACTION_CONTROLLER = "ignivorusAction";


    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ignivorus.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.ignivorus.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.ignivorus.run");
    private static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("animation.ignivorus.take_off");
    private static final RawAnimation LANDING = RawAnimation.begin().thenPlay("animation.ignivorus.landing");
    private static final RawAnimation LANDED = RawAnimation.begin().thenPlay("animation.ignivorus.landed");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("animation.ignivorus.glide");
    private static final RawAnimation GLIDE_DOWN = RawAnimation.begin().thenLoop("animation.ignivorus.glide_down");
    private static final RawAnimation FALLING = RawAnimation.begin().thenLoop("animation.ignivorus.falling");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.ignivorus.flap");
    private static final RawAnimation SPRINT_FLAP = RawAnimation.begin().thenLoop("animation.ignivorus.sprint_flap");
    private static final RawAnimation FLY_IDLE = RawAnimation.begin().thenLoop("animation.ignivorus.fly_idle");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.ignivorus.sit");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.ignivorus.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.ignivorus.up");
    private static final RawAnimation FALL_ASLEEP = RawAnimation.begin().thenPlay("animation.ignivorus.fall_asleep");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("animation.ignivorus.wake_up");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.ignivorus.swim");
    private static final RawAnimation STUNNED = RawAnimation.begin().thenLoop("animation.ignivorus.stunned");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.ignivorus.sleep");
    private static final RawAnimation BULLDOZER_IDLE = RawAnimation.begin().thenLoop("animation.ignivorus.bulldozer_idle");
    private static final RawAnimation BULLDOZING = RawAnimation.begin().thenLoop("animation.ignivorus.bulldozing");
    private static final RawAnimation PHASE2_IDLE = RawAnimation.begin().thenLoop("animation.ignivorus.phase2_idle");
    private static final RawAnimation PHASE2_WALK = RawAnimation.begin().thenLoop("animation.ignivorus.phase2_walk");
    private static final RawAnimation PHASE2_RUN = RawAnimation.begin().thenLoop("animation.ignivorus.phase2_run");
    private static final RawAnimation PHASE2_TAKEOFF = RawAnimation.begin().thenPlay("animation.ignivorus.phase2_takeoff");
    private static final RawAnimation PHASE2_LANDED = RawAnimation.begin().thenPlay("animation.ignivorus.phase2_landed");
    private static final RawAnimation PHASE2_ULTIMATE = RawAnimation.begin().thenPlay("animation.ignivorus.phase2_ultimate");
    private static final RawAnimation LEAP_TAKEOFF = RawAnimation.begin().thenPlay("animation.ignivorus.ignivorus_leap");
    private static final AnimationHelper.Animations GROUND_ANIMATIONS =
            new AnimationHelper.Animations(IDLE, WALK, RUN, SIT, SIT_DOWN, SIT_UP, FALL_ASLEEP, SLEEP, WAKE_UP, SWIM, STUNNED, FALLING);
    private static final AnimationHelper.Transitions GROUND_TRANSITIONS =
            new AnimationHelper.Transitions(4, 4, 4, 4, 4, 4, 4, 4);
    private static final AnimationHelper.FlightAnimations FLIGHT_ANIMATIONS =
            new AnimationHelper.FlightAnimations(TAKEOFF, null, LANDED, GLIDE, GLIDE_DOWN, FLY_IDLE, FLAP, SPRINT_FLAP);
    private static final AnimationHelper.FlightTransitions FLIGHT_TRANSITIONS =
            new AnimationHelper.FlightTransitions(2, 12, 6, 3, 6, 4, 3, 2);
    private static final int ACTION_TRANSITION_TICKS = 4;
    private static final int FAST_ACTION_TRANSITION_TICKS = 1;

    public void triggerSitDownAnimation() {
        dragon.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, AnimationHelper.SIT_DOWN);
    }

    public void triggerSitUpAnimation() {
        dragon.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, AnimationHelper.SIT_UP);
    }

    public void triggerFallAsleepAnimation() {
        dragon.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, AnimationHelper.FALL_ASLEEP);
    }

    public void triggerWakeUpAnimation() {
        dragon.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, AnimationHelper.WAKE_UP);
    }

    public void triggerBulldozeEnterAnimation() {
        dragon.triggerAnim(MOVEMENT_CONTROLLER, "bulldozer_enter");
    }

    public void triggerBulldozeExitAnimation() {
        dragon.triggerAnim(MOVEMENT_CONTROLLER, "bulldozer_exit");
    }

    public void triggerPhase2EnterAnimation() {
        dragon.triggerAnim(FAST_ACTION_CONTROLLER, "phase2_enter");
    }

    public void triggerPhase2ExitAnimation() {
        dragon.triggerAnim(FAST_ACTION_CONTROLLER, "phase2_exit");
    }

    public void triggerLeapImpactAnimation() {
        dragon.triggerAnim(MOVEMENT_CONTROLLER, "leap_impact");
    }

    public void setupActionController(AnimationController<Ignivorus> controller) {
        controller.triggerableAnim("bite",
            RawAnimation.begin().thenPlay("animation.ignivorus.bite"));
        controller.triggerableAnim("body_slam",
            RawAnimation.begin().thenPlay("animation.ignivorus.body_slam"));
        controller.triggerableAnim("roar",
            RawAnimation.begin().thenPlay("animation.ignivorus.roar"));
        controller.triggerableAnim("fire_breath_start",
                RawAnimation.begin().thenPlay("animation.ignivorus.fire_breath_start"));
        controller.triggerableAnim("fire_breathing",
                RawAnimation.begin().thenLoop("animation.ignivorus.fire_breathing"));
        controller.triggerableAnim("fire_breath_stop",
                RawAnimation.begin().thenPlay("animation.ignivorus.fire_breath_end"));
    }

    public void setupMovementController(AnimationController<Ignivorus> controller) {
        AnimationHelper.register(controller, GROUND_ANIMATIONS);
        controller.triggerableAnim("wing_swipe_left",
                RawAnimation.begin().thenPlay("animation.ignivorus.wing_swipe_left"));
        controller.triggerableAnim("wing_swipe_right",
                RawAnimation.begin().thenPlay("animation.ignivorus.wing_swipe_right"));
        controller.triggerableAnim("bulldozer_enter",
                RawAnimation.begin().thenPlay("animation.ignivorus.bulldozer_enter"));
        controller.triggerableAnim("bulldozer_exit",
                RawAnimation.begin().thenPlay("animation.ignivorus.bulldozer_exit"));
        controller.triggerableAnim("stomp_left",
                RawAnimation.begin().thenPlay("animation.ignivorus.ignivorus_stomp_left"));
        controller.triggerableAnim("stomp_right",
                RawAnimation.begin().thenPlay("animation.ignivorus.ignivorus_stomp_right"));
        controller.triggerableAnim("leap_impact",
                RawAnimation.begin().thenPlay("animation.ignivorus.ignivorus_impact"));
        controller.triggerableAnim("ultimate_start",
                RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_start"));
        controller.triggerableAnim("ultimate",
                RawAnimation.begin().thenPlay("animation.ignivorus.ultimate"));
        controller.triggerableAnim("ultimate_end",
                RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_end"));
        controller.triggerableAnim("phase2_ultimate", PHASE2_ULTIMATE);
    }

    public void setupTransitionController(AnimationController<Ignivorus> controller) {
        AnimationHelper.registerTransitions(controller, GROUND_ANIMATIONS);
    }

    public void setupInteractionController(AnimationController<Ignivorus> controller) {
        controller.triggerableAnim("ignivorus_hurt",
                RawAnimation.begin().thenPlay("animation.ignivorus.hurt"));
        controller.triggerableAnim(AnimationHelper.DIE,
                RawAnimation.begin().thenPlay("animation.ignivorus.die"));
        controller.triggerableAnim(AnimationHelper.EAT,
                RawAnimation.begin().thenPlay("animation.ignivorus.eat"));
    }

    public void setupFlightController(AnimationController<Ignivorus> controller) {
        AnimationHelper.registerFlightStandard(controller, TAKEOFF, null, LANDED);
        AnimationHelper.registerFlight(controller, AnimationHelper.PHASE2_TAKEOFF, PHASE2_TAKEOFF);
        AnimationHelper.registerFlight(controller, AnimationHelper.PHASE2_LANDED, PHASE2_LANDED);
        AnimationHelper.registerFlight(controller, "ultimate_start_air",
                RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_start_air"));
        AnimationHelper.registerFlight(controller, "ultimate_air",
                RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_air"));
        AnimationHelper.registerFlight(controller, "ultimate_end_air",
                RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_end_air"));
    }

    public void setupFastActionController(AnimationController<Ignivorus> controller) {
        controller.triggerableAnim("phase2_enter",
                RawAnimation.begin().thenPlay("animation.ignivorus.phase2_enter"));
        controller.triggerableAnim("phase2_exit",
                RawAnimation.begin().thenPlay("animation.ignivorus.phase2_exit"));
        controller.triggerableAnim("ignivorus_flex",
                RawAnimation.begin().thenPlay("animation.ignivorus.flex"));
        controller.triggerableAnim("fireball_level1_charge",
                RawAnimation.begin().thenPlay("animation.ignivorus.fireball_level1_charge"));
        controller.triggerableAnim("fireball_level2_charge",
                RawAnimation.begin().thenPlay("animation.ignivorus.fireball_level2_charge"));
        controller.triggerableAnim("fireball_level3_charge",
                RawAnimation.begin().thenPlay("animation.ignivorus.fireball_level3_charge"));
        controller.triggerableAnim("fireball_level3_hold",
                RawAnimation.begin().thenLoop("animation.ignivorus.fireball_level3_hold"));
        controller.triggerableAnim("fireball_level1_shoot",
                RawAnimation.begin().thenPlay("animation.ignivorus.fireball_level1_shoots"));
        controller.triggerableAnim("fireball_level2_shoot",
                RawAnimation.begin().thenPlay("animation.ignivorus.fireball_level2_shoots"));
        controller.triggerableAnim("fireball_level3_shoot",
                RawAnimation.begin().thenPlay("animation.ignivorus.fireball_level3_shoots"));
    }

    public PlayState movementPredicate(AnimationState<Ignivorus> state) {
        var controller = state.getController();
        boolean aerialState = dragon.isFlying() || dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering();

        if (dragon.areRiderControlsLocked()) {
            return PlayState.STOP;
        }

        if (dragon.isTakeoff()) {
            return PlayState.STOP;
        }
        if (dragon.isDying()) {
            return PlayState.STOP;
        }

        if (dragon.isTamingStunned()) {
            controller.transitionLength(GROUND_TRANSITIONS.stunned());
            AnimationHelper.setAndContinue(state, STUNNED);
            return PlayState.CONTINUE;
        }
        PlayState restPose = AnimationHelper.tryHandleRestPose(
                state, dragon, SLEEP, SIT, GROUND_TRANSITIONS.sleep(), GROUND_TRANSITIONS.sit(), !aerialState
        );
        if (restPose != null) {
            return restPose;
        }

        if (dragon.isLeaping() || dragon.getLeapAnimState() != 0) {
            controller.transitionLength(GROUND_TRANSITIONS.bodyTransition());
            AnimationHelper.setAndContinue(state, LEAP_TAKEOFF);
            return PlayState.CONTINUE;
        }

        if (!aerialState && dragon.getEntityData().get(Ignivorus.DATA_BULLDOZING)) {
            float riderForward = dragon.getEntityData().get(Ignivorus.DATA_RIDER_FORWARD);
            float riderStrafe = dragon.getEntityData().get(Ignivorus.DATA_RIDER_STRAFE);
            boolean isMoving = Math.abs(riderForward) > 0.01f || Math.abs(riderStrafe) > 0.01f;
            if (isMoving) {
                controller.transitionLength(GROUND_TRANSITIONS.moving());
                AnimationHelper.setAndContinue(state, BULLDOZING);
            } else {
                controller.transitionLength(GROUND_TRANSITIONS.idle());
                AnimationHelper.setAndContinue(state, BULLDOZER_IDLE);
            }
            return PlayState.CONTINUE;
        }

        if (!aerialState && dragon.isInWaterOrBubble()) {
            controller.transitionLength(GROUND_TRANSITIONS.water());
            AnimationHelper.setAndContinue(state, SWIM);
            return PlayState.CONTINUE;
        }

        if (!aerialState && dragon.getEntityData().get(Ignivorus.DATA_PHASE2)) {
            if (dragon.isVehicle()) {
                float riderForward = dragon.getEntityData().get(Ignivorus.DATA_RIDER_FORWARD);
                float riderStrafe = dragon.getEntityData().get(Ignivorus.DATA_RIDER_STRAFE);
                boolean isMoving = Math.abs(riderForward) > 0.01f || Math.abs(riderStrafe) > 0.01f;

                if (isMoving) {
                    boolean isRunning = dragon.getEntityData().get(Ignivorus.DATA_ACCELERATING);
                    controller.transitionLength(GROUND_TRANSITIONS.moving());
                    AnimationHelper.setAndContinue(state, isRunning ? PHASE2_RUN : PHASE2_WALK);
                } else {
                    controller.transitionLength(GROUND_TRANSITIONS.idle());
                    AnimationHelper.setAndContinue(state, PHASE2_IDLE);
                }
            } else {
                int groundState = dragon.getEntityData().get(Ignivorus.DATA_GROUND_MOVE_STATE);
                switch (groundState) {
                    case 2 -> {
                        controller.transitionLength(GROUND_TRANSITIONS.moving());
                        AnimationHelper.setAndContinue(state, PHASE2_RUN);
                    }
                    case 1 -> {
                        controller.transitionLength(GROUND_TRANSITIONS.moving());
                        AnimationHelper.setAndContinue(state, PHASE2_WALK);
                    }
                    default -> {
                        controller.transitionLength(GROUND_TRANSITIONS.idle());
                        AnimationHelper.setAndContinue(state, PHASE2_IDLE);
                    }
                }
            }
            return PlayState.CONTINUE;
        }

        if (!aerialState && dragon.isVehicle()) {
            float riderForward = dragon.getEntityData().get(Ignivorus.DATA_RIDER_FORWARD);
            float riderStrafe = dragon.getEntityData().get(Ignivorus.DATA_RIDER_STRAFE);
            boolean isMoving = Math.abs(riderForward) > 0.01f || Math.abs(riderStrafe) > 0.01f;
            if (isMoving) {
                boolean isRunning = dragon.getEntityData().get(Ignivorus.DATA_ACCELERATING);
                controller.transitionLength(GROUND_TRANSITIONS.moving());
                AnimationHelper.setAndContinue(state, isRunning ? RUN : WALK);
            } else {
                controller.transitionLength(GROUND_TRANSITIONS.idle());
                AnimationHelper.setAndContinue(state, IDLE);
            }
            return PlayState.CONTINUE;
        }

        if (!aerialState && dragon.isFallingForAnimation()) {
            controller.transitionLength(GROUND_TRANSITIONS.falling());
            AnimationHelper.setAndContinue(state, FALLING);
            return PlayState.CONTINUE;
        }

        if (aerialState) {
            return PlayState.STOP;
        }

        int groundState = dragon.getEntityData().get(Ignivorus.DATA_GROUND_MOVE_STATE);
        switch (groundState) {
            case 2 -> {
                controller.transitionLength(GROUND_TRANSITIONS.moving());
                AnimationHelper.setAndContinue(state, RUN);
            }
            case 1 -> {
                controller.transitionLength(GROUND_TRANSITIONS.moving());
                AnimationHelper.setAndContinue(state, WALK);
            }
            default -> {
                controller.transitionLength(GROUND_TRANSITIONS.idle());
                AnimationHelper.setAndContinue(state, IDLE);
            }
        }
        return PlayState.CONTINUE;
    }
    public PlayState flightPredicate(AnimationState<Ignivorus> state) {
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
        DragonFlightStateEvaluator.VisualState visualState = dragon.getVisualFlightState(state.getPartialTick());
        if (visualState == DragonFlightStateEvaluator.VisualState.FLAP && dragon.isAccelerating()) {
            visualState = DragonFlightStateEvaluator.VisualState.SPRINT_FLAP;
        }
        return AnimationHelper.handleFlightState(state, visualState, FLIGHT_ANIMATIONS, FLIGHT_TRANSITIONS);
    }

    public PlayState actionPredicate(AnimationState<Ignivorus> state) {
        state.getController().transitionLength(ACTION_TRANSITION_TICKS);
        return PlayState.STOP;
    }

    public PlayState fastActionPredicate(AnimationState<Ignivorus> state) {
        state.getController().transitionLength(FAST_ACTION_TRANSITION_TICKS);
        return PlayState.STOP;
    }
}
