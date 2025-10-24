package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Sleep goal for Cindervanes.
 * Tamed Cindervanes sleep at night when their owner sleeps nearby.
 * Wild behavior will be implemented later.
 */
public class CindervaneSleepGoal extends Goal {

    private final Cindervane dragon;
    private int retryCooldown;

    public CindervaneSleepGoal(Cindervane dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldown > 0) {
            retryCooldown--;
            return false;
        }

        if (dragon.isSleepLocked()) return false;
        if (!dragon.canSleepNow() || dragon.isSleepSuppressed()) return false;
        if (dragon.isInWaterOrBubble() || dragon.isInLava()) return false;
        if (dragon.isDying() || dragon.isVehicle() || dragon.getTarget() != null || dragon.isAggressive()) return false;
        if (dragon.level().isThundering()) return false;

        // Only tamed behavior for now - wild behavior will be added later
        return dragon.isTame() && ownerAsleep();
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

    /**
     * Check if owner is sleeping.
     * Cindervanes sleep whenever their owner sleeps, regardless of distance.
     */
    private boolean ownerAsleep() {
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
        return true;
    }

    /**
     * Check if dragon should remain asleep.
     * For now, only tamed behavior is implemented.
     */
    private boolean shouldRemainAsleep() {
        if (dragon.isTame()) {
            return ownerAsleep();
        }
        // Wild behavior will be added later
        return false;
    }
}
