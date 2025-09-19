package com.leon.saintsdragons.server.ai.goals.primitivedrake;

import com.leon.saintsdragons.server.ai.goals.base.DragonSleepGoalBase;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;

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
