package com.leon.saintsdragons.server.ai.goals.raevyx.baby;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Simple follow-parent behaviour for untamed baby Raevyx.
 * Mirrors vanilla {@link net.minecraft.world.entity.ai.goal.FollowParentGoal}
 * but only activates for wild hatchlings so tamed babies can prioritise their owner.
 *
 * Babies maintain a comfortable distance (5-7 blocks) and wander around naturally
 * instead of constantly pushing into the parent.
 */
public class RaevyxFollowParentGoal extends Goal {
    private final Raevyx baby;
    private final double speedModifier;
    private Raevyx parent;
    private int timeToRecalcPath;

    // Comfortable following distance - babies stay 5-7 blocks away
    private static final double MIN_DISTANCE_SQ = 25.0D; // 5 blocks
    private static final double MAX_DISTANCE_SQ = 256.0D; // 16 blocks

    // Wandering behavior - don't constantly path to parent
    private int wanderCooldown = 0;

    public RaevyxFollowParentGoal(Raevyx baby, double speedModifier) {
        this.baby = baby;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!baby.isBaby() || baby.isTame()) {
            return false;
        }

        List<Raevyx> nearby = baby.level().getEntitiesOfClass(
                Raevyx.class,
                baby.getBoundingBox().inflate(12.0D, 6.0D, 12.0D),
                adult -> adult != null && !adult.isBaby() && adult.isAlive()
        );

        double closestDistance = Double.MAX_VALUE;
        Raevyx closestAdult = null;
        for (Raevyx adult : nearby) {
            double dist = baby.distanceToSqr(adult);
            if (dist < closestDistance) {
                closestDistance = dist;
                closestAdult = adult;
            }
        }

        if (closestAdult == null) {
            return false;
        }

        // Only follow if too far away (beyond minimum comfortable distance)
        if (closestDistance < MIN_DISTANCE_SQ) {
            return false; // Already close enough, let baby wander
        }

        this.parent = closestAdult;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!baby.isBaby() || baby.isTame()) {
            return false;
        }
        if (parent == null || !parent.isAlive() || parent.isBaby()) {
            return false;
        }

        double dist = baby.distanceToSqr(parent);

        // Stop following if too close (within minimum distance)
        // Or if too far (beyond maximum distance)
        return dist >= MIN_DISTANCE_SQ && dist <= MAX_DISTANCE_SQ;
    }

    @Override
    public void start() {
        timeToRecalcPath = 0;
        wanderCooldown = 0; // Don't wander on start, begin following immediately
    }

    @Override
    public void stop() {
        parent = null;
        baby.getNavigation().stop();
        wanderCooldown = 0; // Reset cooldown
    }

    @Override
    public void tick() {
        if (parent == null) {
            return;
        }

        double distToParent = baby.distanceToSqr(parent);

        // Decrement wander cooldown
        if (wanderCooldown > 0) {
            wanderCooldown--;
            // If parent is getting too far during wandering, cancel wander and follow
            if (distToParent > MIN_DISTANCE_SQ * 2.0) { // Beyond 10 blocks
                wanderCooldown = 0;
            }
            return; // Don't path to parent while wandering
        }

        // Recalculate path periodically
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = this.adjustedTickDelay(12); // Balanced recalc speed
            baby.getNavigation().moveTo(parent, speedModifier);

            // Only wander if very close to parent (to prevent pushing)
            if (distToParent < MIN_DISTANCE_SQ * 1.2) { // Within ~5.5 blocks
                wanderCooldown = 20 + baby.getRandom().nextInt(20); // 1-2 seconds of wandering
            }
        }
    }
}
