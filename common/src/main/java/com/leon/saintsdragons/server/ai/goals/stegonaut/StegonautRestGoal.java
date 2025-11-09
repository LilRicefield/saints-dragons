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
            if (isDay) {
                System.out.println("[DEBUG] StegonautRestGoal.canUse() = TRUE (already in rest cycle during day: " + drake.getRestManager().getCurrentState() + ")");
                return true;
            } else {
                System.out.println("[DEBUG] StegonautRestGoal.canUse() = FALSE (rest cycle active but it's night - not our rest cycle!)");
                return false;
            }
        }

        if (drake.isTame()) return false;
        if (drake.isOrderedToSit()) return false;
        if (drake.isInWaterOrBubble()) return false;
        if (drake.isDying() || drake.isVehicle()) return false;
        if (drake.getTarget() != null || drake.isAggressive()) return false;

        // Only start new rest cycle during day
        if (!isDay) return false;

        // Random chance to rest (about 0.5% chance per tick when conditions are met)
        boolean randomRest = drake.getRandom().nextFloat() < 0.005f;
        if (randomRest) {
            System.out.println("[DEBUG] StegonautRestGoal.canUse() = TRUE (random rest)");
        }
        return randomRest;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue until rest manager completes the cycle
        boolean safeToRest = !drake.isInWaterOrBubble() && drake.getTarget() == null && !drake.isAggressive();
        boolean shouldContinue = drake.getRestManager().isResting() && safeToRest;

        // DEBUG: Log when we stop continuing
        if (!shouldContinue && drake.getRestManager().isResting()) {
            System.out.println("[DEBUG] StegonautRestGoal.canContinueToUse() = FALSE" +
                " | safeToRest: " + safeToRest +
                " | restState: " + drake.getRestManager().getCurrentState());
        }

        return shouldContinue;
    }

    @Override
    public void start() {
        var restManager = drake.getRestManager();

        // If already resting (loaded from save), don't restart
        if (restManager.isResting()) {
            // Resume from saved state
            System.out.println("[DEBUG] StegonautRestGoal.start() - Resuming from saved state: " + restManager.getCurrentState());
            return;
        }

        // Start new rest cycle with random rest duration (3-6 seconds = 60-120 ticks)
        int restDuration = 60 + drake.getRandom().nextInt(61);
        restManager.startRest(restDuration);
        System.out.println("[DEBUG] StegonautRestGoal.start() - Starting rest cycle (duration: " + restDuration + " ticks)");

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
                    System.out.println("[DEBUG] RestGoal advancing: SITTING_DOWN → SITTING");
                    restManager.advanceState();
                }
                break;

            case SITTING:
                // Rest for the duration (sitting idle)
                restManager.incrementRestingTicks();
                if (restManager.getRestingTicks() >= restManager.getRestDuration()) {
                    // Skip all sleep states at once: FALLING_ASLEEP → SLEEPING → WAKING_UP
                    // This way the entity never sees these states and won't trigger sleep animations
                    System.out.println("[DEBUG] RestGoal: SITTING rest complete, skipping sleep states to SITTING_AFTER");
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
                System.out.println("[DEBUG] RestGoal WARNING: In unexpected sleep state " + state + ", advancing");
                restManager.advanceState();
                break;

            case SITTING_AFTER:
                // Brief pause after resting (0.5 seconds)
                if (restManager.getStateTimer() > 10) {
                    System.out.println("[DEBUG] RestGoal advancing: SITTING_AFTER → STANDING_UP (setting isOrderedToSit = false)");
                    restManager.advanceState();
                    drake.setOrderedToSit(false); // Trigger stand up animation
                }
                break;

            case STANDING_UP:
                // Wait for up animation (38 ticks + 2 tick buffer)
                if (restManager.getStateTimer() > 40) {
                    System.out.println("[DEBUG] RestGoal advancing: STANDING_UP → IDLE (rest cycle complete)");
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

        System.out.println("[DEBUG] StegonautRestGoal.stop() called - RestState: " + restManager.getCurrentState() + " | isOrderedToSit: " + drake.isOrderedToSit());

        // Emergency cleanup - force stand up if interrupted mid-cycle
        if (restManager.isResting() && restManager.getCurrentState() != DragonRestState.STANDING_UP) {
            System.out.println("[DEBUG] RestGoal interrupted mid-cycle! Force-stopping rest.");
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
