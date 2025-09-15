package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.DragonSleepCapable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Base class for dragon sleep goals.
 * Works with any dragon that implements DragonSleepCapable.
 */
public abstract class DragonSleepGoalBase extends Goal {
    protected final DragonEntity dragon;
    protected final DragonSleepCapable sleepCapable;
    
    public DragonSleepGoalBase(DragonEntity dragon) {
        this.dragon = dragon;
        this.sleepCapable = (DragonSleepCapable) dragon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }
    
    @Override
    public boolean canUse() {
        if (agitated()) return false;
        if (sleepCapable.isSleepSuppressed()) return false;
        
        // Use interface method to check if dragon can sleep
        if (!sleepCapable.canSleepNow()) return false;
        
        if (dragon.isTame()) {
            return canTamedDragonSleep();
        } else {
            return canWildDragonSleep();
        }
    }
    
    @Override
    public boolean canContinueToUse() {
        // If already sleeping or transitioning, check if we should wake up
        if (sleepCapable.isSleeping() || sleepCapable.isSleepTransitioning()) {
            // Wake up if agitated
            if (agitated()) return false;
            
            // Wake up based on time patterns
            if (dragon.isTame()) {
                return shouldTamedDragonContinueSleeping();
            } else {
                return shouldWildDragonContinueSleeping();
            }
        }
        
        // If not sleeping, check if we should start sleeping
        if (agitated()) return false;
        if (sleepCapable.isSleepSuppressed()) return false;
        
        if (dragon.isTame()) {
            return canTamedDragonSleep();
        } else {
            return canWildDragonSleep();
        }
    }
    
    @Override
    public void start() {
        sleepCapable.startSleepEnter();
    }
    
    @Override
    public void stop() {
        sleepCapable.startSleepExit();
    }
    
    protected boolean agitated() {
        // Do not allow sleeping in fluids or lava, or while panicking/aggro/etc.
        if (dragon.isInWaterOrBubble() || dragon.isInLava()) return true;
        return dragon.isDeadOrDying() || dragon.isVehicle() || dragon.getTarget() != null || dragon.isAggressive();
    }
    
    protected boolean canTamedDragonSleep() {
        DragonSleepCapable.SleepPreferences prefs = sleepCapable.getSleepPreferences();
        
        // Check if owner is sleeping
        if (ownerSleeping()) return true;
        
        // Check night sleeping near owner
        if (prefs.sleepsNearOwner() && isNight() && nearOwner()) return true;
        
        return false;
    }
    
    protected boolean canWildDragonSleep() {
        DragonSleepCapable.SleepPreferences prefs = sleepCapable.getSleepPreferences();
        
        // Check day sleeping
        if (prefs.canSleepDuringDay() && isDay() && isSheltered()) {
            // Check weather conditions
            if (prefs.avoidsThunderstorms() && dragon.level().isThundering()) {
                return false;
            }
            return true;
        }
        
        // Check night sleeping
        if (prefs.canSleepAtNight() && isNight() && isSheltered()) {
            // Check weather conditions
            if (prefs.avoidsThunderstorms() && dragon.level().isThundering()) {
                return false;
            }
            return true;
        }
        
        return false;
    }
    
    protected boolean isDay() { 
        return dragon.level().isDay(); 
    }
    
    protected boolean isNight() { 
        return !dragon.level().isDay(); 
    }
    
    protected boolean ownerSleeping() {
        LivingEntity owner = dragon.getOwner();
        return owner instanceof Player p && p.isSleeping();
    }
    
    protected boolean nearOwner() {
        LivingEntity owner = dragon.getOwner();
        return owner != null && dragon.distanceToSqr(owner) <= (double) 14 * (double) 14;
    }
    
    protected boolean isSheltered() {
        var level = dragon.level();
        var pos = dragon.blockPosition();
        boolean noSky = !level.canSeeSky(pos);
        int light = level.getMaxLocalRawBrightness(pos);
        return noSky || light < 7;
    }
    
    protected boolean shouldTamedDragonContinueSleeping() {
        // Tamed dragons sleep at night and wake up in the morning
        // They follow the owner's pattern: sleep at night regardless of owner's sleep state
        return isNight();
    }
    
    protected boolean shouldWildDragonContinueSleeping() {
        // Wild dragons sleep during the day (in shelter) and wake up at night
        // They sleep in the morning and wake up when it gets dark
        return isDay() && isSheltered();
    }
}
