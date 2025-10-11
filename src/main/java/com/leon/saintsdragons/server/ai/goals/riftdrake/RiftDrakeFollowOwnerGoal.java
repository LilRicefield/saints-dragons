package com.leon.saintsdragons.server.ai.goals.riftdrake;

import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraft.util.Mth;
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
    private int pathRecalcCooldown;
    private double lastOwnerX = Double.NaN;
    private double lastOwnerY = Double.NaN;
    private double lastOwnerZ = Double.NaN;

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
        resetPathTracking();
        drake.setAccelerating(false);
    }

    @Override
    public void stop() {
        drake.getNavigation().stop();
        drake.setAccelerating(false);
        resetPathTracking();
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
            resetPathTracking();
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

        double speed = shouldRun ? 1.35D : 0.85D;
        updateGroundPath(owner, speed, distance, shouldRun);
    }

    private void updateGroundPath(LivingEntity owner, double speed, double distance, boolean running) {
        if (pathRecalcCooldown > 0) {
            pathRecalcCooldown--;
        }

        boolean ownerMoved = ownerMovedSignificantly(owner);
        boolean navIdle = drake.getNavigation().isDone() || !drake.getNavigation().isInProgress();

        if (navIdle || ownerMoved || pathRecalcCooldown <= 0) {
            // Boost speed in water for amphibious movement
            double effectiveSpeed = drake.isInWater() ? speed * 1.3D : speed;
            if (!drake.getNavigation().moveTo(owner, effectiveSpeed)) {
                drake.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), effectiveSpeed);
            }
            rememberOwnerPosition(owner);
            pathRecalcCooldown = computeRepathCooldown(distance, running);
        }
    }

    private int computeRepathCooldown(double distance, boolean running) {
        int base = (int) Math.ceil(distance * (running ? 0.4 : 0.55));
        return Mth.clamp(base, running ? 4 : 6, running ? 16 : 24);
    }

    private void rememberOwnerPosition(LivingEntity owner) {
        this.lastOwnerX = owner.getX();
        this.lastOwnerY = owner.getY();
        this.lastOwnerZ = owner.getZ();
    }

    private boolean ownerMovedSignificantly(LivingEntity owner) {
        if (Double.isNaN(lastOwnerX)) {
            return true;
        }
        double dx = owner.getX() - this.lastOwnerX;
        double dy = owner.getY() - this.lastOwnerY;
        double dz = owner.getZ() - this.lastOwnerZ;
        return dx * dx + dy * dy + dz * dz > 1.0D;
    }

    private void resetPathTracking() {
        this.pathRecalcCooldown = 0;
        this.lastOwnerX = Double.NaN;
        this.lastOwnerY = Double.NaN;
        this.lastOwnerZ = Double.NaN;
    }
}
