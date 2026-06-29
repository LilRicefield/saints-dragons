package com.leon.saintsdragons.server.entity.dragons.varasuchus.handlers;

import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public record VarasuchusAnimationHandler(Varasuchus drake) {
    public static final String MOVEMENT_CONTROLLER = AnimationHelper.MOVEMENT_CONTROLLER;
    public static final String FAST_ACTION_CONTROLLER = "varasuchusFastAction";
    public static final String ACTION_CONTROLLER = "varasuchusAction";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.varasuchus.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.varasuchus.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.varasuchus.run");
    private static final RawAnimation BUCKING = RawAnimation.begin().thenLoop("animation.varasuchus.bucking");
    private static final RawAnimation THRASHING_UNDERWATER = RawAnimation.begin().thenLoop("animation.varasuchus.thrashing_underwater");
    private static final RawAnimation IDLE2 = RawAnimation.begin().thenLoop("animation.varasuchus.idle2");
    private static final RawAnimation WALK2 = RawAnimation.begin().thenLoop("animation.varasuchus.walk2");
    private static final RawAnimation RUN2 = RawAnimation.begin().thenLoop("animation.varasuchus.run2");
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.varasuchus.swim_idle");
    private static final RawAnimation SWIM_MOVE = RawAnimation.begin().thenLoop("animation.varasuchus.swim_move");
    private static final RawAnimation JUMP = RawAnimation.begin().thenLoop("animation.varasuchus.jump");
    private static final RawAnimation JUMP2 = RawAnimation.begin().thenLoop("animation.varasuchus.jump2");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.varasuchus.sit");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.varasuchus.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.varasuchus.up");
    private static final RawAnimation FALL_ASLEEP = RawAnimation.begin().thenPlay("animation.varasuchus.fall_asleep");
    private static final RawAnimation SLEEP_LOOP = RawAnimation.begin().thenLoop("animation.varasuchus.sleep");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("animation.varasuchus.wake_up");
    private static final RawAnimation SIT_DOWN2 = RawAnimation.begin().thenPlay("animation.varasuchus.down2");
    private static final RawAnimation SIT_UP2 = RawAnimation.begin().thenPlay("animation.varasuchus.up2");
    private static final RawAnimation FLEX = RawAnimation.begin().thenPlay("animation.varasuchus.flex");
    private static final RawAnimation FLEX2 = RawAnimation.begin().thenPlay("animation.varasuchus.flex2");
    private static final RawAnimation BITE2 = RawAnimation.begin().thenPlay("animation.varasuchus.bite2");
    private static final RawAnimation HORN_GORE = RawAnimation.begin().thenPlay("animation.varasuchus.horn_gore");
    private static final RawAnimation TAIL_SWIPE_LEFT = RawAnimation.begin().thenPlay("animation.varasuchus.tail_swipe_left");
    private static final RawAnimation PHASE2_DASH_LEFT = RawAnimation.begin().thenPlay("animation.varasuchus.phase2_dash_left");
    private static final RawAnimation PHASE2_DASH_RIGHT = RawAnimation.begin().thenPlay("animation.varasuchus.phase2_dash_right");
    private static final RawAnimation PHASE1 = RawAnimation.begin().thenPlay("animation.varasuchus.phase1");
    private static final RawAnimation PHASE2 = RawAnimation.begin().thenPlay("animation.varasuchus.phase2");
    private static final RawAnimation PHASE1_UNDERWATER = RawAnimation.begin().thenPlay("animation.varasuchus.phase1_underwater");
    private static final RawAnimation PHASE2_UNDERWATER = RawAnimation.begin().thenPlay("animation.varasuchus.phase2_underwater");
    private static final RawAnimation TAILGUARD = RawAnimation.begin().thenPlay("animation.varasuchus.tailguard");
    private static final RawAnimation TAILGUARD_HOLD = RawAnimation.begin().thenLoop("animation.varasuchus.tailguard_hold");
    private static final RawAnimation TAILGUARD_CANCEL = RawAnimation.begin().thenPlay("animation.varasuchus.tailguard_cancel");
    private static final RawAnimation TAILGUARD_PARRY = RawAnimation.begin().thenPlay("animation.varasuchus.tailguard_parry");
    private static final AnimationHelper.Animations GROUND_ANIMATIONS =
            new AnimationHelper.Animations(IDLE, WALK, RUN, SIT, SIT_DOWN, SIT_UP, FALL_ASLEEP, SLEEP_LOOP, WAKE_UP, SWIM_MOVE, null, JUMP);
    private static final AnimationHelper.Transitions GROUND_TRANSITIONS =
            new AnimationHelper.Transitions(4, 3, 4, 4, 4, 4, 4, 4);
    private static final int ACTION_TRANSITION_TICKS = 4;
    private static final int FAST_ACTION_TRANSITION_TICKS = 1;

    public void setupActionController(AnimationController<Varasuchus> controller) {
        controller.triggerableAnim("bite",
                RawAnimation.begin().thenPlay("animation.varasuchus.bite"));
        controller.triggerableAnim("bite2", BITE2);
        controller.triggerableAnim("horn_gore", HORN_GORE);
    }

    public PlayState movementPredicate(AnimationState<Varasuchus> state) {
        if (drake.isDying()) {
            return PlayState.STOP;
        }
        var controller = state.getController();
        controller.setAnimationSpeed(1.0F);

        if (drake.isWildRideAnimationActive()) {
            controller.transitionLength(GROUND_TRANSITIONS.stunned());
            state.setAndContinue(drake.isInWaterOrBubble() ? THRASHING_UNDERWATER : BUCKING);
            return PlayState.CONTINUE;
        }
        if (drake.isRiddenGroundJumpAirborne()) {
            controller.transitionLength(GROUND_TRANSITIONS.bodyTransition());
            state.setAndContinue(drake.isPhaseTwoActive() ? JUMP2 : JUMP);
            return PlayState.CONTINUE;
        }
        if (drake.areRiderControlsLocked()) {
            return PlayState.STOP;
        }

        boolean isSwimming = drake.isSwimming();
        boolean isInWater = drake.isInWaterOrBubble();
        boolean isNavigating = drake.getNavigation().isInProgress() && drake.getNavigation().getPath() != null;
        double totalSpeedSq = drake.getDeltaMovement().lengthSqr();
        boolean isMovingLand = state.isMoving();

        if (isSwimming || isInWater) {
            controller.transitionLength(GROUND_TRANSITIONS.water());

            boolean isSwimmingMoving;

            if (drake.isVehicle() && drake.getControllingPassenger() != null) {
                float riderFwd = Math.abs(drake.getLastRiderForward());
                float riderStr = Math.abs(drake.getLastRiderStrafe());
                boolean riderMoving = riderFwd > 0.05F || riderStr > 0.05F;
                isSwimmingMoving = riderMoving || totalSpeedSq > 0.004D;
            } else {
                isSwimmingMoving = drake.isSwimmingMoving() || isNavigating ||
                        totalSpeedSq > 0.002D ||
                        Math.abs(drake.zza) > 0.01F ||
                        Math.abs(drake.yya) > 0.01F;
            }

            RawAnimation swimAnim = isSwimmingMoving ? SWIM_MOVE : SWIM_IDLE;
            AnimationHelper.setAndContinue(state, swimAnim);
        } else {
            PlayState restPose = AnimationHelper.tryHandleRestPose(
                    state, drake, SLEEP_LOOP, SIT, GROUND_TRANSITIONS.sleep(), GROUND_TRANSITIONS.sit()
            );
            if (restPose != null) {
                return restPose;
            }

            PlayState dance = AnimationHelper.tryHandleDance(state, drake, GROUND_TRANSITIONS.idle());
            if (dance != null) {
                return dance;
            }

            int groundState = drake.getEffectiveGroundState();
            boolean phaseTwo = drake.isPhaseTwoActive();
            boolean isAggressive = drake.shouldUseRunAnimation() && isMovingLand;

            if (groundState == 2 || isAggressive) {
                controller.transitionLength(GROUND_TRANSITIONS.moving());
                AnimationHelper.setAndContinue(state, phaseTwo ? RUN2 : RUN);
            } else if (groundState == 1 || isMovingLand) {
                controller.transitionLength(GROUND_TRANSITIONS.moving());
                AnimationHelper.setAndContinue(state, phaseTwo ? WALK2 : WALK);
            } else {
                controller.transitionLength(GROUND_TRANSITIONS.idle());
                AnimationHelper.setAndContinue(state, phaseTwo ? IDLE2 : IDLE);
            }
        }
        return PlayState.CONTINUE;
    }

    public PlayState actionPredicate(AnimationState<Varasuchus> state) {
        state.getController().transitionLength(ACTION_TRANSITION_TICKS);
        return PlayState.STOP;
    }
    public PlayState fastActionPredicate(AnimationState<Varasuchus> state) {
        state.getController().transitionLength(FAST_ACTION_TRANSITION_TICKS);
        return PlayState.STOP;
    }
    public void setupMovementController(AnimationController<Varasuchus> controller) {
        AnimationHelper.register(controller, GROUND_ANIMATIONS);
        AnimationHelper.register(controller, "tail_swipe_left", TAIL_SWIPE_LEFT);
        AnimationHelper.register(controller, "phase2_dash_left", PHASE2_DASH_LEFT);
        AnimationHelper.register(controller, "phase2_dash_right", PHASE2_DASH_RIGHT);
        AnimationHelper.register(controller, "flex", FLEX);
        AnimationHelper.register(controller, "flex2", FLEX2);
        AnimationHelper.register(controller, "phase1", PHASE1);
        AnimationHelper.register(controller, "phase2", PHASE2);
        AnimationHelper.register(controller, "phase1_underwater", PHASE1_UNDERWATER);
        AnimationHelper.register(controller, "phase2_underwater", PHASE2_UNDERWATER);
        AnimationHelper.register(controller, "tailguard", TAILGUARD);
        AnimationHelper.register(controller, "tailguard_hold", TAILGUARD_HOLD);
        AnimationHelper.register(controller, "tailguard_cancel", TAILGUARD_CANCEL);
        AnimationHelper.register(controller, "tailguard_parry", TAILGUARD_PARRY);
    }
    public void setupTransitionController(AnimationController<Varasuchus> controller) {
        AnimationHelper.registerTransitions(controller, GROUND_ANIMATIONS);
        AnimationHelper.register(controller, "sit_down2", SIT_DOWN2);
        AnimationHelper.register(controller, "sit_up2", SIT_UP2);
    }
    public void triggerSitDownAnimation() {
        boolean isPhaseTwo = drake.isPhaseTwoActive();
        drake.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, isPhaseTwo ? "sit_down2" : AnimationHelper.SIT_DOWN);
    }
    public void triggerSitUpAnimation() {
        boolean isPhaseTwo = drake.isPhaseTwoActive();
        drake.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, isPhaseTwo ? "sit_up2" : AnimationHelper.SIT_UP);
    }
    public void triggerFallAsleepAnimation() {
        drake.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, AnimationHelper.FALL_ASLEEP);
    }
    public void triggerWakeUpAnimation() {
        drake.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, AnimationHelper.WAKE_UP);
    }
    public void triggerFlexAnimation() {
        drake.triggerAnim(MOVEMENT_CONTROLLER, drake.isPhaseTwoActive() ? "flex2" : "flex");
    }
    public void setupFastActionController(AnimationController<Varasuchus> controller) {
        controller.triggerableAnim("tail_attack_right",
                RawAnimation.begin().thenPlay("animation.varasuchus.tail_attack_right"));
        controller.triggerableAnim("tail_attack_left",
                RawAnimation.begin().thenPlay("animation.varasuchus.tail_attack_left"));
        controller.triggerableAnim("claw_left",
                RawAnimation.begin().thenPlay("animation.varasuchus.claw_left"));
        controller.triggerableAnim("claw_right",
                RawAnimation.begin().thenPlay("animation.varasuchus.claw_right"));
        controller.triggerableAnim("run_and_claw_left",
                RawAnimation.begin().thenPlay("animation.varasuchus.run_and_claw_left"));
        controller.triggerableAnim("run_and_claw_right",
                RawAnimation.begin().thenPlay("animation.varasuchus.run_and_claw_right"));
        controller.triggerableAnim("slash_barrage",
                RawAnimation.begin().thenPlay("animation.varasuchus.slash_barrage"));
    }
    public void setupInteractionController(AnimationController<Varasuchus> controller) {
        controller.triggerableAnim(AnimationHelper.EAT,
                RawAnimation.begin().thenPlay("animation.varasuchus.eat"));
        controller.triggerableAnim("varasuchus_hurt",
                RawAnimation.begin().thenPlay("animation.varasuchus.hurt"));
        controller.triggerableAnim("varasuchus_die",
                RawAnimation.begin().thenPlay("animation.varasuchus.die"));
    }
}
