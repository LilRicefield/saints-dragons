package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Casual rest goal for Raevyx - makes wild dragons sleep with full animation cycle.
 * Animation sequence: down → sit → fall_asleep → sleep → wake_up → sit → up → idle
 * Prevents T-pose by properly transitioning through sitting before sleeping.
 */
public class RaevyxRestGoal extends Goal {

    private final Raevyx wyvern;
    private int restingTicks;
    private int restDuration;
    private int retryCooldown;

    // State tracking for animation sequence
    private enum RestState {
        SITTING_DOWN,    // Playing down → sit animation
        SITTING,         // In sit idle, waiting to sleep
        FALLING_ASLEEP,  // Transitioning fall_asleep → sleep
        SLEEPING,        // In sleep loop
        WAKING_UP,       // Transitioning wake_up → sit
        SITTING_AFTER,   // Back in sit after waking
        STANDING_UP      // Playing up → idle animation
    }
    private RestState state;
    private int stateTimer;

    public RaevyxRestGoal(Raevyx wyvern) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        // Cooldown between rest attempts
        if (retryCooldown > 0) {
            retryCooldown--;
            return false;
        }

        // Only wild (untamed) dragons rest casually
        if (wyvern.isTame()) return false;

        // Don't rest if busy with other things
        if (wyvern.isOrderedToSit()) return false;
        if (wyvern.isSleepLocked()) return false;
        if (wyvern.isInWaterOrBubble() || wyvern.isInLava()) return false;
        if (wyvern.isDying() || wyvern.isVehicle()) return false;
        if (wyvern.getTarget() != null || wyvern.isAggressive()) return false;
        if (wyvern.isFlying()) return false;

        // Random chance to rest (about 1% chance per second when idle)
        return wyvern.getRandom().nextFloat() < 0.0005f;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue until we've completed the full sequence
        boolean safeToRest = !wyvern.isInWaterOrBubble() && wyvern.getTarget() == null;
        boolean sequenceComplete = (state == RestState.STANDING_UP && stateTimer > 20); // After stand up animation
        return !sequenceComplete && safeToRest;
    }

    @Override
    public void start() {
        // Random rest duration: 100-200 ticks (5-10 seconds) - for the SLEEPING phase
        restDuration = 100 + wyvern.getRandom().nextInt(101);
        restingTicks = 0;
        stateTimer = 0;

        // Start with sitting down animation (down → sit)
        state = RestState.SITTING_DOWN;
        wyvern.setOrderedToSit(true); // Triggers down animation
        wyvern.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (wyvern.level().isClientSide) return;

        stateTimer++;

        // Stop navigation and stay still
        wyvern.getNavigation().stop();
        wyvern.setDeltaMovement(0, wyvern.getDeltaMovement().y, 0);

        // Ensure dragon stays sitting ONLY during initial sit-down and sitting states
        // DO NOT re-trigger sit during WAKING_UP or SITTING_AFTER (wake_up already transitions to sit)
        if (state == RestState.SITTING_DOWN || state == RestState.SITTING) {
            if (!wyvern.isOrderedToSit()) {
                wyvern.setOrderedToSit(true); // Re-enforce sit if it got cleared somehow
            }
        }

        // State machine for animation sequence
        switch (state) {
            case SITTING_DOWN:
                // Wait for down → sit animation (1.5s = 30 ticks, +5 buffer)
                if (stateTimer > 35 && !wyvern.isInSitTransition()) {
                    state = RestState.SITTING;
                    stateTimer = 0;
                }
                break;

            case SITTING:
                // Pause in sit idle before falling asleep (1 second)
                if (stateTimer > 20) {
                    state = RestState.FALLING_ASLEEP;
                    stateTimer = 0;
                    wyvern.startSleepEnter(); // Triggers fall_asleep → sleep
                }
                break;

            case FALLING_ASLEEP:
                // Wait for fall_asleep animation (2.5s = 50 ticks, +5 buffer)
                // Use state check as primary, timer as fallback
                if ((wyvern.isSleeping() && !wyvern.isSleepTransitioning()) || stateTimer > 55) {
                    state = RestState.SLEEPING;
                    stateTimer = 0;
                    restingTicks = 0; // Start counting sleep duration
                }
                break;

            case SLEEPING:
                // Sleep for the duration, then wake up
                restingTicks++;
                if (restingTicks >= restDuration) {
                    state = RestState.WAKING_UP;
                    stateTimer = 0;
                    wyvern.startSleepExit(); // Triggers wake_up → sit (with yawn!)
                    // Keep sit flag true so wake_up can play (don't let dragon stand yet!)
                    wyvern.setOrderedToSit(true);
                }
                break;

            case WAKING_UP:
                // Wait for wake_up animation (2.625s = 53 ticks, +5 buffer for safety)
                // ONLY use timer - state checks fire too early!
                if (stateTimer > 58) {
                    state = RestState.SITTING_AFTER;
                    stateTimer = 0;
                    // Ensure still sitting after wake_up completes (wake_up ends in sit pose)
                    wyvern.setOrderedToSit(true);
                }
                break;

            case SITTING_AFTER:
                // Pause in sit after waking (stretching moment, 1 second)
                if (stateTimer > 20) {
                    state = RestState.STANDING_UP;
                    stateTimer = 0;
                    wyvern.setOrderedToSit(false); // NOW trigger up → idle animation
                }
                break;

            case STANDING_UP:
                // Wait for up animation (1.0s = 20 ticks, goal ends via canContinueToUse)
                // Goal will end via canContinueToUse() after 20 tick timer
                break;
        }
    }

    @Override
    public void stop() {
        // Emergency cleanup - force stand up if interrupted
        if (state != RestState.STANDING_UP) {
            if (wyvern.isSleeping() || wyvern.isSleepTransitioning()) {
                wyvern.startSleepExit();
            }
            wyvern.setOrderedToSit(false);
        }

        // Set cooldown before next rest (200-400 ticks = 10-20 seconds)
        retryCooldown = 200 + wyvern.getRandom().nextInt(201);

        restingTicks = 0;
        restDuration = 0;
        stateTimer = 0;
        state = null;
    }

    @Override
    public boolean isInterruptable() {
        return true; // Can be interrupted by threats
    }
}
