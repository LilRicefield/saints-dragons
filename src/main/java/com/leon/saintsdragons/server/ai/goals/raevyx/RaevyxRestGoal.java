package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Casual rest goal for Raevyx - makes wild dragons sit down to rest occasionally.
 * This is NOT sleep - just a brief sitting animation for ambient behavior.
 */
public class RaevyxRestGoal extends Goal {

    private final Raevyx wyvern;
    private int restingTicks;
    private int restDuration;
    private int retryCooldown;

    public RaevyxRestGoal(Raevyx wyvern) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Cooldown between rest attempts
        if (retryCooldown > 0) {
            retryCooldown--;
            return false;
        }

        // Only wild (untamed) dragons rest casually
        if (wyvern.isTame()) return false;

        // Don't rest if busy with other things
        if (wyvern.isOrderedToSit()) return false;
        if (wyvern.isSleepLocked()) return false;
        if (wyvern.isInWaterOrBubble() || wyvern.isInLava()) return false;
        if (wyvern.isDying() || wyvern.isVehicle()) return false;
        if (wyvern.getTarget() != null || wyvern.isAggressive()) return false;
        if (wyvern.isFlying()) return false;

        // Random chance to rest (about 1% chance per second when idle)
        return wyvern.getRandom().nextFloat() < 0.0005f;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue resting until duration is complete
        return restingTicks < restDuration && !wyvern.isInWaterOrBubble() && wyvern.getTarget() == null;
    }

    @Override
    public void start() {
        // Random rest duration: 60-120 ticks (3-6 seconds)
        restDuration = 60 + wyvern.getRandom().nextInt(61);
        restingTicks = 0;

        // Trigger sit down (will be handled by sitProgress system)
        wyvern.setOrderedToSit(true);
        wyvern.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (wyvern.level().isClientSide) return;

        restingTicks++;

        // Stop navigation and stay still
        wyvern.getNavigation().stop();
        wyvern.setDeltaMovement(0, wyvern.getDeltaMovement().y, 0);
    }

    @Override
    public void stop() {
        // Stand back up
        wyvern.setOrderedToSit(false);

        // Set cooldown before next rest (200-400 ticks = 10-20 seconds)
        retryCooldown = 200 + wyvern.getRandom().nextInt(201);

        restingTicks = 0;
        restDuration = 0;
    }

    @Override
    public boolean isInterruptable() {
        return true; // Can be interrupted by threats
    }
}
