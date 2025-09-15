package com.leon.saintsdragons.server.ai.goals.lightningdragon;

import com.leon.saintsdragons.server.ai.goals.base.DragonSleepGoalBase;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.DragonSleepCapable;

/**
 * Lightning Dragon specific sleep goal.
 * Extends the base sleep goal with Lightning Dragon specific behaviors.
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
        
        // Lightning Dragons are more active during storms
        if (lightningDragon.level().isThundering() && lightningDragon.getRandom().nextFloat() < 0.7f) {
            return false; // 70% chance to stay awake during storms
        }
        
        return super.canUse();
    }
    
    @Override
    protected boolean canWildDragonSleep() {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Lightning Dragons have unique sleep preferences
        DragonSleepCapable.SleepPreferences prefs = sleepCapable.getSleepPreferences();
        
        // Check day sleeping (Lightning Dragons prefer to sleep during day)
        if (prefs.canSleepDuringDay() && isDay() && isSheltered()) {
            // Lightning Dragons are less affected by thunderstorms
            if (prefs.avoidsThunderstorms() && lightningDragon.level().isThundering()) {
                // Only 30% chance to avoid sleeping during storms
                return lightningDragon.getRandom().nextFloat() < 0.3f;
            }
            return true;
        }
        
        // Check night sleeping (Lightning Dragons can sleep at night but prefer day)
        if (prefs.canSleepAtNight() && isNight() && isSheltered()) {
            // Lightning Dragons are less affected by thunderstorms
            if (prefs.avoidsThunderstorms() && lightningDragon.level().isThundering()) {
                // Only 30% chance to avoid sleeping during storms
                return lightningDragon.getRandom().nextFloat() < 0.3f;
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public void start() {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Lightning Dragon specific sleep start behavior
        lightningDragon.setSleeping(true);
        // lightningDragon.setSleepTransitioning(true);
        
        // Trigger Lightning Dragon specific sleep animation
        lightningDragon.playSleepAnimation();
        
        super.start();
    }
    
    @Override
    public void stop() {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Lightning Dragon specific sleep stop behavior
        lightningDragon.setSleeping(false);
        // lightningDragon.setSleepTransitioning(false);
        
        // Trigger Lightning Dragon specific wake animation
        lightningDragon.playWakeAnimation();
        
        super.stop();
    }
}
