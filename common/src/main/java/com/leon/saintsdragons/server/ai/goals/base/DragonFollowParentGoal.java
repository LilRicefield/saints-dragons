package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class DragonFollowParentGoal<T extends DragonEntity> extends Goal {
    private final T baby;
    private final Class<T> dragonClass;
    private final double speedModifier;
    private T parent;
    private int timeToRecalcPath;
    private static final double MAX_DISTANCE_SQ = 576.0D;
    private static final double SEARCH_HORIZONTAL_RANGE = 20.0D;
    private static final double SEARCH_VERTICAL_RANGE = 8.0D;
    private int wanderCooldown = 0;

    public DragonFollowParentGoal(T baby, Class<T> dragonClass, double speedModifier) {
        this.baby = baby;
        this.dragonClass = dragonClass;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!baby.isBaby() || baby.isTame() || baby.getOwner() != null || baby.isVehicle()) {
            return false;
        }

        T closestAdult = resolveParentCandidate();

        if (closestAdult == null) {
            return false;
        }

        this.parent = closestAdult;

        double minDist = calculateMinimumDistance(closestAdult);
        double closestDistance = baby.distanceToSqr(closestAdult);
        return !(closestDistance < minDist * minDist);
    }

    @Override
    public boolean canContinueToUse() {
        if (!baby.isBaby() || baby.isTame() || baby.getOwner() != null || baby.isVehicle()) {
            return false;
        }
        if (parent == null || !parent.isAlive() || parent.isBaby()) {
            return false;
        }
        UUID assignedParentUuid = baby.getAssignedParentUuid();
        if (assignedParentUuid == null || !assignedParentUuid.equals(parent.getUUID())) {
            T resolvedParent = resolveParentCandidate();
            if (resolvedParent == null) {
                return false;
            }
            this.parent = resolvedParent;
        }

        double dist = baby.distanceToSqr(parent);
        double minDist = calculateMinimumDistance(parent);

        return dist >= minDist * minDist && dist <= MAX_DISTANCE_SQ;
    }

    @Override
    public void start() {
        timeToRecalcPath = 0;
        wanderCooldown = 0;
    }

    @Override
    public void stop() {
        parent = null;
        baby.getNavigation().stop();
        wanderCooldown = 0; // Reset cooldown
    }


    private double calculateMinimumDistance(T parent) {
        double parentRadius = Math.max(parent.getBoundingBox().getXsize(), parent.getBoundingBox().getZsize()) / 2.0;
        double babyRadius = Math.max(baby.getBoundingBox().getXsize(), baby.getBoundingBox().getZsize()) / 2.0;
        double combinedRadii = parentRadius + babyRadius;
        double safeBuffer = 1.5D;
        return Math.max(2.75D, combinedRadii + safeBuffer);
    }

    @Override
    public void tick() {
        if (parent == null) {
            return;
        }

        double distToParent = baby.distanceToSqr(parent);
        double minDist = calculateMinimumDistance(parent);
        double minDistSq = minDist * minDist;
        double comfortableDistSq = minDistSq * 1.2;
        if (distToParent < minDistSq) {
            baby.getNavigation().stop();
            wanderCooldown = 40 + baby.getRandom().nextInt(40);
            return;
        }

        if (wanderCooldown > 0) {
            wanderCooldown--;
            if (distToParent > minDistSq * 2.0) {
                wanderCooldown = 0;
            }
            return;
        }

        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = this.adjustedTickDelay(8);
            if (distToParent >= comfortableDistSq) {
                baby.getNavigation().moveTo(parent, speedModifier);
            } else {
                baby.getNavigation().stop();
                wanderCooldown = 20 + baby.getRandom().nextInt(20);
            }
        }
    }

    private T resolveParentCandidate() {
        UUID assignedParentUuid = baby.getAssignedParentUuid();
        List<T> nearby = baby.level().getEntitiesOfClass(
                dragonClass,
                baby.getBoundingBox().inflate(SEARCH_HORIZONTAL_RANGE, SEARCH_VERTICAL_RANGE, SEARCH_HORIZONTAL_RANGE),
                adult -> adult != null && !adult.isBaby() && adult.isAlive()
        );

        if (assignedParentUuid != null) {
            for (T adult : nearby) {
                if (assignedParentUuid.equals(adult.getUUID())) {
                    return adult;
                }
            }
            baby.clearAssignedParentUuid();
        }

        double closestDistance = Double.MAX_VALUE;
        T closestAdult = null;
        for (T adult : nearby) {
            if (!adult.isFemale()) {
                continue;
            }
            double dist = baby.distanceToSqr(adult);
            if (dist < closestDistance) {
                closestDistance = dist;
                closestAdult = adult;
            }
        }

        if (closestAdult != null) {
            baby.setAssignedParentUuid(closestAdult.getUUID());
        }

        return closestAdult;
    }
}