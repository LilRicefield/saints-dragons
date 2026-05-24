package com.leon.saintsdragons.server.entity.dragons.varasuchus.handlers;

import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.util.animation.DragonInteractionAnimationHelper;
import com.leon.saintsdragons.util.animation.MovementAnimationHelper;
import com.leon.saintsdragons.util.animation.DragonStateAnimationHelper;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public record VarasuchusAnimationHandler(Varasuchus drake) {
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
    private static final int MOVEMENT_TRANSITION_TICKS = 6;
    private static final int SWIM_TRANSITION_TICKS = 7;

    public void setupActionController(AnimationController<Varasuchus> controller) {
        controller.triggerableAnim("bite",
                RawAnimation.begin().thenPlay("animation.varasuchus.bite"));
        controller.triggerableAnim("bite2",
                RawAnimation.begin().thenPlay("animation.varasuchus.bite2"));
        controller.triggerableAnim("horn_gore",
                RawAnimation.begin().thenPlay("animation.varasuchus.horn_gore"));
        controller.triggerableAnim("tail_swipe_left",
                RawAnimation.begin().thenPlay("animation.varasuchus.tail_swipe_left"));
        controller.triggerableAnim("phase2_dash_left",
                RawAnimation.begin().thenPlay("animation.varasuchus.phase2_dash_left"));
        controller.triggerableAnim("phase2_dash_right",
                RawAnimation.begin().thenPlay("animation.varasuchus.phase2_dash_right"));
        controller.triggerableAnim("flex", FLEX);
        controller.triggerableAnim("flex2", FLEX2);
    }

    public PlayState movementPredicate(AnimationState<Varasuchus> state) {
        if (drake.isDying()) {
            return PlayState.STOP;
        }
        var controller = state.getController();
        controller.setAnimationSpeed(1.0F);

        if (drake.isWildRideAnimationActive()) {
            controller.transitionLength(2);
            state.setAndContinue(drake.isInWaterOrBubble() ? THRASHING_UNDERWATER : BUCKING);
            return PlayState.CONTINUE;
        }
        if (drake.isRiddenGroundJumpAirborne()) {
            controller.transitionLength(1);
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
            controller.transitionLength(SWIM_TRANSITION_TICKS);

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
            MovementAnimationHelper.setAndContinue(state, swimAnim);
        } else {
            PlayState restPose = MovementAnimationHelper.tryHandleRestPose(
                    state, drake, SLEEP_LOOP, SIT, 6, 4
            );
            if (restPose != null) {
                return restPose;
            }

            int groundState = drake.getEffectiveGroundState();
            boolean phaseTwo = drake.isPhaseTwoActive();
            boolean abilityActive = drake.getActiveAbility() != null;
            boolean riderControlled = drake.isVehicle() && drake.getControllingPassenger() instanceof Player player && drake.isOwnedBy(player);
            boolean isAggressive = drake.shouldUseRunAnimation() && isMovingLand;
            int baseTransition = MOVEMENT_TRANSITION_TICKS;
            if (riderControlled) {
                baseTransition = Math.max(3, baseTransition - 2);
            }
            if (abilityActive) {
                baseTransition = Math.max(2, baseTransition - 1);
            }

            if (groundState == 2 || isAggressive) {
                controller.transitionLength(baseTransition);
                MovementAnimationHelper.setAndContinue(state, phaseTwo ? RUN2 : RUN);
            } else if (groundState == 1 || isMovingLand) {
                controller.transitionLength(Math.max(3, baseTransition + 1));
                MovementAnimationHelper.setAndContinue(state, phaseTwo ? WALK2 : WALK);
            } else {
                controller.transitionLength(Math.max(3, baseTransition + (abilityActive ? 2 : 0)));
                MovementAnimationHelper.setAndContinue(state, phaseTwo ? IDLE2 : IDLE);
            }
        }
        return PlayState.CONTINUE;
    }

    public PlayState actionPredicate(AnimationState<Varasuchus> state) {
        state.getController().transitionLength(4);
        return PlayState.STOP;
    }
    public PlayState fastActionPredicate(AnimationState<Varasuchus> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }
    public void setupStateController(AnimationController<Varasuchus> controller) {
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.SIT_DOWN, SIT_DOWN);
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.SIT_UP, SIT_UP);
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.FALL_ASLEEP, FALL_ASLEEP);
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.SLEEP, SLEEP_LOOP);
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.WAKE_UP, WAKE_UP);
        DragonStateAnimationHelper.register(controller, "sit_down2", SIT_DOWN2);
        DragonStateAnimationHelper.register(controller, "sit_up2", SIT_UP2);
    }
    public void triggerSitDownAnimation() {
        boolean isPhaseTwo = drake.isPhaseTwoActive();
        drake.triggerAnim(DragonStateAnimationHelper.CONTROLLER, isPhaseTwo ? "sit_down2" : DragonStateAnimationHelper.SIT_DOWN);
    }
    public void triggerSitUpAnimation() {
        boolean isPhaseTwo = drake.isPhaseTwoActive();
        drake.triggerAnim(DragonStateAnimationHelper.CONTROLLER, isPhaseTwo ? "sit_up2" : DragonStateAnimationHelper.SIT_UP);
    }
    public void triggerFallAsleepAnimation() {
        drake.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.FALL_ASLEEP);
    }
    public void triggerSleepAnimation() {
        drake.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.SLEEP);
    }
    public void triggerWakeUpAnimation() {
        drake.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.WAKE_UP);
    }
    public void triggerFlexAnimation() {
        drake.triggerAnim(ACTION_CONTROLLER, drake.isPhaseTwoActive() ? "flex2" : "flex");
    }
    public void setupFastActionController(AnimationController<Varasuchus> controller) {
        controller.triggerableAnim("phase1",
                RawAnimation.begin().thenPlay("animation.varasuchus.phase1"));
        controller.triggerableAnim("phase2",
                RawAnimation.begin().thenPlay("animation.varasuchus.phase2"));
        controller.triggerableAnim("phase2_underwater",
                RawAnimation.begin().thenPlay("animation.varasuchus.phase2_underwater"));
        controller.triggerableAnim("phase1_underwater",
                RawAnimation.begin().thenPlay("animation.varasuchus.phase1_underwater"));
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
        controller.triggerableAnim("tailguard",
                RawAnimation.begin().thenPlay("animation.varasuchus.tailguard"));
        controller.triggerableAnim("tailguard_hold",
                RawAnimation.begin().thenLoop("animation.varasuchus.tailguard_hold"));
        controller.triggerableAnim("tailguard_cancel",
                RawAnimation.begin().thenPlay("animation.varasuchus.tailguard_cancel"));
        controller.triggerableAnim("tailguard_parry",
                RawAnimation.begin().thenPlay("animation.varasuchus.tailguard_parry"));
    }
    public void setupInteractionController(AnimationController<Varasuchus> controller) {
        controller.triggerableAnim(DragonInteractionAnimationHelper.EAT,
                RawAnimation.begin().thenPlay("animation.varasuchus.eat"));
        controller.triggerableAnim("varasuchus_hurt",
                RawAnimation.begin().thenPlay("animation.varasuchus.hurt"));
        controller.triggerableAnim("varasuchus_die",
                RawAnimation.begin().thenPlay("animation.varasuchus.die"));
    }
}
