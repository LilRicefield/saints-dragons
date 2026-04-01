package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Generic follow-parent behavior for untamed baby dragons.
 * Mirrors vanilla {@link net.minecraft.world.entity.ai.goal.FollowParentGoal}
 * but only activates for wild hatchlings so tamed babies can prioritize their owner.
 *
 * Babies maintain a comfortable distance (5-7 blocks) and wander around naturally
 * instead of constantly pushing into the parent.
 *
 * @param <T> The dragon type (e.g., Raevyx, Ignivorus, etc.)
 */
public class DragonFollowParentGoal<T extends DragonEntity> extends Goal {
    private final T baby;
    private final Class<T> dragonClass;
    private final double speedModifier;
    private T parent;
    private int timeToRecalcPath;

    // Comfortable following distance - babies stay 5-7 blocks away
    private static final double MIN_DISTANCE_SQ = 25.0D; // 5 blocks
    private static final double MAX_DISTANCE_SQ = 576.0D; // 24 blocks
    private static final double SEARCH_HORIZONTAL_RANGE = 20.0D;
    private static final double SEARCH_VERTICAL_RANGE = 8.0D;

    // Wandering behavior - don't constantly path to parent
    private int wanderCooldown = 0;

    public DragonFollowParentGoal(T baby, Class<T> dragonClass, double speedModifier) {
        this.baby = baby;
        this.dragonClass = dragonClass;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only wild, un-owned, un-ridden babies follow parents - tamed/owned/ridden babies follow owner instead
        if (!baby.isBaby() || baby.isTame() || baby.getOwner() != null || baby.isVehicle()) {
            return false;
        }

        T closestAdult = resolveParentCandidate();

        if (closestAdult == null) {
            return false;
        }

        this.parent = closestAdult;

        // Calculate dynamic minimum distance based on entity sizes
        double minDist = calculateMinimumDistance(closestAdult);
        double closestDistance = baby.distanceToSqr(closestAdult);

        // Only follow if too far away (beyond minimum comfortable distance)
        if (closestDistance < minDist * minDist) {
            return false; // Already close enough, let baby wander
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop following parent if tamed, owned, or being ridden - prioritize owner/rider instead
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

        // Stop following if too close (within minimum distance)
        // Or if too far (beyond maximum distance)
        return dist >= minDist * minDist && dist <= MAX_DISTANCE_SQ;
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

    /**
     * Calculate the minimum safe following distance based on entity hitbox sizes.
     * For multi-part entities like Ignivorus (8 blocks wide), this prevents babies
     * from trying to path into the parent's collision box.
     *
     * @param parent The parent entity to follow
     * @return Minimum distance in blocks (not squared)
     */
    private double calculateMinimumDistance(T parent) {
        // Get the horizontal radius (half-width) of each entity
        double parentRadius = Math.max(parent.getBoundingBox().getXsize(), parent.getBoundingBox().getZsize()) / 2.0;
        double babyRadius = Math.max(baby.getBoundingBox().getXsize(), baby.getBoundingBox().getZsize()) / 2.0;

        // Combined radii (where entities would just touch)
        double combinedRadii = parentRadius + babyRadius;

        // Keep babies close enough to visibly follow without crowding.
        double safeBuffer = 1.5D;
        return Math.max(2.75D, combinedRadii + safeBuffer);
    }

    @Override
    public void tick() {
        if (parent == null) {
            return;
        }

        double distToParent = baby.distanceToSqr(parent);

        // Calculate dynamic minimum distance based on entity hitboxes
        double minDist = calculateMinimumDistance(parent);
        double minDistSq = minDist * minDist;
        double comfortableDistSq = minDistSq * 1.2; // 20% buffer beyond minimum

        // CRITICAL: Check distance EVERY tick and stop immediately if too close
        // This prevents pushing while old navigation paths are still active
        if (distToParent < minDistSq) {
            baby.getNavigation().stop();
            wanderCooldown = 40 + baby.getRandom().nextInt(40); // 2-4 seconds of wandering
            return;
        }

        // Decrement wander cooldown
        if (wanderCooldown > 0) {
            wanderCooldown--;
            // If parent is getting too far during wandering, cancel wander and follow
            if (distToParent > minDistSq * 2.0) {
                wanderCooldown = 0;
            }
            return; // Don't path to parent while wandering
        }

        // Recalculate path periodically
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = this.adjustedTickDelay(8);

            // Only path if far enough away (beyond comfortable distance)
            if (distToParent >= comfortableDistSq) {
                baby.getNavigation().moveTo(parent, speedModifier);
            } else {
                // Close enough, stop and wander
                baby.getNavigation().stop();
                wanderCooldown = 20 + baby.getRandom().nextInt(20); // 1-2 seconds of wandering
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
