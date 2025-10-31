package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.sleep.DragonRestState;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Night rest/sleep goal for Cindervane - makes wild dragons sleep at night with full animation cycle.
 * Animation sequence: down → sit → fall_asleep → sleep → wake_up → sit → up → idle
 * Now uses persistent DragonRestManager so state survives save/load.
 * Unlike Raevyx, Cindervane sleeps through the entire night until dawn (like Stegonaut from Dawn Era).
 */
public class CindervaneRestGoal extends Goal {

    private final Cindervane amphithere;
    private int retryCooldown;

    public CindervaneRestGoal(Cindervane amphithere) {
        this.amphithere = amphithere;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldown > 0) {
            retryCooldown--;
            return false;
        }

        // If already in a rest cycle (e.g., loaded from save), continue it
        if (amphithere.getRestManager().isResting()) {
            return true;
        }

        if (amphithere.isTame()) return false;
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
        // Continue until rest manager completes the cycle
        boolean safeToRest = !amphithere.isInWaterOrBubble() && amphithere.getTarget() == null && !amphithere.isAggressive();

        // Also wake up if it becomes day
        long dayTime = amphithere.level().getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime < 23000;

        return amphithere.getRestManager().isResting() && safeToRest && isNight;
    }

    @Override
    public void start() {
        var restManager = amphithere.getRestManager();

        // If already resting (loaded from save), don't restart
        if (restManager.isResting()) {
            // Resume from saved state
            return;
        }

        // Start new rest cycle - sleep until dawn (set duration to max int, will be interrupted by day)
        restManager.startRest(Integer.MAX_VALUE);

        amphithere.setOrderedToSit(true); // Triggers down animation
        amphithere.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (amphithere.level().isClientSide) return;

        var restManager = amphithere.getRestManager();
        DragonRestState state = restManager.getCurrentState();

        // Stop navigation and stay still
        amphithere.getNavigation().stop();
        amphithere.setDeltaMovement(0, amphithere.getDeltaMovement().y, 0);

        // Ensure dragon stays sitting ONLY during initial sit-down and sitting states
        if (state == DragonRestState.SITTING_DOWN || state == DragonRestState.SITTING) {
            if (!amphithere.isOrderedToSit()) {
                amphithere.setOrderedToSit(true);
            }
        }

        // Check if it's time to wake up (became day)
        long dayTime = amphithere.level().getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime < 23000;

        // State machine for animation sequence
        switch (state) {
            case SITTING_DOWN:
                // Wait for down → sit animation (2.25s = 45 ticks, +5 buffer)
                if (restManager.getStateTimer() > 50 && !amphithere.isInSitTransition()) {
                    restManager.advanceState();
                }
                break;

            case SITTING:
                // Pause in sit idle before falling asleep (1 second)
                if (restManager.getStateTimer() > 20) {
                    restManager.advanceState();
                    amphithere.startSleepEnter(); // Triggers fall_asleep animation
                }
                break;

            case FALLING_ASLEEP:
                // Wait for fall_asleep animation (3.0s = 60 ticks, +5 buffer)
                if ((amphithere.isSleeping() && !amphithere.isSleepTransitioning()) || restManager.getStateTimer() > 65) {
                    restManager.advanceState();
                }
                break;

            case SLEEPING:
                // Sleep until dawn (or until interrupted by threats/damage)
                if (!isNight) {
                    restManager.advanceState();
                    amphithere.startSleepExit(); // Triggers wake_up animation
                    amphithere.setOrderedToSit(true);
                }
                break;

            case WAKING_UP:
                // Wait for wake_up animation (2.0833s = 42 ticks, +5 buffer)
                if (restManager.getStateTimer() > 47) {
                    restManager.advanceState();
                    amphithere.setOrderedToSit(true);
                }
                break;

            case SITTING_AFTER:
                // Pause in sit after waking (stretching moment, 1 second)
                if (restManager.getStateTimer() > 20) {
                    restManager.advanceState();
                    amphithere.setOrderedToSit(false); // Trigger stand up animation
                }
                break;

            case STANDING_UP:
                // Wait for up animation (2.2917s = 46 ticks)
                if (restManager.getStateTimer() > 46) {
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
        var restManager = amphithere.getRestManager();

        // Emergency cleanup - force stand up if interrupted mid-cycle
        if (restManager.isResting() && restManager.getCurrentState() != DragonRestState.STANDING_UP) {
            if (amphithere.isSleeping() || amphithere.isSleepTransitioning()) {
                amphithere.startSleepExit();
            }
            amphithere.setOrderedToSit(false);
            restManager.stopRest(); // Clear rest state
        }

        // Set cooldown before next rest (400-800 ticks = 20-40 seconds)
        retryCooldown = 400 + amphithere.getRandom().nextInt(401);
    }

    @Override
    public boolean isInterruptable() {
        return true; // Can be interrupted by threats
    }
}
