package com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonInteractionAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonMovementAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonStateAnimationHelper;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;


public record IgnivorusAnimationHandler(Ignivorus dragon) {
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

    public PlayState movementPredicate(AnimationState<Ignivorus> state) {
        state.getController().transitionLength(6);
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
            state.getController().transitionLength(4);
            state.setAndContinue(STUNNED);
            return PlayState.CONTINUE;
        }
        PlayState restPose = DragonMovementAnimationHelper.tryHandleRestPose(
                state, dragon, SLEEP, SIT, 6, 0, !aerialState
        );
        if (restPose != null) {
            return restPose;
        }

        if (dragon.isLeaping() || dragon.getLeapAnimState() != 0) {
            state.getController().transitionLength(2);
            state.setAndContinue(LEAP_TAKEOFF);
            return PlayState.CONTINUE;
        }

        if (!aerialState && dragon.getEntityData().get(Ignivorus.DATA_BULLDOZING)) {
            float riderForward = dragon.getEntityData().get(Ignivorus.DATA_RIDER_FORWARD);
            float riderStrafe = dragon.getEntityData().get(Ignivorus.DATA_RIDER_STRAFE);
            boolean isMoving = Math.abs(riderForward) > 0.01f || Math.abs(riderStrafe) > 0.01f;
            if (isMoving) {
                state.setAndContinue(BULLDOZING);
            } else {
                state.setAndContinue(BULLDOZER_IDLE);
            }
            return PlayState.CONTINUE;
        }

        if (!aerialState && dragon.getEntityData().get(Ignivorus.DATA_PHASE2)) {
            if (dragon.isVehicle()) {
                float riderForward = dragon.getEntityData().get(Ignivorus.DATA_RIDER_FORWARD);
                float riderStrafe = dragon.getEntityData().get(Ignivorus.DATA_RIDER_STRAFE);
                boolean isMoving = Math.abs(riderForward) > 0.01f || Math.abs(riderStrafe) > 0.01f;

                if (isMoving) {
                    boolean isRunning = dragon.getEntityData().get(Ignivorus.DATA_ACCELERATING);
                    state.setAndContinue(isRunning ? PHASE2_RUN : PHASE2_WALK);
                } else {
                    state.setAndContinue(PHASE2_IDLE);
                }
            } else {
                int groundState = dragon.getEntityData().get(Ignivorus.DATA_GROUND_MOVE_STATE);
                switch (groundState) {
                    case 2 -> state.setAndContinue(PHASE2_RUN);
                    case 1 -> state.setAndContinue(PHASE2_WALK);
                    default -> state.setAndContinue(PHASE2_IDLE);
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
                state.setAndContinue(isRunning ? RUN : WALK);
            } else {
                state.setAndContinue(IDLE);
            }
            return PlayState.CONTINUE;
        }

        if (!dragon.isVehicle() && !aerialState && dragon.getCommand() == 1) {
            state.setAndContinue(SIT);
            return PlayState.CONTINUE;
        }

        if (!aerialState && dragon.isInWaterOrBubble()) {
            state.setAndContinue(SWIM);
            return PlayState.CONTINUE;
        }

        if (dragon.isFallingForAnimation()) {
            state.getController().transitionLength(4);
            state.setAndContinue(FALLING);
            return PlayState.CONTINUE;
        }

        if (aerialState) {
            boolean sprinting = dragon.isAccelerating();
            DragonFlightStateEvaluator.VisualState visualState = dragon.getVisualFlightState(state.getPartialTick());

            if (visualState == DragonFlightStateEvaluator.VisualState.TAKEOFF
                    || dragon.isTakeoff()
                    || (dragon.isFlying() && dragon.timeFlying < Ignivorus.TAKEOFF_ANIMATION_TICKS)) {
                state.getController().transitionLength(4);
                if (dragon.getEntityData().get(Ignivorus.DATA_PHASE2)) {
                    state.setAndContinue(PHASE2_TAKEOFF);
                } else {
                    state.setAndContinue(TAKEOFF);
                }
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.LANDING) {
                state.getController().transitionLength(4);
                state.setAndContinue(LANDING);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.GLIDE_DOWN) {
                state.getController().transitionLength(6);
                state.setAndContinue(GLIDE_DOWN);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.FLY_IDLE) {
                state.getController().transitionLength(6);
                state.setAndContinue(FLY_IDLE);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.SPRINT_FLAP) {
                state.getController().transitionLength(3);
                state.setAndContinue(SPRINT_FLAP);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.FLAP) {
                state.getController().transitionLength(4);
                state.setAndContinue(sprinting ? SPRINT_FLAP : FLAP);
                return PlayState.CONTINUE;
            }

            state.getController().transitionLength(12);
            state.setAndContinue(GLIDE);
        } else {
            int groundState = dragon.getEntityData().get(Ignivorus.DATA_GROUND_MOVE_STATE);

            switch (groundState) {
                case 2 -> state.setAndContinue(RUN);
                case 1 -> state.setAndContinue(WALK);
                default -> state.setAndContinue(IDLE);
            }
        }
        return PlayState.CONTINUE;
    }

    public void triggerSitDownAnimation() {
        dragon.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.SIT_DOWN);
    }

    public void triggerSitUpAnimation() {
        dragon.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.SIT_UP);
    }

    public void triggerFallAsleepAnimation() {
        dragon.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.FALL_ASLEEP);
    }

    public void triggerSleepAnimation() {
        dragon.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.SLEEP);
    }

    public void triggerWakeUpAnimation() {
        dragon.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.WAKE_UP);
    }

    public void triggerBulldozeEnterAnimation() {
        dragon.triggerAnim(FAST_ACTION_CONTROLLER, "bulldozer_enter");
    }

    public void triggerBulldozeExitAnimation() {
        dragon.triggerAnim(FAST_ACTION_CONTROLLER, "bulldozer_exit");
    }

    public void triggerPhase2EnterAnimation() {
        dragon.triggerAnim(FAST_ACTION_CONTROLLER, "phase2_enter");
    }

    public void triggerPhase2ExitAnimation() {
        dragon.triggerAnim(FAST_ACTION_CONTROLLER, "phase2_exit");
    }

    public void triggerLeapImpactAnimation() {
        dragon.triggerAnim(FAST_ACTION_CONTROLLER, "leap_impact");
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

    public void setupStateController(AnimationController<Ignivorus> controller) {
        DragonStateAnimationHelper.registerStandard(controller, "ignivorus");
    }

    public PlayState fastActionPredicate(AnimationState<Ignivorus> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public PlayState actionPredicate(AnimationState<Ignivorus> state) {
        state.getController().transitionLength(4);
        return PlayState.STOP;
    }

    public void setupInteractionController(AnimationController<Ignivorus> controller) {
        controller.triggerableAnim("ignivorus_hurt",
                RawAnimation.begin().thenPlay("animation.ignivorus.hurt"));
        controller.triggerableAnim(DragonInteractionAnimationHelper.DIE,
                RawAnimation.begin().thenPlay("animation.ignivorus.die"));
        controller.triggerableAnim(DragonInteractionAnimationHelper.EAT,
                RawAnimation.begin().thenPlay("animation.ignivorus.eat"));
    }
    public void setupFastActionController(AnimationController<Ignivorus> controller) {
        controller.triggerableAnim("takeoff", TAKEOFF);
        controller.triggerableAnim("phase2_takeoff", PHASE2_TAKEOFF);
        controller.triggerableAnim("wing_swipe_left",
                RawAnimation.begin().thenPlay("animation.ignivorus.wing_swipe_left"));
        controller.triggerableAnim("wing_swipe_right",
                RawAnimation.begin().thenPlay("animation.ignivorus.wing_swipe_right"));
        controller.triggerableAnim("landed", LANDED);
        controller.triggerableAnim("phase2_landed", PHASE2_LANDED);
        controller.triggerableAnim("bulldozer_enter",
                RawAnimation.begin().thenPlay("animation.ignivorus.bulldozer_enter"));
        controller.triggerableAnim("bulldozer_exit",
                RawAnimation.begin().thenPlay("animation.ignivorus.bulldozer_exit"));
        controller.triggerableAnim("phase2_enter",
                RawAnimation.begin().thenPlay("animation.ignivorus.phase2_enter"));
        controller.triggerableAnim("phase2_exit",
                RawAnimation.begin().thenPlay("animation.ignivorus.phase2_exit"));
        controller.triggerableAnim("stomp_left",
                RawAnimation.begin().thenPlay("animation.ignivorus.ignivorus_stomp_left"));
        controller.triggerableAnim("stomp_right",
                RawAnimation.begin().thenPlay("animation.ignivorus.ignivorus_stomp_right"));
        controller.triggerableAnim("ignivorus_flex",
                RawAnimation.begin().thenPlay("animation.ignivorus.flex"));
        controller.triggerableAnim("leap_impact",
                RawAnimation.begin().thenPlay("animation.ignivorus.ignivorus_impact"));
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
        controller.triggerableAnim("ultimate_start",
                RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_start"));
        controller.triggerableAnim("ultimate",
                RawAnimation.begin().thenPlay("animation.ignivorus.ultimate"));
        controller.triggerableAnim("ultimate_end",
                RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_end"));
        controller.triggerableAnim("ultimate_start_air",
                RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_start_air"));
        controller.triggerableAnim("ultimate_air",
                RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_air"));
        controller.triggerableAnim("ultimate_end_air",
                RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_end_air"));
        controller.triggerableAnim("phase2_ultimate", PHASE2_ULTIMATE);
    }
}
