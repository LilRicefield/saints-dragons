package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.sleep.DragonRestState;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Casual rest goal for Nulljaw - makes wild dragons sleep with full animation cycle.
 * Animation sequence: down → sit → fall_asleep → sleep → wake_up → sit → up → idle
 * Now uses persistent DragonRestManager so state survives save/load.
 */
public class NulljawRestGoal extends Goal {

    private final Nulljaw drake;
    private int retryCooldown;

    // Normal timing constants
    private static final int SIT_PAUSE = 20;          // 1s pause before falling asleep
    private static final int SLEEP_MIN = 80;          // 4s minimum sleep duration
    private static final int SLEEP_MAX = 160;         // 8s maximum sleep duration
    private static final int WAKE_PAUSE = 20;         // 1s pause after waking
    private static final int RETRY_COOLDOWN_MIN = 200; // 10s minimum cooldown
    private static final int RETRY_COOLDOWN_MAX = 400; // 20s maximum cooldown

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

        // If already in a rest cycle (e.g., loaded from save), continue it
        if (drake.getRestManager().isResting()) {
            return true;
        }

        if (drake.isTame()) return false;
        if (drake.isSleepLocked()) return false;
        if (drake.isInWaterOrBubble() || drake.isInLava()) return false;
        if (drake.isDying() || drake.isVehicle()) return false;
        if (drake.getTarget() != null || drake.isAggressive()) return false;
        if (drake.isSwimming()) return false;
        if (drake.getActiveAbility() != null) return false;

        // Random chance to rest (about 0.05% chance per second when idle)
        return drake.getRandom().nextFloat() < 0.0005F;
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
            // Resume from saved state
            return;
        }

        // Start new rest cycle with normal sleep duration (4-8 seconds)
        int sleepDuration = SLEEP_MIN + drake.getRandom().nextInt(SLEEP_MAX - SLEEP_MIN);
        restManager.startRest(sleepDuration);

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

        // Ensure dragon stays sitting ONLY during initial sit-down and sitting states
        if (state == DragonRestState.SITTING_DOWN || state == DragonRestState.SITTING) {
            if (!drake.isOrderedToSit()) {
                drake.setOrderedToSit(true);
            }
        }

        // State machine for animation sequence
        switch (state) {
            case SITTING_DOWN:
                // Wait for down → sit animation (33 ticks + small buffer)
                if (restManager.getStateTimer() > 38 && !drake.isInSitTransition()) {
                    restManager.advanceState();
                }
                break;

            case SITTING:
                // Pause in sit before falling asleep (1 second)
                if (restManager.getStateTimer() > SIT_PAUSE) {
                    restManager.advanceState();
                    drake.startSleepEnter(); // Triggers fall_asleep animation
                }
                break;

            case FALLING_ASLEEP:
                // Wait for fall_asleep animation (2.5s = 50 ticks + buffer)
                if ((drake.isSleeping() && !drake.isSleepTransitioning()) || restManager.getStateTimer() > 55) {
                    restManager.advanceState();
                }
                break;

            case SLEEPING:
                // Sleep for the duration, then wake up
                restManager.incrementRestingTicks();
                if (restManager.getRestingTicks() >= restManager.getRestDuration()) {
                    restManager.advanceState();
                    drake.startSleepExit(); // Triggers wake_up animation
                    drake.setOrderedToSit(true);
                }
                break;

            case WAKING_UP:
                // Wait for wake_up animation (40 ticks + buffer)
                if (restManager.getStateTimer() > 45) {
                    restManager.advanceState();
                    drake.setOrderedToSit(true);
                }
                break;

            case SITTING_AFTER:
                // Pause after waking (1 second)
                if (restManager.getStateTimer() > WAKE_PAUSE) {
                    restManager.advanceState();
                    drake.setOrderedToSit(false); // Trigger stand up animation
                }
                break;

            case STANDING_UP:
                // Wait for up animation to complete
                if (restManager.getStateTimer() > 45) {
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

        // Cooldown before next rest (10-20 seconds)
        retryCooldown = RETRY_COOLDOWN_MIN + drake.getRandom().nextInt(RETRY_COOLDOWN_MAX - RETRY_COOLDOWN_MIN);
    }

    @Override
    public boolean isInterruptable() {
        return true; // Can be interrupted by threats
    }
}
