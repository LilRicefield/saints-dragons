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
        // If agitated, stop sleeping immediately
        if (agitated()) return false;
        
        // Check if we should continue sleeping based on current conditions
        if (sleepCapable.isSleepSuppressed()) return false;
        
        if (dragon.isTame()) {
            return canTamedDragonSleep();
        } else {
            // For wild dragons, check if they should wake up due to day/night transition
            DragonSleepCapable.SleepPreferences prefs = sleepCapable.getSleepPreferences();
            
            // If dragon is sleeping at night and it becomes day, they should wake up
            if (prefs.canSleepAtNight() && !prefs.canSleepDuringDay() && isDay()) {
                return false; // Wake up when day comes
            }
            
            // If dragon is sleeping during day and it becomes night, they should wake up
            if (prefs.canSleepDuringDay() && !prefs.canSleepAtNight() && isNight()) {
                return false; // Wake up when night comes
            }
            
            // Otherwise, continue sleeping if conditions are still met
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
        
        // Check weather conditions first - tamed dragons should respect weather like wild dragons
        if (prefs.avoidsThunderstorms() && dragon.level().isThundering()) {
            return false; // Don't sleep during thunderstorms
        }
        
        // Check if owner is sleeping
        if (ownerSleeping()) return true;
        
        // Check night sleeping near owner
        if (prefs.sleepsNearOwner() && isNight() && nearOwner()) return true;
        
        // Tamed dragons should NOT sleep during day unless owner is sleeping
        // This allows them to wake up naturally when morning comes
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
}
