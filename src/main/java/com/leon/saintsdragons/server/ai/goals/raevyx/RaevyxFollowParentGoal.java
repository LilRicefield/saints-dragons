package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Simple follow-parent behaviour for untamed baby Raevyx.
 * Mirrors vanilla {@link net.minecraft.world.entity.ai.goal.FollowParentGoal}
 * but only activates for wild hatchlings so tamed babies can prioritise their owner.
 */
public class RaevyxFollowParentGoal extends Goal {
    private final Raevyx baby;
    private final double speedModifier;
    private Raevyx parent;
    private int timeToRecalcPath;

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

        if (closestDistance < 9.0D) { // Already close enough
            return false;
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
        return dist >= 9.0D && dist <= 256.0D;
    }

    @Override
    public void start() {
        timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        parent = null;
    }

    @Override
    public void tick() {
        if (--timeToRecalcPath <= 0 && parent != null) {
            timeToRecalcPath = this.adjustedTickDelay(10);
            baby.getNavigation().moveTo(parent, speedModifier);
        }
    }
}
