package com.leon.saintsdragons.server.ai.goals.stegonaut;

import com.leon.saintsdragons.server.ai.goals.base.DragonSleepGoalBase;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;

/**
 * Primitive Drake specific sleep goal.
 *
 * Simple sleep behavior:
 * - Sleeps at night, awake during day
 * - Takes short naps during the day (1-2 minutes)
 * - No complex sleep transitions, just lie down and stand up
 * - Same behavior for both wild and tamed drakes
 */
public class StegonautSleepGoal extends DragonSleepGoalBase {

    public StegonautSleepGoal(Stegonaut dragon) {
        super(dragon);
    }

    @Override
    public boolean canUse() {
        // Use base implementation - it handles the DragonSleepCapable interface properly
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Use base implementation - it handles day/night transitions properly
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        if (dragon instanceof Stegonaut drake) {
            if (drake.consumeDayNapQueued()) {
                drake.startNap();
            }
        }
        super.start();
    }

    @Override
    public void stop() {
        // Use the simple sleep system - no complex transitions
        super.stop();
    }

    @Override
    protected boolean canTamedDragonSleep() {
        // Daytime napping is now handled by StegonautNapGoal - this only handles nighttime sleep
        if (dragon.level().isDay()) {
            return false;
        }
        return super.canTamedDragonSleep();
    }

    @Override
    protected boolean canWildDragonSleep() {
        // Daytime napping is now handled by StegonautNapGoal - this only handles nighttime sleep
        if (dragon.level().isDay()) {
            return false;
        }

        Stegonaut drake = (Stegonaut) dragon;
        var prefs = sleepCapable.getSleepPreferences();

        boolean sheltered = !prefs.requiresShelter() || isSheltered();
        if (prefs.canSleepAtNight() && isNight() && sheltered) {
            if (prefs.avoidsThunderstorms() && dragon.level().isThundering()) {
                return false;
            }
            return true;
        }
        return false;
    }
}
