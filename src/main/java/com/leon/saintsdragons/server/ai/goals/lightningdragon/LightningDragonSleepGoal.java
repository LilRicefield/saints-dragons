package com.leon.saintsdragons.server.ai.goals.lightningdragon;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Fresh sleep goal for Lightning Dragons.
 */
public class LightningDragonSleepGoal extends Goal {

    private final LightningDragonEntity dragon;
    private int retryCooldown;

    public LightningDragonSleepGoal(LightningDragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldown > 0) retryCooldown--;
        if (retryCooldown > 0) return false;
        if (dragon.isSleepLocked()) return false;
        if (!dragon.canSleepNow() || dragon.isSleepSuppressed()) return false;
        if (dragon.isInWaterOrBubble() || dragon.isInLava()) return false;
        if (dragon.isDying() || dragon.isVehicle() || dragon.getTarget() != null || dragon.isAggressive()) return false;
        if (dragon.level().isThundering()) return false;

        return dragon.isTame() ? ownerAsleepNearby() : wildShouldSleep();
    }

    @Override
    public boolean canContinueToUse() {
        return dragon.isSleepLocked();
    }

    @Override
    public void start() {
        dragon.startSleepEnter();
    }

    @Override
    public void tick() {
        if (dragon.level().isClientSide) return;
        if (dragon.isSleepLocked() && !shouldRemainAsleep()) {
            if (!dragon.isSleepTransitioning()) {
                dragon.startSleepExit();
            }
        }
    }

    @Override
    public void stop() {
        if (!dragon.isSleepLocked()) {
            retryCooldown = 100;
        }
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    private boolean ownerAsleepNearby() {
        LivingEntity owner = dragon.getOwner();
        if (!(owner instanceof Player player)) {
            return false;
        }
        if (!player.isSleeping() || !player.isAlive()) {
            return false;
        }
        if (player.level() != dragon.level()) {
            return false;
        }
        return dragon.distanceToSqr(owner) <= 14 * 14;
    }

    private boolean wildShouldSleep() {
        // Wild lightning dragons nap during the day when sheltered.
        if (!dragon.level().isDay()) return false;
        var pos = dragon.blockPosition();
        var level = dragon.level();
        return !level.canSeeSky(pos) || level.getMaxLocalRawBrightness(pos) < 7;
    }

    private boolean shouldRemainAsleep() {
        if (dragon.isTame()) {
            return ownerAsleepNearby();
        }
        return wildShouldSleep();
    }
}