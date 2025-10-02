package com.leon.saintsdragons.server.ai.goals.primitivedrake;

import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Simple follow owner goal for Primitive Drake.
 * Ground-based following with teleportation for extreme distances.
 */
public class PrimitiveDrakeFollowOwnerGoal extends Goal {
    private final PrimitiveDrakeEntity drake;

    // Distance constants - tuned for ground-based following
    private static final double START_FOLLOW_DIST = 12.0;
    private static final double STOP_FOLLOW_DIST = 8.0;
    private static final double TELEPORT_DIST = 2000.0;

    // Performance optimization - don't re-path constantly
    private BlockPos previousOwnerPos;
    private int repathCooldown = 0;

    public PrimitiveDrakeFollowOwnerGoal(PrimitiveDrakeEntity drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Basic requirements
        if (!drake.isTame() || drake.isOrderedToSit()) {
            return false;
        }

        // Never follow while playing dead
        if (drake.isPlayingDead()) {
            return false;
        }

        // Never follow while actively targeting an enemy
        LivingEntity target = drake.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }

        LivingEntity owner = drake.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        // Must be in same dimension
        if (owner.level() != drake.level()) {
            return false;
        }

        // Only follow if owner is far enough away
        double ownerDist = drake.distanceToSqr(owner);
        return ownerDist > START_FOLLOW_DIST * START_FOLLOW_DIST;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity owner = drake.getOwner();
        if (owner == null || !owner.isAlive() || drake.isOrderedToSit()) {
            return false;
        }

        // Suspend following while playing dead
        if (drake.isPlayingDead()) {
            return false;
        }

        // Suspend following while fighting
        LivingEntity target = drake.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }

        if (owner.level() != drake.level()) {
            return false;
        }

        // Keep following until we're close enough
        double dist = drake.distanceToSqr(owner);
        return dist > STOP_FOLLOW_DIST * STOP_FOLLOW_DIST;
    }

    @Override
    public void start() {
        // Reset tracking
        previousOwnerPos = null;
        repathCooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity owner = drake.getOwner();
        if (owner == null) return;

        double distance = drake.distanceTo(owner);

        // Emergency teleport if owner gets stupidly far away
        if (distance > TELEPORT_DIST) {
            drake.teleportTo(owner.getX(), owner.getY() + 1, owner.getZ());
            return;
        }

        // Always look at owner while following
        drake.getLookControl().setLookAt(owner, 10.0f, 10.0f);

        // Handle ground following
        handleGroundFollowing(owner, distance);
    }

    /**
     * Handle following on ground
     */
    private void handleGroundFollowing(LivingEntity owner, double distance) {
        // Check if we should stop moving
        if (distance <= STOP_FOLLOW_DIST) {
            drake.getNavigation().stop();
            return;
        }

        // Only update movement state if we're not currently moving or need to repath
        if (drake.getNavigation().isDone() || !drake.getNavigation().isInProgress()) {
            // Primitive Drake always walks - never runs (lore-appropriate)
            double baseSpeed = 0.8;
            // Slightly increase speed when farther away to help catch up
            double speed = baseSpeed * (1.0 + (distance / 100.0));
            speed = Math.min(speed, 1.0); // Cap at walking speed

            // Check if we need to recalculate the path
            boolean navDone = drake.getNavigation().isDone();
            boolean ownerMoved = previousOwnerPos == null || previousOwnerPos.distSqr(owner.blockPosition()) > 1;
            boolean cooldownExpired = (repathCooldown-- <= 0);

            if (navDone || ownerMoved || cooldownExpired) {
                // Try to move directly to the owner if path is clear
                if (distance < 16.0 && drake.getNavigation().createPath(owner, 0) != null) {
                    drake.getNavigation().moveTo(owner, speed);
                } else {
                    // If path is blocked or far away, try to get closer first
                    drake.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), speed);
                }
                previousOwnerPos = owner.blockPosition();
                repathCooldown = 2; // More frequent updates for better following
            }
        }
        
        // If stuck, try to jump or find alternative path
        if (drake.getNavigation().isStuck()) {
            drake.getJumpControl().jump();
            drake.getNavigation().stop();
            repathCooldown = 0; // Force repath next tick
        }
    }

    @Override
    public void stop() {
        drake.getNavigation().stop();
        previousOwnerPos = null;
    }
}
