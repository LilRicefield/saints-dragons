package com.leon.saintsdragons.server.ai.goals;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Unified sleep goal for both tamed and wild dragons.
 * - Tamed: sleeps at night near owner, or immediately when owner sleeps.
 * - Wild: sleeps during daytime when sheltered; never during thunderstorms.
 * Goal runs only when calm (not ridden, not flying/dodging/aggro/targeting).
 */
public class DragonSleepGoal extends Goal {
    private final LightningDragonEntity dragon;

    public DragonSleepGoal(LightningDragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    private boolean isDay() { return dragon.level().isDay(); }
    private boolean isNight() { return !dragon.level().isDay(); }
    // Clear weather helper to avoid always-inverted checks at call sites
    private boolean isClearWeather() { return !dragon.level().isThundering(); }
    private boolean ownerSleeping() {
        LivingEntity owner = dragon.getOwner();
        return owner instanceof net.minecraft.world.entity.player.Player p && p.isSleeping();
    }
    private boolean nearOwner() {
        LivingEntity owner = dragon.getOwner();
        return owner != null && dragon.distanceToSqr(owner) <= (double) 14 * (double) 14;
    }
    private boolean isSheltered() {
        var level = dragon.level();
        var pos = dragon.blockPosition();
        boolean noSky = !level.canSeeSky(pos);
        int light = level.getMaxLocalRawBrightness(pos);
        return noSky || light < 7;
    }
    // Agitation helper to avoid always-inverted calm() checks at call sites
    private boolean agitated() {
        // Do not allow sleeping in fluids or lava, or while panicking/aggro/etc.
        if (dragon.isInWaterOrBubble() || dragon.isInLava()) return true;
        return dragon.isDying() || dragon.isDodging() || dragon.isFlying()
                || dragon.isVehicle() || dragon.getTarget() != null || dragon.isAggressive();
    }

    @Override
    public boolean canUse() {
        if (agitated()) return false;
        if (dragon.isSleepSuppressed()) return false;
        if (dragon.isTame()) {
            // Tamed dragons can sleep when owner sleeps OR at night near owner
            if (ownerSleeping()) return true;
            return isNight() && nearOwner();
        } else {
            return isDay() && isClearWeather() && isSheltered();
        }
    }

    @Override
    public boolean canContinueToUse() {
        // If already sleeping or transitioning, continue unless agitated
        if (dragon.isSleeping() || dragon.isSleepTransitioning()) {
            return !agitated();
        }
        
        // If not sleeping, check if we should start sleeping
        if (agitated()) return false;
        if (dragon.isSleepSuppressed()) return false;
        
        if (dragon.isTame()) {
            return ownerSleeping() || (isNight() && nearOwner());
        } else {
            return isDay() && isClearWeather() && isSheltered();
        }
    }

    @Override
    public void start() {
        dragon.startSleepEnter();
    }

    @Override
    public void stop() {
        dragon.startSleepExit();
    }
}
