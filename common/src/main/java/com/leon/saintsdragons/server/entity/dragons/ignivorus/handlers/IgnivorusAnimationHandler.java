package com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;


public record IgnivorusAnimationHandler(Ignivorus dragon) {

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

    public PlayState handleMovementAnimation(AnimationState<Ignivorus> state) {
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
        if (dragon.isSleeping() && !dragon.isSleepingEntering() && !dragon.isSleepingExiting()) {
            state.getController().transitionLength(6);
            state.setAndContinue(SLEEP);
            return PlayState.CONTINUE;
        } else if (dragon.isSleepingEntering() || dragon.isSleepingExiting()) {
            return PlayState.STOP;
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

        if (!aerialState && (dragon.isOrderedToSit() || dragon.getSitProgress() > 0.5f)) {
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
        dragon.triggerAnim("action", "sit_down");
    }

    public void triggerSitUpAnimation() {
        dragon.triggerAnim("action", "sit_up");
    }

    public void triggerFallAsleepAnimation() {
        dragon.triggerAnim("action", "fall_asleep");
    }

    public void triggerSleepAnimation() {
        dragon.triggerAnim("action", "sleep");
    }

    public void triggerWakeUpAnimation() {
        dragon.triggerAnim("action", "wake_up");
    }

    public void triggerBulldozeEnterAnimation() {
        dragon.triggerAnim("action", "bulldozer_enter");
    }

    public void triggerBulldozeExitAnimation() {
        dragon.triggerAnim("action", "bulldozer_exit");
    }

    public void triggerPhase2EnterAnimation() {
        dragon.triggerAnim("action", "phase2_enter");
    }

    public void triggerPhase2ExitAnimation() {
        dragon.triggerAnim("action", "phase2_exit");
    }

    public void triggerLeapImpactAnimation() {
        dragon.triggerAnim("action", "leap_impact");
    }

    public void setupActionController(AnimationController<Ignivorus> actionController) {
        actionController.triggerableAnim("sit_down",
            RawAnimation.begin().thenPlay("animation.ignivorus.down"));
        actionController.triggerableAnim("sit_up",
            RawAnimation.begin().thenPlay("animation.ignivorus.up"));
        actionController.triggerableAnim("fall_asleep",
            RawAnimation.begin().thenPlay("animation.ignivorus.fall_asleep"));
        actionController.triggerableAnim("wake_up",
            RawAnimation.begin().thenPlay("animation.ignivorus.wake_up"));
        actionController.triggerableAnim("sleep",
            RawAnimation.begin().thenLoop("animation.ignivorus.sleep"));
        actionController.triggerableAnim("bite",
            RawAnimation.begin().thenPlay("animation.ignivorus.bite"));
        actionController.triggerableAnim("eat",
            RawAnimation.begin().thenPlay("animation.ignivorus.eat"));
        actionController.triggerableAnim("stomp_left",
            RawAnimation.begin().thenPlay("animation.ignivorus.ignivorus_stomp_left"));
        actionController.triggerableAnim("stomp_right",
            RawAnimation.begin().thenPlay("animation.ignivorus.ignivorus_stomp_right"));
        actionController.triggerableAnim("body_slam",
            RawAnimation.begin().thenPlay("animation.ignivorus.body_slam"));
        actionController.triggerableAnim("leap_impact",
            RawAnimation.begin().thenPlay("animation.ignivorus.ignivorus_impact"));
        actionController.triggerableAnim("fire_breath_start",
            RawAnimation.begin().thenPlay("animation.ignivorus.fire_breath_start"));
        actionController.triggerableAnim("fire_breathing",
            RawAnimation.begin().thenLoop("animation.ignivorus.fire_breathing"));
        actionController.triggerableAnim("fire_breath_stop",
            RawAnimation.begin().thenPlay("animation.ignivorus.fire_breath_end"));
        actionController.triggerableAnim("fireball_level1_charge",
            RawAnimation.begin().thenPlay("animation.ignivorus.fireball_level1_charge"));
        actionController.triggerableAnim("fireball_level2_charge",
            RawAnimation.begin().thenPlay("animation.ignivorus.fireball_level2_charge"));
        actionController.triggerableAnim("fireball_level3_charge",
            RawAnimation.begin().thenPlay("animation.ignivorus.fireball_level3_charge"));
        actionController.triggerableAnim("fireball_level3_hold",
            RawAnimation.begin().thenLoop("animation.ignivorus.fireball_level3_hold"));
        actionController.triggerableAnim("fireball_level1_shoot",
            RawAnimation.begin().thenPlay("animation.ignivorus.fireball_level1_shoots"));
        actionController.triggerableAnim("fireball_level2_shoot",
            RawAnimation.begin().thenPlay("animation.ignivorus.fireball_level2_shoots"));
        actionController.triggerableAnim("fireball_level3_shoot",
            RawAnimation.begin().thenPlay("animation.ignivorus.fireball_level3_shoots"));
        actionController.triggerableAnim("roar",
            RawAnimation.begin().thenPlay("animation.ignivorus.roar"));
        actionController.triggerableAnim("ultimate_start",
            RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_start"));
        actionController.triggerableAnim("ultimate",
            RawAnimation.begin().thenPlay("animation.ignivorus.ultimate"));
        actionController.triggerableAnim("ultimate_end",
            RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_end"));
        actionController.triggerableAnim("ultimate_start_air",
            RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_start_air"));
        actionController.triggerableAnim("ultimate_air",
            RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_air"));
        actionController.triggerableAnim("ultimate_end_air",
            RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_end_air"));
        actionController.triggerableAnim("phase2_ultimate", PHASE2_ULTIMATE);
        actionController.triggerableAnim("landed", LANDED);
        actionController.triggerableAnim("phase2_landed", PHASE2_LANDED);
        actionController.triggerableAnim("bulldozer_enter",
            RawAnimation.begin().thenPlay("animation.ignivorus.bulldozer_enter"));
        actionController.triggerableAnim("bulldozer_exit",
            RawAnimation.begin().thenPlay("animation.ignivorus.bulldozer_exit"));
        actionController.triggerableAnim("phase2_enter",
            RawAnimation.begin().thenPlay("animation.ignivorus.phase2_enter"));
        actionController.triggerableAnim("phase2_exit",
            RawAnimation.begin().thenPlay("animation.ignivorus.phase2_exit"));
        actionController.triggerableAnim("ignivorus_grumble1",
            RawAnimation.begin().thenPlay("animation.ignivorus.grumble1"));
        actionController.triggerableAnim("ignivorus_grumble2",
            RawAnimation.begin().thenPlay("animation.ignivorus.grumble2"));
        actionController.triggerableAnim("ignivorus_grumble3",
            RawAnimation.begin().thenPlay("animation.ignivorus.grumble3"));
    }

    public PlayState instantActionPredicate(AnimationState<Ignivorus> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public void setupInstantActionController(AnimationController<Ignivorus> controller) {
        controller.triggerableAnim("takeoff", TAKEOFF);
        controller.triggerableAnim("phase2_takeoff", PHASE2_TAKEOFF);
        controller.triggerableAnim("wing_swipe_left",
                RawAnimation.begin().thenPlay("animation.ignivorus.wing_swipe_left"));
        controller.triggerableAnim("wing_swipe_right",
                RawAnimation.begin().thenPlay("animation.ignivorus.wing_swipe_right"));
        controller.triggerableAnim("ignivorus_hurt",
                RawAnimation.begin().thenPlay("animation.ignivorus.hurt"));
        controller.triggerableAnim("die",
                RawAnimation.begin().thenPlay("animation.ignivorus.die"));
    }
}
