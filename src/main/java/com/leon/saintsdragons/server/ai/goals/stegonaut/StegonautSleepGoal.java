package com.leon.saintsdragons.server.ai.goals.stegonaut;

import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.sleep.DragonRestState;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Primitive Drake specific sleep goal.
 * Now uses persistent DragonRestManager so state survives save/load.
 *
 * Simple sleep behavior:
 * - Sleeps at night, awake during day
 * - Takes short naps during the day (1-2 minutes)
 * - Simple animation cycle: down → sit → sleep → wake up → stand
 * - Same behavior for both wild and tamed drakes
 */
public class StegonautSleepGoal extends Goal {

    private final Stegonaut drake;
    private int retryCooldown;

    public StegonautSleepGoal(Stegonaut drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldown > 0) {
            retryCooldown--;
            return false;
        }

        // If already in a rest cycle (e.g., loaded from save), continue it
        if (drake.getRestManager().isResting()) {
            return true;
        }

        if (drake.isOrderedToSit()) return false;
        if (drake.isDying() || drake.isVehicle()) return false;
        if (drake.getTarget() != null || drake.isAggressive()) return false;

        // Sleep at night
        long dayTime = drake.level().getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime < 23000;
        if (!isNight) return false;

        // Check if daytime nap was queued
        if (drake.consumeDayNapQueued()) {
            return true;
        }

        // Random chance to sleep at night
        return drake.getRandom().nextFloat() < 0.001f;
    }

    @Override
    public boolean canContinueToUse() {
        boolean safeToRest = drake.getTarget() == null && !drake.isAggressive();

        // Wake up if it becomes day
        long dayTime = drake.level().getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime < 23000;

        return drake.getRestManager().isResting() && safeToRest && isNight;
    }

    @Override
    public void start() {
        var restManager = drake.getRestManager();

        // If already resting (loaded from save), don't restart
        if (restManager.isResting()) {
            // Resume from saved state
            return;
        }

        // Check if this is a daytime nap
        if (drake.consumeDayNapQueued()) {
            // Start nap (1-2 minutes)
            int napDuration = 1200 + drake.getRandom().nextInt(1200);
            drake.startNap();
            restManager.startRest(napDuration);
        } else {
            // Start nighttime sleep - sleep until dawn
            restManager.startRest(Integer.MAX_VALUE);
        }

        drake.setOrderedToSit(true); // Triggers down animation
        drake.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (drake.level().isClientSide) return;

        var restManager = drake.getRestManager();
        DragonRestState state = restManager.getCurrentState();

        // Stop navigation and stay still
        drake.getNavigation().stop();
        drake.setDeltaMovement(0, drake.getDeltaMovement().y, 0);

        // Ensure drake stays sitting during relevant states
        if (state == DragonRestState.SITTING_DOWN || state == DragonRestState.SITTING) {
            if (!drake.isOrderedToSit()) {
                drake.setOrderedToSit(true);
            }
        }

        // Check if it's time to wake up (became day)
        long dayTime = drake.level().getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime < 23000;

        // State machine for simple sleep cycle
        switch (state) {
            case SITTING_DOWN:
                // Wait for down → sit animation (simple, ~30 ticks)
                if (restManager.getStateTimer() > 35) {
                    restManager.advanceState();
                }
                break;

            case SITTING:
                // Brief pause before falling asleep
                if (restManager.getStateTimer() > 10) {
                    restManager.advanceState();
                    drake.startSleepEnter(); // Triggers fall_asleep animation
                }
                break;

            case FALLING_ASLEEP:
                // Wait for fall_asleep animation
                if ((drake.isSleeping() && !drake.isSleepTransitioning()) || restManager.getStateTimer() > 50) {
                    restManager.advanceState();
                }
                break;

            case SLEEPING:
                // Sleep until dawn (or until duration expires for naps, or interrupted by threats)
                restManager.incrementRestingTicks();
                boolean shouldWake = !isNight || (restManager.getRestingTicks() >= restManager.getRestDuration());

                if (shouldWake) {
                    restManager.advanceState();
                    drake.startSleepExit(); // Triggers wake_up animation
                    drake.setOrderedToSit(true);
                }
                break;

            case WAKING_UP:
                // Wait for wake_up animation
                if (restManager.getStateTimer() > 45) {
                    restManager.advanceState();
                    drake.setOrderedToSit(true);
                }
                break;

            case SITTING_AFTER:
                // Brief pause after waking
                if (restManager.getStateTimer() > 10) {
                    restManager.advanceState();
                    drake.setOrderedToSit(false); // Trigger stand up animation
                }
                break;

            case STANDING_UP:
                // Wait for up animation
                if (restManager.getStateTimer() > 30) {
                    restManager.advanceState(); // Returns to IDLE
                }
                break;

            default:
                break;
        }

        // Tick the rest manager timer
        restManager.tick();
    }

    @Override
    public void stop() {
        var restManager = drake.getRestManager();

        // Emergency cleanup - force stand up if interrupted mid-cycle
        if (restManager.isResting() && restManager.getCurrentState() != DragonRestState.STANDING_UP) {
            if (drake.isSleeping() || drake.isSleepTransitioning()) {
                drake.startSleepExit();
            }
            drake.setOrderedToSit(false);
            restManager.stopRest(); // Clear rest state
        }

        // Set cooldown before next rest
        retryCooldown = 200 + drake.getRandom().nextInt(201);
    }

    @Override
    public boolean isInterruptable() {
        return true; // Can be interrupted by threats
    }
}
