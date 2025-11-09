package com.leon.saintsdragons.server.ai.goals.stegonaut;

import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.sleep.DragonRestState;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Casual rest goal for Stegonaut - makes wild drakes occasionally sit down to rest.
 * Animation sequence: down → sit → (rest) → up → idle
 * Uses persistent DragonRestManager so state survives save/load.
 */
public class StegonautRestGoal extends Goal {

    private final Stegonaut drake;
    private int retryCooldown;

    public StegonautRestGoal(Stegonaut drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldown > 0) {
            retryCooldown--;
            return false;
        }

        // Check if it's daytime first
        long dayTime = drake.level().getDayTime() % 24000;
        boolean isDay = dayTime >= 0 && dayTime < 13000;

        // If already in a rest cycle (e.g., loaded from save), only continue during day
        // This prevents RestGoal from stealing SleepGoal's nighttime sleep cycles
        if (drake.getRestManager().isResting()) {
            return isDay;
        }

        if (drake.isTame()) return false;
        if (drake.isOrderedToSit()) return false;
        if (drake.isInWaterOrBubble()) return false;
        if (drake.isDying() || drake.isVehicle()) return false;
        if (drake.getTarget() != null || drake.isAggressive()) return false;

        // Only start new rest cycle during day
        if (!isDay) return false;

        // Random chance to rest (about 0.5% chance per tick when conditions are met)
        return drake.getRandom().nextFloat() < 0.005f;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue until rest manager completes the cycle
        boolean safeToRest = !drake.isInWaterOrBubble() && drake.getTarget() == null && !drake.isAggressive();

        return drake.getRestManager().isResting() && safeToRest;
    }

    @Override
    public void start() {
        var restManager = drake.getRestManager();

        // If already resting (loaded from save), don't restart
        if (restManager.isResting()) {
            return;
        }

        // Start new rest cycle with random rest duration (3-6 seconds = 60-120 ticks)
        int restDuration = 60 + drake.getRandom().nextInt(61);
        restManager.startRest(restDuration);

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

        // State machine for rest sequence (simple: down → sit → rest → up)
        // All transition animations are 1.88 seconds = 38 ticks
        switch (state) {
            case SITTING_DOWN:
                // Wait for down → sit animation (38 ticks + 2 tick buffer)
                if (restManager.getStateTimer() > 40) {
                    restManager.advanceState();
                }
                break;

            case SITTING:
                // Rest for the duration (sitting idle)
                restManager.incrementRestingTicks();
                if (restManager.getRestingTicks() >= restManager.getRestDuration()) {
                    // Skip all sleep states at once: FALLING_ASLEEP → SLEEPING → WAKING_UP
                    restManager.advanceState(); // SITTING → FALLING_ASLEEP
                    restManager.advanceState(); // FALLING_ASLEEP → SLEEPING
                    restManager.advanceState(); // SLEEPING → WAKING_UP
                    restManager.advanceState(); // WAKING_UP → SITTING_AFTER
                }
                break;

            case FALLING_ASLEEP:
            case SLEEPING:
            case WAKING_UP:
                // Should never reach these states for casual rest (skipped in SITTING case)
                // But if we do somehow (e.g., interrupted mid-skip), skip them
                restManager.advanceState();
                break;

            case SITTING_AFTER:
                // Brief pause after resting (0.5 seconds)
                if (restManager.getStateTimer() > 10) {
                    restManager.advanceState();
                    drake.setOrderedToSit(false); // Trigger stand up animation
                }
                break;

            case STANDING_UP:
                // Wait for up animation (38 ticks + 2 tick buffer)
                if (restManager.getStateTimer() > 40) {
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
            drake.setOrderedToSit(false);
            restManager.stopRest(); // Clear rest state
        }

        // Set cooldown before next rest (600-900 ticks = 30-45 seconds)
        retryCooldown = 600 + drake.getRandom().nextInt(301);
    }

    @Override
    public boolean isInterruptable() {
        return true; // Can be interrupted by threats
    }
}
