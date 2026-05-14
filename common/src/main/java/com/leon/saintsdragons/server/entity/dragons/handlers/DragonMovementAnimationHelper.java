package com.leon.saintsdragons.server.entity.dragons.handlers;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public final class DragonMovementAnimationHelper {
    private DragonMovementAnimationHelper() {
    }

    public static PlayState tryHandleRestPose(AnimationState<?> state,
                                              DragonEntity dragon,
                                              RawAnimation sleepAnimation,
                                              RawAnimation sitAnimation,
                                              int sleepTransitionTicks,
                                              int sitTransitionTicks) {
        return tryHandleRestPose(state, dragon, sleepAnimation, sitAnimation, sleepTransitionTicks, sitTransitionTicks, true);
    }

    public static PlayState tryHandleRestPose(AnimationState<?> state,
                                              DragonEntity dragon,
                                              RawAnimation sleepAnimation,
                                              RawAnimation sitAnimation,
                                              int sleepTransitionTicks,
                                              int sitTransitionTicks,
                                              boolean allowSitPose) {
        if (dragon.isSleeping() && !dragon.isSleepTransitioning()) {
            if (sleepAnimation == null) {
                return PlayState.STOP;
            }
            state.getController().transitionLength(sleepTransitionTicks);
            state.setAndContinue(sleepAnimation);
            return PlayState.CONTINUE;
        }
        if (dragon.isSleepTransitioning()) {
            return PlayState.STOP;
        }

        float sitProgress = dragon.getSitProgress();
        if (!allowSitPose && sitProgress > 0f) {
            return null;
        }
        if (sitProgress >= dragon.maxSitTicks()) {
            if (sitAnimation == null) {
                return PlayState.STOP;
            }
            state.getController().transitionLength(sitTransitionTicks);
            state.setAndContinue(sitAnimation);
            return PlayState.CONTINUE;
        }
        if (sitProgress > 0f) {
            return PlayState.STOP;
        }

        return null;
    }

    public static PlayState handleGroundMovement(AnimationState<?> state,
                                                 RideableDragonBase dragon,
                                                 RawAnimation idleAnimation,
                                                 RawAnimation walkAnimation,
                                                 RawAnimation runAnimation) {
        return handleGroundMovement(state, dragon, idleAnimation, walkAnimation, runAnimation, false);
    }

    public static PlayState handleGroundMovement(AnimationState<?> state,
                                                 RideableDragonBase dragon,
                                                 RawAnimation idleAnimation,
                                                 RawAnimation walkAnimation,
                                                 RawAnimation runAnimation,
                                                 boolean treatAnimationStateMovingAsWalk) {
        int groundState = dragon.getEffectiveGroundState();
        if (groundState == 2 || dragon.isRunning()) {
            state.setAndContinue(runAnimation);
        } else if (groundState == 1 || dragon.isWalking() || (treatAnimationStateMovingAsWalk && state.isMoving())) {
            state.setAndContinue(walkAnimation);
        } else {
            state.setAndContinue(idleAnimation);
        }
        return PlayState.CONTINUE;
    }

    public static PlayState handleGroundMovement(AnimationState<?> state,
                                                 RideableDragonBase dragon,
                                                 RawAnimation idleAnimation,
                                                 RawAnimation walkAnimation,
                                                 RawAnimation runAnimation,
                                                 int movingTransitionTicks,
                                                 int idleTransitionTicks) {
        int groundState = dragon.getEffectiveGroundState();
        if (groundState == 2 || dragon.isRunning()) {
            state.getController().transitionLength(movingTransitionTicks);
            state.setAndContinue(runAnimation);
        } else if (groundState == 1 || dragon.isWalking()) {
            state.getController().transitionLength(movingTransitionTicks);
            state.setAndContinue(walkAnimation);
        } else {
            state.getController().transitionLength(idleTransitionTicks);
            state.setAndContinue(idleAnimation);
        }
        return PlayState.CONTINUE;
    }
}
