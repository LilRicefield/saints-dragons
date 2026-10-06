package com.leon.saintsdragons.server.entity.npc;

import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;

final class IvyMovementVisualState {
    private static final double SWIM_FAST_SPEED_SQR = 0.06D * 0.06D;
    private static final double WATER_MOVE_SPEED_SQR = 0.015D * 0.015D;

    IvyMovementVisualState() {
    }

    boolean apply(AnimationState<?> state,
               IvyTheDragonMerchant ivy,
               RawAnimation idle,
               RawAnimation sit,
               RawAnimation walk,
               RawAnimation run,
               RawAnimation falling,
               RawAnimation climbing,
               RawAnimation climbIdle,
               RawAnimation swimIdle,
               RawAnimation swim,
               RawAnimation swimFast,
               RawAnimation waterWadeIdle,
               RawAnimation waterWading) {
        state.getController().transitionLength(4);
        State resolved = resolve(state, ivy);
        switch (resolved) {
            case SWIM_FAST -> AnimationHelper.setAndContinue(state, swimFast);
            case SWIM -> AnimationHelper.setAndContinue(state, swim);
            case SWIM_IDLE -> AnimationHelper.setAndContinue(state, swimIdle);
            case WATER_WADING -> AnimationHelper.setAndContinue(state, waterWading);
            case WATER_WADE_IDLE -> AnimationHelper.setAndContinue(state, waterWadeIdle);
            case FALLING -> AnimationHelper.setAndContinue(state, falling);
            case CLIMBING -> AnimationHelper.setAndContinue(state, climbing);
            case CLIMB_IDLE -> AnimationHelper.setAndContinue(state, climbIdle);
            case SIT -> AnimationHelper.setAndContinue(state, sit);
            case RUN -> AnimationHelper.setAndContinue(state, run);
            case WALK -> AnimationHelper.setAndContinue(state, walk);
            case IDLE -> AnimationHelper.setAndContinue(state, idle);
        }
        return resolved == State.IDLE || resolved == State.WALK || resolved == State.RUN || resolved == State.FALLING;
    }

    private State resolve(AnimationState<?> state, IvyTheDragonMerchant ivy) {
        boolean grounded = ivy.onGround();
        double yVelocity = ivy.getDeltaMovement().y;

        if (ivy.isClimbingLadder()) {
            return Math.abs(yVelocity) > 0.01D ? State.CLIMBING : State.CLIMB_IDLE;
        }

        if (ivy.getCompanionCommand() == IvyTheDragonMerchant.CompanionCommand.STAY
                && grounded
                && !ivy.isInWaterOrBubble()) {
            return State.SIT;
        }

        if (ivy.isInWaterOrBubble()) {
            return waterState(state, ivy);
        }

        if (!grounded && ivy.getFallAnimationState().isFalling()) {
            return State.FALLING;
        }

        return normalGroundState(state, ivy);
    }

    private State waterState(AnimationState<?> state, IvyTheDragonMerchant ivy) {
        Vec3 velocity = ivy.getDeltaMovement();
        double horizontalSpeedSqr = velocity.horizontalDistanceSqr();
        boolean moving = ivy.getAsyncSwimController().isMoving()
                || horizontalSpeedSqr > WATER_MOVE_SPEED_SQR
                || Math.abs(ivy.xxa) > 0.01F
                || Math.abs(ivy.zza) > 0.01F;

        if (ivy.isInShallowWaterForWading()) {
            return moving ? State.WATER_WADING : State.WATER_WADE_IDLE;
        }

        if (!moving) {
            return State.SWIM_IDLE;
        }
        return ivy.isRunning() || horizontalSpeedSqr > SWIM_FAST_SPEED_SQR ? State.SWIM_FAST : State.SWIM;
    }

    private static State normalGroundState(AnimationState<?> state, IvyTheDragonMerchant ivy) {
        if (state.isMoving()) {
            return ivy.isRunning() ? State.RUN : State.WALK;
        }
        return State.IDLE;
    }

    enum State {
        IDLE,
        SIT,
        WALK,
        RUN,
        FALLING,
        CLIMBING,
        CLIMB_IDLE,
        WATER_WADE_IDLE,
        WATER_WADING,
        SWIM_IDLE,
        SWIM,
        SWIM_FAST
    }
}
