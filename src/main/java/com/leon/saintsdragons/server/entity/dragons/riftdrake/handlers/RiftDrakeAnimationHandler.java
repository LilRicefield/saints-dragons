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
    private static final RawAnimation SWIM_MOVE = RawAnimation.begin().thenLoop("animation.rift_drake.swim_move");

    private final RiftDrakeEntity drake;

    public RiftDrakeAnimationHandler(RiftDrakeEntity drake) {
        this.drake = drake;
    }

    public PlayState movementPredicate(AnimationState<RiftDrakeEntity> state) {
        boolean isSwimming = drake.isSwimming();
        boolean isInWaterColumn = drake.isInWaterOrBubble() || drake.getFluidTypeHeight(net.minecraft.world.level.material.Fluids.WATER.getFluidType()) > 0.1F;
        boolean swimmingContext = isSwimming || isInWaterColumn;
        boolean isNavigating = drake.getNavigation().isInProgress() && drake.getNavigation().getPath() != null;
        double horizontalSpeedSq = drake.getDeltaMovement().horizontalDistanceSqr();
        boolean isMovingLand = state.isMoving() || horizontalSpeedSq > 0.008D;
        boolean isMovingWater = horizontalSpeedSq > 0.004D || isNavigating;

        if (swimmingContext) {
            // Aquatic animations
            state.setAnimation(isMovingWater ? SWIM_MOVE : SWIM_IDLE);
        } else {
            // Ground movement transitions
            if (isMovingLand && drake.isAccelerating()) {
                state.getController().transitionLength(3); // Fast transition into run
                state.setAnimation(RUN);
            } else if (isMovingLand) {
                state.getController().transitionLength(3); // Quick walk engage
                state.setAnimation(WALK);
            } else {
                state.getController().transitionLength(4); // Soft transition to idle
                state.setAnimation(IDLE);
            }
        }
        state.getController().setAnimationSpeed(1.0F);
        return PlayState.CONTINUE;
    }
}
