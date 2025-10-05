package com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers;

import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class RiftDrakeAnimationHandler {

    // Phase 1 animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rift_drake.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.rift_drake.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.rift_drake.run");

    // Phase 2 animations
    private static final RawAnimation IDLE2 = RawAnimation.begin().thenLoop("animation.rift_drake.idle2");
    private static final RawAnimation WALK2 = RawAnimation.begin().thenLoop("animation.rift_drake.walk2");
    private static final RawAnimation RUN2 = RawAnimation.begin().thenLoop("animation.rift_drake.run2");

    // Swim animations (shared between phases)
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.rift_drake.swim_idle");
    private static final RawAnimation SWIM_CRUISE = RawAnimation.begin().thenLoop("animation.rift_drake.swim_move");
    private static final RawAnimation SWIM_UP = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_up");
    private static final RawAnimation SWIM_DOWN = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_down");
    private static final RawAnimation SWIM_LEFT = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_left");
    private static final RawAnimation SWIM_RIGHT = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_right");
    private static final RawAnimation SWIM_NEUTRAL = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_off");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.rift_drake.sit");

    private final RiftDrakeEntity drake;

    public RiftDrakeAnimationHandler(RiftDrakeEntity drake) {
        this.drake = drake;
    }

    /**
     * Sets up all GeckoLib animation triggers for the action controller
     */
    public void setupActionController(AnimationController<RiftDrakeEntity> actionController) {
        // Register phase 2 transition animation
        actionController.triggerableAnim("phase2",
                RawAnimation.begin().thenPlay("animation.rift_drake.phase2"));

        // Phase 1 melee attack (works both on ground and underwater)
        actionController.triggerableAnim("bite",
                RawAnimation.begin().thenPlay("animation.rift_drake.bite"));

        // Phase 2 rage mode bite - faster
        actionController.triggerableAnim("bite2",
                RawAnimation.begin().thenPlay("animation.rift_drake.bite2"));

        // Phase 2 claw attacks (bipedal melee)
        actionController.triggerableAnim("claw_left",
                RawAnimation.begin().thenPlay("animation.rift_drake.claw_left"));
        actionController.triggerableAnim("claw_right",
                RawAnimation.begin().thenPlay("animation.rift_drake.claw_right"));
    }

    public PlayState movementPredicate(AnimationState<RiftDrakeEntity> state) {
        boolean isSwimming = drake.isSwimming();
        boolean isNavigating = drake.getNavigation().isInProgress() && drake.getNavigation().getPath() != null;
        double horizontalSpeedSq = drake.getDeltaMovement().horizontalDistanceSqr();
        boolean isMovingLand = state.isMoving() || horizontalSpeedSq > 0.008D;

        if (isSwimming) {
            RawAnimation swimAnim = drake.isSwimmingMoving() || isNavigating ? SWIM_CRUISE : SWIM_IDLE;
            state.setAnimation(swimAnim);
        } else if (drake.getSitProgress() > 0.5f) {
            // Drive SIT from our custom progress system only to avoid de-sync
            state.setAnimation(SIT);
        } else {
            // Ground movement transitions - use synced state for proper networking
            int groundState = drake.getEffectiveGroundState();
            boolean phaseTwo = drake.isPhaseTwoActive();

            if (groundState == 2) {
                // Running state
                state.getController().transitionLength(3); // Fast transition into run
                state.setAnimation(phaseTwo ? RUN2 : RUN);
            } else if (groundState == 1 || isMovingLand) {
                // Walking state or moving
                state.getController().transitionLength(3); // Quick walk engage
                state.setAnimation(phaseTwo ? WALK2 : WALK);
            } else {
                // Idle state
                state.getController().transitionLength(4); // Soft transition to idle
                state.setAnimation(phaseTwo ? IDLE2 : IDLE);
            }
        }
        state.getController().setAnimationSpeed(1.0F);
        return PlayState.CONTINUE;
    }

    public PlayState swimDirectionPredicate(AnimationState<RiftDrakeEntity> state) {
        var controller = state.getController();
        controller.transitionLength(6);

        if (!drake.isSwimming()) {
            state.setAndContinue(SWIM_NEUTRAL);
            return PlayState.CONTINUE;
        }

        boolean pitchingUp = drake.isSwimmingUp();
        boolean pitchingDown = drake.isSwimmingDown();
        int yawDir = drake.getSwimTurnDirection();

        if (pitchingUp) {
            state.setAndContinue(SWIM_UP);
        } else if (pitchingDown) {
            state.setAndContinue(SWIM_DOWN);
        } else if (yawDir < 0) {
            state.setAndContinue(SWIM_LEFT);
        } else if (yawDir > 0) {
            state.setAndContinue(SWIM_RIGHT);
        } else {
            state.setAndContinue(SWIM_NEUTRAL);
        }
        return PlayState.CONTINUE;
    }

    public PlayState actionPredicate(AnimationState<RiftDrakeEntity> state) {
        // Action controller handles one-shot animations triggered via triggerAnim()
        state.getController().transitionLength(5);
        // Return STOP so this controller doesn't compete with movement controller when idle
        return PlayState.STOP;
    }
}
