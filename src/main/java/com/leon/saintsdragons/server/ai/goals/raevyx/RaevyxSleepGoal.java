package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Sleep goal for Raevyx - ONLY for tamed dragons when owner sleeps.
 * Wild dragons use RaevyxRestGoal for ambient resting behavior instead.
 */
public class RaevyxSleepGoal extends Goal {

    private final Raevyx wyvern;
    private int retryCooldown;

    public RaevyxSleepGoal(Raevyx wyvern) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldown > 0) retryCooldown--;
        if (retryCooldown > 0) return false;
        if (wyvern.isSleepLocked()) return false;
        if (!wyvern.canSleepNow() || wyvern.isSleepSuppressed()) return false;
        if (wyvern.isInWaterOrBubble() || wyvern.isInLava()) return false;
        if (wyvern.isDying() || wyvern.isVehicle() || wyvern.getTarget() != null || wyvern.isAggressive()) return false;
        if (wyvern.level().isThundering()) return false;

        // ONLY tamed dragons sleep (when owner sleeps). Wild dragons use RestGoal instead.
        return wyvern.isTame() && ownerAsleep();
    }

    @Override
    public boolean canContinueToUse() {
        return wyvern.isSleepLocked();
    }

    @Override
    public void start() {
        wyvern.startSleepEnter();
    }

    @Override
    public void tick() {
        if (wyvern.level().isClientSide) return;
        if (wyvern.isSleepLocked() && !shouldRemainAsleep()) {
            if (!wyvern.isSleepTransitioning()) {
                wyvern.startSleepExit();
            }
        }
    }

    @Override
    public void stop() {
        if (!wyvern.isSleepLocked()) {
            retryCooldown = 100;
        }
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    private boolean ownerAsleep() {
        LivingEntity owner = wyvern.getOwner();
        if (!(owner instanceof Player player)) {
            return false;
        }
        if (!player.isSleeping() || !player.isAlive()) {
            return false;
        }
        if (player.level() != wyvern.level()) {
            return false;
        }
        return true;
    }

    private boolean shouldRemainAsleep() {
        // Only tamed dragons sleep, so only check owner status
        return ownerAsleep();
    }
}
