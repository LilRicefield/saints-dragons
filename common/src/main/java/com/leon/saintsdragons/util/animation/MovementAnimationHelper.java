package com.leon.saintsdragons.util.animation;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public final class MovementAnimationHelper {
    private MovementAnimationHelper() {
    }


    public static void setAndContinue(AnimationState<?> state, RawAnimation animation) {
        state.setAnimation(animation);
        if (animation != null
                && state.getController().getCurrentAnimation() == null
                && state.isCurrentAnimation(animation)) {
            state.resetCurrentAnimation();
            state.setAnimation(animation);
        }
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
            setAndContinue(state, sleepAnimation);
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
            setAndContinue(state, sitAnimation);
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
        if (dragon.isVehicle()) {
            if (groundState == 2 || dragon.isRunning()) {
                setAndContinue(state, runAnimation);
            } else if (groundState == 1 || dragon.isWalking() || (treatAnimationStateMovingAsWalk && state.isMoving())) {
                setAndContinue(state, walkAnimation);
            } else {
                setAndContinue(state, idleAnimation);
            }
            return PlayState.CONTINUE;
        }

        if (groundState == 2 || (state.isMoving() && dragon.shouldUseRunAnimation())) {
            setAndContinue(state, runAnimation);
        } else if (groundState == 1 || state.isMoving()) {
            setAndContinue(state, walkAnimation);
        } else {
            setAndContinue(state, idleAnimation);
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
        if (dragon.isVehicle()) {
            if (groundState == 2 || dragon.isRunning()) {
                state.getController().transitionLength(movingTransitionTicks);
                setAndContinue(state, runAnimation);
            } else if (groundState == 1 || dragon.isWalking()) {
                state.getController().transitionLength(movingTransitionTicks);
                setAndContinue(state, walkAnimation);
            } else {
                state.getController().transitionLength(idleTransitionTicks);
                setAndContinue(state, idleAnimation);
            }
            return PlayState.CONTINUE;
        }

        if (groundState == 2 || (state.isMoving() && dragon.shouldUseRunAnimation())) {
            state.getController().transitionLength(movingTransitionTicks);
            setAndContinue(state, runAnimation);
        } else if (groundState == 1 || state.isMoving()) {
            state.getController().transitionLength(movingTransitionTicks);
            setAndContinue(state, walkAnimation);
        } else {
            state.getController().transitionLength(idleTransitionTicks);
            setAndContinue(state, idleAnimation);
        }
        return PlayState.CONTINUE;
    }
}
