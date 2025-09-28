package com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers;

import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class RiftDrakeAnimationHandler {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rift_drake.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.rift_drake.walk");
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.rift_drake.swim_idle");
    private static final RawAnimation SWIM_MOVE = RawAnimation.begin().thenLoop("animation.rift_drake.swim_move");

    private final RiftDrakeEntity drake;

    public RiftDrakeAnimationHandler(RiftDrakeEntity drake) {
        this.drake = drake;
    }

    public PlayState movementPredicate(AnimationState<RiftDrakeEntity> state) {
        if (drake.isSwimming()) {
            state.setAnimation(state.isMoving() ? SWIM_MOVE : SWIM_IDLE);
        } else {
            state.setAnimation(state.isMoving() ? WALK : IDLE);
        }
        return PlayState.CONTINUE;
    }
}
