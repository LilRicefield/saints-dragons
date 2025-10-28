package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Sleep goal for Nulljaw. Tamed dragons curl up when their owner sleeps.
 * Wild dragons use {@link NulljawRestGoal} instead.
 */
public class NulljawSleepGoal extends Goal {

    private final Nulljaw drake;
    private int retryCooldown;

    public NulljawSleepGoal(Nulljaw drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldown > 0) retryCooldown--;
        if (retryCooldown > 0) return false;
        if (drake.isSleepLocked()) return false;
        if (!drake.canSleepNow() || drake.isSleepSuppressed()) return false;
        if (drake.isInWaterOrBubble() || drake.isInLava()) return false;
        if (drake.isDying() || drake.isVehicle()) return false;
        if (drake.getTarget() != null || drake.isAggressive()) return false;
        if (drake.level().isThundering()) return false;

        return drake.isTame() && ownerAsleep();
    }

    @Override
    public boolean canContinueToUse() {
        return drake.isSleepLocked();
    }

    @Override
    public void start() {
        drake.startSleepEnter();
    }

    @Override
    public void tick() {
        if (drake.level().isClientSide) return;
        if (drake.isSleepLocked() && !shouldRemainAsleep()) {
            if (!drake.isSleepTransitioning()) {
                drake.startSleepExit();
            }
        }
    }

    @Override
    public void stop() {
        if (!drake.isSleepLocked()) {
            retryCooldown = 100;
        }
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    private boolean ownerAsleep() {
        LivingEntity owner = drake.getOwner();
        if (!(owner instanceof Player player)) {
            return false;
        }
        if (!player.isSleeping() || !player.isAlive()) {
            return false;
        }
        return player.level() == drake.level();
    }

    private boolean shouldRemainAsleep() {
        return ownerAsleep();
    }
}
