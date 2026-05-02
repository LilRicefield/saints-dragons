package com.leon.saintsdragons.server.ai.goals.stegonaut;

import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class StegonautFollowOwnerGoal extends Goal {
    private final Stegonaut drake;
    private static final double START_FOLLOW_DIST = 12.0;
    private static final double STOP_FOLLOW_DIST = 8.0;
    private static final double TELEPORT_DIST = 32;
    private int pathRecalcCooldown = 0;
    private double lastOwnerX = Double.NaN;
    private double lastOwnerY = Double.NaN;
    private double lastOwnerZ = Double.NaN;

    public StegonautFollowOwnerGoal(Stegonaut drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (drake.isTame() && drake.getCommand() != 0) {
            return false;
        }
        if (!drake.isTame() || drake.isOrderedToSit()) {
            return false;
        }
        if (drake.isInLove()) {
            return false;
        }

        LivingEntity target = drake.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }

        LivingEntity owner = drake.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        if (owner.level() != drake.level()) {
            return false;
        }
        double ownerDist = drake.distanceToSqr(owner);
        return ownerDist > START_FOLLOW_DIST * START_FOLLOW_DIST;
    }

    @Override
    public boolean canContinueToUse() {
        if (drake.isTame() && drake.getCommand() != 0) {
            return false;
        }

        LivingEntity owner = drake.getOwner();
        if (owner == null || !owner.isAlive() || drake.isOrderedToSit()) {
            return false;
        }

        if (drake.isInLove()) {
            return false;
        }

        LivingEntity target = drake.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }

        if (owner.level() != drake.level()) {
            return false;
        }

        double dist = drake.distanceToSqr(owner);
        return dist > STOP_FOLLOW_DIST * STOP_FOLLOW_DIST;
    }

    @Override
    public void start() {
        resetPathTracking();
    }

    @Override
    public void tick() {
        LivingEntity owner = drake.getOwner();
        if (owner == null) return;

        double distance = drake.distanceTo(owner);

        if (distance > TELEPORT_DIST) {
            drake.teleportTo(owner.getX(), owner.getY() + 1, owner.getZ());
            resetPathTracking();
            return;
        }
        drake.getLookControl().setLookAt(owner, 10.0f, 10.0f);
        handleGroundFollowing(owner, distance);
    }


    private void handleGroundFollowing(LivingEntity owner, double distance) {
        if (distance <= STOP_FOLLOW_DIST) {
            drake.getNavigation().stop();
            pathRecalcCooldown = 0;
            return;
        }

        double baseSpeed = 0.8;
        double speed = baseSpeed * (1.0 + (distance / 100.0));
        speed = Math.min(speed, 1.0); // Cap at walking speed
        updateGroundPath(owner, speed, distance);
        if (drake.getNavigation().isStuck()) {
            drake.getJumpControl().jump();
            drake.getNavigation().stop();
            pathRecalcCooldown = 0;
        }
    }

    @Override
    public void stop() {
        drake.getNavigation().stop();
        resetPathTracking();
    }

    private void updateGroundPath(LivingEntity owner, double speed, double distance) {
        if (pathRecalcCooldown > 0) {
            pathRecalcCooldown--;
        }

        boolean ownerMoved = ownerMovedSignificantly(owner);
        boolean navIdle = drake.getNavigation().isDone() || !drake.getNavigation().isInProgress();

        if (navIdle || ownerMoved || pathRecalcCooldown <= 0) {
            if (!drake.getNavigation().moveTo(owner, speed)) {
                drake.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), speed);
            }
            rememberOwnerPosition(owner);
            pathRecalcCooldown = computeRepathCooldown(distance);
        }
    }

    private int computeRepathCooldown(double distance) {
        int base = (int) Math.ceil(distance * 0.45);
        return Mth.clamp(base, 6, 24);
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

    private void rememberOwnerPosition(LivingEntity owner) {
        this.lastOwnerX = owner.getX();
        this.lastOwnerY = owner.getY();
        this.lastOwnerZ = owner.getZ();
    }

    private void resetPathTracking() {
        this.pathRecalcCooldown = 0;
        this.lastOwnerX = Double.NaN;
        this.lastOwnerY = Double.NaN;
        this.lastOwnerZ = Double.NaN;
    }
}