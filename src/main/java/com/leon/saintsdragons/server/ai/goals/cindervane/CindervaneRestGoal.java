package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Night rest/sleep goal for Cindervane - makes wild dragons sleep at night with full animation cycle.
 * Animation sequence: down → sit → fall_asleep → sleep → wake_up → sit → up → idle
 * Prevents T-pose by properly transitioning through sitting before sleeping.
 * Unlike Raevyx, Cindervane sleeps through the entire night until dawn (like Stegonaut from Dawn Era).
 */
public class CindervaneRestGoal extends Goal {

    private final Cindervane amphithere;
    private int retryCooldown;

    // State tracking for animation sequence
    private enum RestState {
        SITTING_DOWN,    // Playing down → sit animation
        SITTING,         // In sit idle, waiting to sleep
        FALLING_ASLEEP,  // Transitioning fall_asleep → sleep
        SLEEPING,        // In sleep loop - sleeps until dawn
        WAKING_UP,       // Transitioning wake_up → sit
        SITTING_AFTER,   // Back in sit after waking
        STANDING_UP      // Playing up → idle animation
    }
    private RestState state;
    private int stateTimer;

    public CindervaneRestGoal(Cindervane amphithere) {
        this.amphithere = amphithere;
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
        if (amphithere.isTame()) return false;

        // Don't rest if busy with other things
        if (amphithere.isOrderedToSit()) return false;
        if (amphithere.isSleepLocked()) return false;
        if (amphithere.isInWaterOrBubble() || amphithere.isInLava()) return false;
        if (amphithere.isDying() || amphithere.isVehicle()) return false;
        if (amphithere.getTarget() != null || amphithere.isAggressive()) return false;
        if (amphithere.isFlying()) return false;

        // NIGHT-ONLY: Cindervanes sleep at night (like Stegonaut)
        long dayTime = amphithere.level().getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime < 23000;
        if (!isNight) return false;

        // Random chance to sleep at night (about 1% chance per second when idle)
        return amphithere.getRandom().nextFloat() < 0.0005f;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue until we've completed the full sequence
        boolean safeToRest = !amphithere.isInWaterOrBubble() && amphithere.getTarget() == null;
        boolean sequenceComplete = (state == RestState.STANDING_UP && stateTimer > 46); // After stand up animation

        // Also wake up if it becomes day
        long dayTime = amphithere.level().getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime < 23000;

        return !sequenceComplete && safeToRest && isNight;
    }

    @Override
    public void start() {
        stateTimer = 0;

        // Start with sitting down animation (down → sit)
        state = RestState.SITTING_DOWN;
        amphithere.setOrderedToSit(true); // Triggers down animation
        amphithere.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (amphithere.level().isClientSide) return;

        stateTimer++;

        // Stop navigation and stay still
        amphithere.getNavigation().stop();
        amphithere.setDeltaMovement(0, amphithere.getDeltaMovement().y, 0);

        // Ensure dragon stays sitting ONLY during initial sit-down and sitting states
        // DO NOT re-trigger sit during WAKING_UP or SITTING_AFTER (wake_up already transitions to sit)
        if (state == RestState.SITTING_DOWN || state == RestState.SITTING) {
            if (!amphithere.isOrderedToSit()) {
                amphithere.setOrderedToSit(true); // Re-enforce sit if it got cleared somehow
            }
        }

        // State machine for animation sequence
        switch (state) {
            case SITTING_DOWN:
                // Wait for down → sit animation (2.25s = 45 ticks, +5 buffer)
                if (stateTimer > 50 && !amphithere.isInSitTransition()) {
                    state = RestState.SITTING;
                    stateTimer = 0;
                }
                break;

            case SITTING:
                // Pause in sit idle before falling asleep (1 second)
                if (stateTimer > 20) {
                    state = RestState.FALLING_ASLEEP;
                    stateTimer = 0;
                    amphithere.startSleepEnter(); // Triggers fall_asleep → sleep
                }
                break;

            case FALLING_ASLEEP:
                // Wait for fall_asleep animation (3.0s = 60 ticks, +5 buffer)
                // Use state check as primary, timer as fallback
                if ((amphithere.isSleeping() && !amphithere.isSleepTransitioning()) || stateTimer > 65) {
                    state = RestState.SLEEPING;
                    stateTimer = 0;
                }
                break;

            case SLEEPING:
                // Sleep until dawn (or until interrupted by threats/damage)
                long dayTime = amphithere.level().getDayTime() % 24000;
                boolean isNight = dayTime >= 13000 && dayTime < 23000;

                // Wake up when it becomes day
                if (!isNight) {
                    state = RestState.WAKING_UP;
                    stateTimer = 0;
                    amphithere.startSleepExit(); // Triggers wake_up → sit (with yawn!)
                    // Keep sit flag true so wake_up can play (don't let dragon stand yet!)
                    amphithere.setOrderedToSit(true);
                }
                break;

            case WAKING_UP:
                // Wait for wake_up animation (2.0833s = 42 ticks, +5 buffer for safety)
                // ONLY use timer - state checks fire too early!
                if (stateTimer > 47) {
                    state = RestState.SITTING_AFTER;
                    stateTimer = 0;
                    // Ensure still sitting after wake_up completes (wake_up ends in sit pose)
                    amphithere.setOrderedToSit(true);
                }
                break;

            case SITTING_AFTER:
                // Pause in sit after waking (stretching moment, 1 second)
                if (stateTimer > 20) {
                    state = RestState.STANDING_UP;
                    stateTimer = 0;
                    amphithere.setOrderedToSit(false); // NOW trigger up → idle animation
                }
                break;

            case STANDING_UP:
                // Wait for up animation (2.2917s = 46 ticks, goal ends via canContinueToUse)
                // Goal will end via canContinueToUse() after 46 tick timer
                break;
        }
    }

    @Override
    public void stop() {
        // Emergency cleanup - force stand up if interrupted
        if (state != RestState.STANDING_UP) {
            if (amphithere.isSleeping() || amphithere.isSleepTransitioning()) {
                amphithere.startSleepExit();
            }
            amphithere.setOrderedToSit(false);
        }

        // Set cooldown before next rest (400-800 ticks = 20-40 seconds)
        retryCooldown = 400 + amphithere.getRandom().nextInt(401);

        stateTimer = 0;
        state = null;
    }

    @Override
    public boolean isInterruptable() {
        return true; // Can be interrupted by threats
    }
}
