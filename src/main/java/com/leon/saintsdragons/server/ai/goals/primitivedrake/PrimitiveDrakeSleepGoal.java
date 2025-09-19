package com.leon.saintsdragons.server.ai.goals.primitivedrake;

import com.leon.saintsdragons.server.ai.goals.base.DragonSleepGoalBase;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import com.leon.saintsdragons.server.entity.interfaces.DragonSleepCapable;

/**
 * Primitive Drake specific sleep goal.
 * 
 * Simple sleep behavior:
 * - Sleeps at night, awake during day
 * - Takes short naps during the day (1-2 minutes)
 * - No complex sleep transitions, just lie down and stand up
 * - Same behavior for both wild and tamed drakes
 */
public class PrimitiveDrakeSleepGoal extends DragonSleepGoalBase {
    
    public PrimitiveDrakeSleepGoal(PrimitiveDrakeEntity dragon) {
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
    protected boolean canWildDragonSleep() {
        DragonSleepCapable.SleepPreferences prefs = sleepCapable.getSleepPreferences();
        
        // Primitive Drake doesn't need shelter - simple sleep behavior
        // Check day sleeping (for naps)
        if (prefs.canSleepDuringDay() && isDay()) {
            // Check weather conditions
            if (prefs.avoidsThunderstorms() && dragon.level().isThundering()) {
                return false;
            }
            return true;
        }
        
        // Check night sleeping
        if (prefs.canSleepAtNight() && isNight()) {
            // Check weather conditions
            if (prefs.avoidsThunderstorms() && dragon.level().isThundering()) {
                return false;
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public void start() {
        // Use the simple sleep system - no complex transitions
        super.start();
    }
    
    @Override
    public void stop() {
        // Use the simple sleep system - no complex transitions
        super.stop();
    }
}
