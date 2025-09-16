package com.leon.saintsdragons.server.ai.goals.lightningdragon;

import com.leon.saintsdragons.server.ai.goals.base.DragonSleepGoalBase;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.DragonSleepCapable;

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
        
        // Lightning Dragons are energized by storms - much less likely to sleep
        if (lightningDragon.level().isThundering()) {
            // Only 15% chance to sleep during storms (they're too excited!)
            return lightningDragon.getRandom().nextFloat() < 0.15f;
        }
        
        // Lightning Dragons are more active during rain (but not as much as storms)
        if (lightningDragon.level().isRaining() && lightningDragon.getRandom().nextFloat() < 0.4f) {
            return false; // 40% chance to stay awake during rain
        }
        
        return super.canUse();
    }
    
    @Override
    protected boolean canWildDragonSleep() {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        DragonSleepCapable.SleepPreferences prefs = sleepCapable.getSleepPreferences();
        
        // Check if basic conditions are met (day/night + shelter)
        boolean canSleepNow = (prefs.canSleepDuringDay() && isDay() && isSheltered()) ||
                             (prefs.canSleepAtNight() && isNight() && isSheltered());
        
        if (!canSleepNow) return false;
        
        // Lightning Dragons are energized by storms - less likely to sleep during them
        if (prefs.avoidsThunderstorms() && lightningDragon.level().isThundering()) {
            // Only 30% chance to sleep during storms (they're too excited!)
            return lightningDragon.getRandom().nextFloat() < 0.3f;
        }
        
        return true;
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