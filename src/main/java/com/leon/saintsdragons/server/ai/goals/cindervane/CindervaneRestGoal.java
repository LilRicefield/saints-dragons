package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Casual rest goal for Cindervane - makes wild dragons sit down to rest occasionally.
 * This is NOT sleep - just a brief sitting animation for ambient behavior.
 */
public class CindervaneRestGoal extends Goal {

    private final Cindervane dragon;
    private int restingTicks;
    private int restDuration;
    private int retryCooldown;

    public CindervaneRestGoal(Cindervane dragon) {
        this.dragon = dragon;
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
        if (dragon.isTame()) return false;

        // Don't rest if busy with other things
        if (dragon.isOrderedToSit()) return false;
        if (dragon.isSleepLocked()) return false;
        if (dragon.isInWaterOrBubble() || dragon.isInLava()) return false;
        if (dragon.isDying() || dragon.isVehicle()) return false;
        if (dragon.getTarget() != null || dragon.isAggressive()) return false;
        if (dragon.isFlying()) return false;

        // Random chance to rest (about 1% chance per second when idle)
        return dragon.getRandom().nextFloat() < 0.0005f;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue resting until duration is complete
        return restingTicks < restDuration && !dragon.isInWaterOrBubble() && dragon.getTarget() == null;
    }

    @Override
    public void start() {
        // Random rest duration: 60-120 ticks (3-6 seconds)
        restDuration = 60 + dragon.getRandom().nextInt(61);
        restingTicks = 0;

        // Trigger sit down (will be handled by sitProgress system)
        dragon.setOrderedToSit(true);
        dragon.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (dragon.level().isClientSide) return;

        restingTicks++;

        // Stop navigation and stay still
        dragon.getNavigation().stop();
        dragon.setDeltaMovement(0, dragon.getDeltaMovement().y, 0);
    }

    @Override
    public void stop() {
        // Stand back up
        dragon.setOrderedToSit(false);

        // Set cooldown before next rest (200-400 ticks = 10-20 seconds)
        retryCooldown = 200 + dragon.getRandom().nextInt(201);

        restingTicks = 0;
        restDuration = 0;
    }

    @Override
    public boolean isInterruptable() {
        return true; // Can be interrupted by threats
    }
}
