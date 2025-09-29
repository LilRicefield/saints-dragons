package com.leon.saintsdragons.server.ai.goals.riftdrake;

import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Simple follow-owner goal tailored for the ground-based Rift Drake.
 */
public class RiftDrakeFollowOwnerGoal extends Goal {
    private static final double START_DISTANCE = 12.0D;
    private static final double STOP_DISTANCE = 7.0D;
    private static final double RUN_DISTANCE = 18.0D;
    private static final double TELEPORT_DISTANCE = 32.0D;

    private final RiftDrakeEntity drake;
    private int repathCooldown;
    private BlockPos lastOwnerPos;

    public RiftDrakeFollowOwnerGoal(RiftDrakeEntity drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!drake.isTame() || drake.isOrderedToSit() || drake.isVehicle()) {
            return false;
        }

        LivingEntity owner = drake.getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != drake.level()) {
            return false;
        }

        // Do not interfere while actively targeting something
        if (drake.getTarget() != null) {
            return false;
        }

        return drake.distanceToSqr(owner) > START_DISTANCE * START_DISTANCE;
    }

    @Override
    public boolean canContinueToUse() {
        if (!drake.isTame() || drake.isOrderedToSit() || drake.isVehicle()) {
            return false;
        }

        LivingEntity owner = drake.getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != drake.level()) {
            return false;
        }

        if (drake.getTarget() != null) {
            return false;
        }

        return drake.distanceToSqr(owner) > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        repathCooldown = 0;
        lastOwnerPos = null;
        drake.setAccelerating(false);
    }

    @Override
    public void stop() {
        drake.getNavigation().stop();
        drake.setAccelerating(false);
    }

    @Override
    public void tick() {
        LivingEntity owner = drake.getOwner();
        if (owner == null) {
            return;
        }

        double distance = drake.distanceTo(owner);

        // Emergency teleport when extremely far (e.g., after dimension travel glitches)
        if (distance > TELEPORT_DISTANCE) {
            drake.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            drake.getNavigation().stop();
            drake.setAccelerating(false);
            return;
        }

        // Keep eyes on the owner for a responsive feel
        drake.getLookControl().setLookAt(owner, 10.0F, drake.getMaxHeadXRot());

        if (distance <= STOP_DISTANCE) {
            drake.getNavigation().stop();
            drake.setAccelerating(false);
            return;
        }

        boolean shouldRun = distance > RUN_DISTANCE;
        drake.setAccelerating(shouldRun);

        if (repathCooldown > 0) {
            repathCooldown--;
        }

        boolean ownerMoved = lastOwnerPos == null || lastOwnerPos.distSqr(owner.blockPosition()) > 1;
        boolean navigatorIdle = drake.getNavigation().isDone() || !drake.getNavigation().isInProgress();

        if (navigatorIdle || ownerMoved || repathCooldown <= 0) {
            double speed = shouldRun ? 1.35D : 0.85D;
            drake.getNavigation().moveTo(owner, speed);
            lastOwnerPos = owner.blockPosition();
            repathCooldown = shouldRun ? 2 : 4;
        }
    }
}
