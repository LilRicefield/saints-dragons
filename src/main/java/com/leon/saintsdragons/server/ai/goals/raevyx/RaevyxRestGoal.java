package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.sleep.DragonRestState;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Casual rest goal for Raevyx - makes wild dragons sleep with full animation cycle.
 * Animation sequence: down → sit → fall_asleep → sleep → wake_up → sit → up → idle
 * Now uses persistent DragonRestManager so state survives save/load.
 */
public class RaevyxRestGoal extends Goal {

    private final Raevyx wyvern;
    private int retryCooldown;

    public RaevyxRestGoal(Raevyx wyvern) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldown > 0) {
            retryCooldown--;
            return false;
        }

        // If already in a rest cycle (e.g., loaded from save), continue it
        if (wyvern.getRestManager().isResting()) {
            return true;
        }

        if (wyvern.isTame()) return false;
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
        // Continue until rest manager completes the cycle
        boolean safeToRest = !wyvern.isInWaterOrBubble() && wyvern.getTarget() == null && !wyvern.isAggressive();
        return wyvern.getRestManager().isResting() && safeToRest;
    }

    @Override
    public void start() {
        var restManager = wyvern.getRestManager();

        // If already resting (loaded from save), don't restart
        if (restManager.isResting()) {
            // Resume from saved state
            return;
        }

        // Start new rest cycle with random sleep duration (5-10 seconds)
        int sleepDuration = 100 + wyvern.getRandom().nextInt(101);
        restManager.startRest(sleepDuration);

        wyvern.setOrderedToSit(true); // Triggers down animation
        wyvern.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (wyvern.level().isClientSide) return;

        var restManager = wyvern.getRestManager();
        DragonRestState state = restManager.getCurrentState();

        // Stop navigation and stay still
        wyvern.getNavigation().stop();
        wyvern.setDeltaMovement(0, wyvern.getDeltaMovement().y, 0);

        // Ensure dragon stays sitting ONLY during initial sit-down and sitting states
        if (state == DragonRestState.SITTING_DOWN || state == DragonRestState.SITTING) {
            if (!wyvern.isOrderedToSit()) {
                wyvern.setOrderedToSit(true);
            }
        }

        // State machine for animation sequence
        switch (state) {
            case SITTING_DOWN:
                // Wait for down → sit animation (1.5s = 30 ticks, +5 buffer)
                if (restManager.getStateTimer() > 35 && !wyvern.isInSitTransition()) {
                    restManager.advanceState();
                }
                break;

            case SITTING:
                // Pause in sit idle before falling asleep (1 second)
                if (restManager.getStateTimer() > 20) {
                    restManager.advanceState();
                    wyvern.startSleepEnter(); // Triggers fall_asleep animation
                }
                break;

            case FALLING_ASLEEP:
                // Wait for fall_asleep animation (2.5s = 50 ticks, +5 buffer)
                if ((wyvern.isSleeping() && !wyvern.isSleepTransitioning()) || restManager.getStateTimer() > 55) {
                    restManager.advanceState();
                }
                break;

            case SLEEPING:
                // Sleep for the duration, then wake up
                restManager.incrementRestingTicks();
                if (restManager.getRestingTicks() >= restManager.getRestDuration()) {
                    restManager.advanceState();
                    wyvern.startSleepExit(); // Triggers wake_up animation
                    wyvern.setOrderedToSit(true);
                }
                break;

            case WAKING_UP:
                // Wait for wake_up animation (2.625s = 53 ticks, +5 buffer)
                if (restManager.getStateTimer() > 58) {
                    restManager.advanceState();
                    wyvern.setOrderedToSit(true);
                }
                break;

            case SITTING_AFTER:
                // Pause in sit after waking (stretching moment, 1 second)
                if (restManager.getStateTimer() > 20) {
                    restManager.advanceState();
                    wyvern.setOrderedToSit(false); // Trigger stand up animation
                }
                break;

            case STANDING_UP:
                // Wait for up animation (1.0s = 20 ticks)
                if (restManager.getStateTimer() > 20) {
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
        var restManager = wyvern.getRestManager();

        // Emergency cleanup - force stand up if interrupted mid-cycle
        if (restManager.isResting() && restManager.getCurrentState() != DragonRestState.STANDING_UP) {
            if (wyvern.isSleeping() || wyvern.isSleepTransitioning()) {
                wyvern.startSleepExit();
            }
            wyvern.setOrderedToSit(false);
            restManager.stopRest(); // Clear rest state
        }

        // Set cooldown before next rest (200-400 ticks = 10-20 seconds)
        retryCooldown = 200 + wyvern.getRandom().nextInt(201);
    }

    @Override
    public boolean isInterruptable() {
        return true; // Can be interrupted by threats
    }
}
