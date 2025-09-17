package com.leon.saintsdragons.server.ai.goals.lightningdragon;

import com.leon.saintsdragons.server.ai.goals.base.DragonSleepGoalBase;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;

/**
 * Lightning Dragon specific sleep goal.
 * 
 * Lightning Dragons have unique sleep patterns:
 * - Highly energized by storms and rain (less likely to sleep)
 * - Prefer to sleep during the day in sheltered areas
 * - Can sleep at night but are more active during storms
 * - Won't sleep while charging or during severe weather
 * 
 * Sleep Probability:
 * - Clear weather: Normal sleep patterns
 * - Rain: 40% chance to stay awake
 * - Storms: Only 15% chance to sleep (they're too excited!)
 */
public class LightningDragonSleepGoal extends DragonSleepGoalBase {
    
    public LightningDragonSleepGoal(LightningDragonEntity dragon) {
        super(dragon);
    }
    
    @Override
    public boolean canUse() {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Lightning Dragons have special sleep conditions
        if (lightningDragon.isCharging()) {
            return false; // Don't sleep while charging
        }
        
        // Additional check: Never sleep during thunderstorms (double-check)
        if (lightningDragon.level().isThundering()) {
            return false; // Absolutely no sleeping during thunderstorms
        }
        
        // Weather conditions are now handled by the base class based on sleep preferences
        // Lightning Dragons will not sleep during thunderstorms (avoidsThunderstorms = true)
        
        return super.canUse();
    }
    
    @Override
    public boolean canContinueToUse() {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Additional check: Stop sleeping immediately if thunderstorm starts
        if (lightningDragon.level().isThundering()) {
            return false; // Immediately stop sleeping during thunderstorms
        }
        
        return super.canContinueToUse();
    }
    
    @Override
    protected boolean canWildDragonSleep() {
        // Use the base class implementation which properly handles weather conditions
        // based on the sleep preferences (avoidsThunderstorms = true)
        return super.canWildDragonSleep();
    }
    
    @Override
    public void start() {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Use the proper sleep transition system instead of bypassing it
        lightningDragon.startSleepEnter();
    }
    
    @Override
    public void stop() {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Use the proper sleep transition system instead of bypassing it
        lightningDragon.startSleepExit();
    }
}