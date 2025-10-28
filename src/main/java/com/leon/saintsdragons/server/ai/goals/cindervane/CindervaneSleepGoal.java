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

    private final Cindervane amphithere;
    private int retryCooldown;

    public CindervaneSleepGoal(Cindervane amphithere) {
        this.amphithere = amphithere;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldown > 0) {
            retryCooldown--;
            return false;
        }

        if (amphithere.isSleepLocked()) return false;
        if (!amphithere.canSleepNow() || amphithere.isSleepSuppressed()) return false;
        if (amphithere.isInWaterOrBubble() || amphithere.isInLava()) return false;
        if (amphithere.isDying() || amphithere.isVehicle() || amphithere.getTarget() != null || amphithere.isAggressive()) return false;
        if (amphithere.level().isThundering()) return false;

        // Only tamed behavior for now - wild behavior will be added later
        return amphithere.isTame() && ownerAsleep();
    }

    @Override
    public boolean canContinueToUse() {
        return amphithere.isSleepLocked();
    }

    @Override
    public void start() {
        amphithere.startSleepEnter();
    }

    @Override
    public void tick() {
        if (amphithere.level().isClientSide) return;
        if (amphithere.isSleepLocked() && !shouldRemainAsleep()) {
            if (!amphithere.isSleepTransitioning()) {
                amphithere.startSleepExit();
            }
        }
    }

    @Override
    public void stop() {
        if (!amphithere.isSleepLocked()) {
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
        LivingEntity owner = amphithere.getOwner();
        if (!(owner instanceof Player player)) {
            return false;
        }
        if (!player.isSleeping() || !player.isAlive()) {
            return false;
        }
        if (player.level() != amphithere.level()) {
            return false;
        }
        return true;
    }

    /**
     * Check if dragon should remain asleep.
     * For now, only tamed behavior is implemented.
     */
    private boolean shouldRemainAsleep() {
        if (amphithere.isTame()) {
            return ownerAsleep();
        }
        // Wild behavior will be added later
        return false;
    }
}
