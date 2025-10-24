package com.leon.saintsdragons.server.ai.goals.stegonaut;

import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Handles daytime napping for Stegonauts.
 * Separate from the main sleep goal to avoid conflicts.
 * Wild drakes occasionally take short naps during the day.
 */
public class StegonautNapGoal extends Goal {
    private final Stegonaut drake;
    private int napDuration;
    private int napTicks;

    private static final int MIN_NAP_DURATION = 60;  // 3 seconds
    private static final int MAX_NAP_DURATION = 120; // 6 seconds
    private static final int NAP_COOLDOWN = 600;     // 30 seconds between naps
    private int napCooldown = 0;

    public StegonautNapGoal(Stegonaut drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only during daytime
        if (!drake.level().isDay()) {
            return false;
        }

        // Don't nap if already sleeping via main sleep goal
        if (drake.isSleeping()) {
            return false;
        }

        // Don't nap if sitting, playing dead, or being ridden
        if (drake.isOrderedToSit() || drake.isPlayingDead() || drake.isVehicle()) {
            return false;
        }

        // Cooldown between naps
        if (napCooldown > 0) {
            napCooldown--;
            return false;
        }

        // Random chance to nap (~0.5% per tick when conditions are met)
        return drake.getRandom().nextFloat() < 0.005f;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if disturbed
        if (drake.getTarget() != null || drake.isVehicle()) {
            return false;
        }

        // Stop if owner commands to stand
        if (drake.isTame() && !drake.isOrderedToSit()) {
            return false;
        }

        // Stop if nap duration is over
        return napTicks < napDuration;
    }

    @Override
    public void start() {
        // Set nap duration
        napDuration = MIN_NAP_DURATION + drake.getRandom().nextInt(MAX_NAP_DURATION - MIN_NAP_DURATION);
        napTicks = 0;

        // Sit down
        drake.setOrderedToSit(true);
        drake.getNavigation().stop();
    }

    @Override
    public void tick() {
        napTicks++;
    }

    @Override
    public void stop() {
        // Stand back up
        drake.setOrderedToSit(false);

        // Set cooldown before next nap
        napCooldown = NAP_COOLDOWN + drake.getRandom().nextInt(NAP_COOLDOWN);
        napTicks = 0;
    }
}
