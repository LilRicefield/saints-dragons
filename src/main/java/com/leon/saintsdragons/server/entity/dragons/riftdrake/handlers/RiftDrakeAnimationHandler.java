package com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers;

import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class RiftDrakeAnimationHandler {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rift_drake.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.rift_drake.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.rift_drake.run");
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.rift_drake.swim_idle");
    private static final RawAnimation SWIM_CRUISE = RawAnimation.begin().thenLoop("animation.rift_drake.swim_move");
    private static final RawAnimation SWIM_UP = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_up");
    private static final RawAnimation SWIM_DOWN = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_down");
    private static final RawAnimation SWIM_LEFT = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_left");
    private static final RawAnimation SWIM_RIGHT = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_right");
    private static final RawAnimation SWIM_NEUTRAL = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_off");

    private final RiftDrakeEntity drake;

    public RiftDrakeAnimationHandler(RiftDrakeEntity drake) {
        this.drake = drake;
    }

    public PlayState movementPredicate(AnimationState<RiftDrakeEntity> state) {
        boolean isSwimming = drake.isSwimming();
        boolean isNavigating = drake.getNavigation().isInProgress() && drake.getNavigation().getPath() != null;
        double horizontalSpeedSq = drake.getDeltaMovement().horizontalDistanceSqr();
        boolean isMovingLand = state.isMoving() || horizontalSpeedSq > 0.008D;

        if (isSwimming) {
            RawAnimation swimAnim = drake.isSwimmingMoving() || isNavigating ? SWIM_CRUISE : SWIM_IDLE;
            state.setAnimation(swimAnim);
        } else {
            // Ground movement transitions - use synced state for proper networking
            int groundState = drake.getEffectiveGroundState();
            
            if (groundState == 2) {
                // Running state
                state.getController().transitionLength(3); // Fast transition into run
                state.setAnimation(RUN);
            } else if (groundState == 1 || isMovingLand) {
                // Walking state or moving
                state.getController().transitionLength(3); // Quick walk engage
                state.setAnimation(WALK);
            } else {
                // Idle state
                state.getController().transitionLength(4); // Soft transition to idle
                state.setAnimation(IDLE);
            }
        }
        state.getController().setAnimationSpeed(1.0F);
        return PlayState.CONTINUE;
    }

    private RawAnimation selectSwimBodyAnimation(boolean moving) {
        if (!drake.isSwimming()) {
            return SWIM_NEUTRAL;
        }
        return moving ? SWIM_CRUISE : SWIM_IDLE;
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
}
