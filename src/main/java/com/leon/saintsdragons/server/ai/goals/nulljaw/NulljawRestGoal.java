package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Casual rest goal for Nulljaw. Plays the full down → sit → fall_asleep → sleep → wake_up → sit → up sequence
 * for wild dragons so they don't snap between poses.
 */
public class NulljawRestGoal extends Goal {

    private final Nulljaw drake;
    private int restingTicks;
    private int restDuration;
    private int retryCooldown;

    private enum RestState {
        SITTING_DOWN,
        SITTING,
        FALLING_ASLEEP,
        SLEEPING,
        WAKING_UP,
        SITTING_AFTER,
        STANDING_UP
    }

    private RestState state;
    private int stateTimer;

    public NulljawRestGoal(Nulljaw drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldown > 0) {
            retryCooldown--;
            return false;
        }

        if (drake.isTame()) return false;
        if (drake.isSleepLocked()) return false;
        if (drake.isInWaterOrBubble() || drake.isInLava()) return false;
        if (drake.isDying() || drake.isVehicle()) return false;
        if (drake.getTarget() != null || drake.isAggressive()) return false;
        if (drake.isSwimming()) return false;
        if (drake.getActiveAbility() != null) return false;

        return drake.getRandom().nextFloat() < 0.0005F;
    }

    @Override
    public boolean canContinueToUse() {
        if (state == null) return false;
        boolean safe = !drake.isInWaterOrBubble() && drake.getTarget() == null && !drake.isAggressive();
        boolean sequenceComplete = state == RestState.STANDING_UP && stateTimer > drake.getSitUpAnimationTicks() + 5;
        return safe && !sequenceComplete;
    }

    @Override
    public void start() {
        restDuration = 80 + drake.getRandom().nextInt(81); // 4-8 seconds
        restingTicks = 0;
        stateTimer = 0;

        state = RestState.SITTING_DOWN;
        drake.setOrderedToSit(true);
        drake.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (drake.level().isClientSide) return;

        stateTimer++;
        drake.getNavigation().stop();
        drake.setDeltaMovement(0, drake.getDeltaMovement().y, 0);

        if (state == RestState.SITTING_DOWN || state == RestState.SITTING) {
            if (!drake.isOrderedToSit()) {
                drake.setOrderedToSit(true);
            }
        }

        switch (state) {
            case SITTING_DOWN -> {
                if (stateTimer > drake.getSitDownAnimationTicks() + 5 && !drake.isInSitTransition()) {
                    state = RestState.SITTING;
                    stateTimer = 0;
                }
            }
            case SITTING -> {
                if (stateTimer > 20) {
                    state = RestState.FALLING_ASLEEP;
                    stateTimer = 0;
                    drake.startSleepEnter();
                }
            }
            case FALLING_ASLEEP -> {
                if ((drake.isSleeping() && !drake.isSleepTransitioning())
                        || stateTimer > drake.getFallAsleepAnimationTicks() + 5) {
                    state = RestState.SLEEPING;
                    stateTimer = 0;
                    restingTicks = 0;
                }
            }
            case SLEEPING -> {
                restingTicks++;
                if (restingTicks >= restDuration) {
                    state = RestState.WAKING_UP;
                    stateTimer = 0;
                    drake.startSleepExit();
                    drake.setOrderedToSit(true);
                }
            }
            case WAKING_UP -> {
                if (stateTimer > drake.getWakeUpAnimationTicks() + 5) {
                    state = RestState.SITTING_AFTER;
                    stateTimer = 0;
                    drake.setOrderedToSit(true);
                }
            }
            case SITTING_AFTER -> {
                if (stateTimer > 20) {
                    state = RestState.STANDING_UP;
                    stateTimer = 0;
                    drake.setOrderedToSit(false);
                }
            }
            case STANDING_UP -> {
                // Wait for the stand-up animation to complete; goal ends via canContinueToUse
            }
        }
    }

    @Override
    public void stop() {
        if (state != RestState.STANDING_UP) {
            if (drake.isSleeping() || drake.isSleepTransitioning()) {
                drake.startSleepExit();
            }
            drake.setOrderedToSit(false);
        }

        retryCooldown = 200 + drake.getRandom().nextInt(201);
        restingTicks = 0;
        restDuration = 0;
        stateTimer = 0;
        state = null;
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }
}
