package com.leon.saintsdragons.server.ai.goals.amphithere;

import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Follow-owner behaviour tuned for Amphitheres.
 * Mirrors the Lightning dragon implementation but with glider-friendly constants.
 */
public class AmphithereFollowOwnerGoal extends Goal {
    private final AmphithereEntity dragon;

    private static final double START_FOLLOW_DIST = 12.0;
    private static final double STOP_FOLLOW_DIST = 8.0;
    private static final double TELEPORT_DIST = 2000.0;
    private static final double RUN_DIST = 18.0;
    private static final double FLIGHT_TRIGGER_DIST = 24.0;
    private static final double FLIGHT_HEIGHT_DIFF = 6.0;
    private static final double LANDING_DISTANCE = 10.0;
    private static final double HOVER_HEIGHT = 2.5;

    private int pathRecalcCooldown;
    private double lastOwnerX = Double.NaN;
    private double lastOwnerY = Double.NaN;
    private double lastOwnerZ = Double.NaN;

    public AmphithereFollowOwnerGoal(AmphithereEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!dragon.isTame() || dragon.isOrderedToSit()) {
            return false;
        }

        if (dragon.getCommand() != 0) {
            return false;
        }

        if (dragon.getTarget() != null && dragon.getTarget().isAlive()) {
            return false;
        }

        LivingEntity owner = dragon.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        if (owner.level() != dragon.level()) {
            return false;
        }

        double distSq = dragon.distanceToSqr(owner);
        return distSq > START_FOLLOW_DIST * START_FOLLOW_DIST;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity owner = dragon.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        if (dragon.isOrderedToSit() || dragon.getCommand() != 0) {
            return false;
        }

        if (dragon.getTarget() != null && dragon.getTarget().isAlive()) {
            return false;
        }

        if (owner.level() != dragon.level()) {
            return false;
        }

        double distSq = dragon.distanceToSqr(owner);
        return distSq > STOP_FOLLOW_DIST * STOP_FOLLOW_DIST;
    }

    @Override
    public void start() {
        resetPathTracking();
    }

    @Override
    public void stop() {
        dragon.setRunning(false);
        dragon.getNavigation().stop();
        dragon.setGroundMoveStateFromAI(0);
        resetPathTracking();
    }

    @Override
    public void tick() {
        LivingEntity owner = dragon.getOwner();
        if (owner == null) {
            return;
        }

        double distance = dragon.distanceTo(owner);

        if (distance > TELEPORT_DIST) {
            dragon.teleportTo(owner.getX(), owner.getY() + 2.0, owner.getZ());
            dragon.setFlying(true);
            dragon.setTakeoff(false);
            dragon.setLanding(false);
            dragon.setHovering(false);
            resetPathTracking();
            return;
        }

        dragon.getLookControl().setLookAt(owner, 10.0f, 10.0f);

        boolean shouldFly = shouldTriggerFlight(owner, distance);
        if (shouldFly && !dragon.isFlying()) {
            dragon.setFlying(true);
            dragon.setTakeoff(true);
            dragon.setLanding(false);
            dragon.setHovering(false);
            resetPathTracking();
        } else if (dragon.isFlying() && distance < STOP_FOLLOW_DIST * 1.5) {
            // Properly land the dragon instead of making it hover in mid-air
            dragon.setLanding(true);
            dragon.setFlying(false);
            dragon.setHovering(false);
            dragon.setTakeoff(false);
            pathRecalcCooldown = 0;
        }

        if (dragon.isFlying()) {
            handleFlightFollowing(owner);
        } else {
            handleGroundFollowing(owner, distance);
        }
    }

    private void handleFlightFollowing(LivingEntity owner) {
        double targetY = owner.getY() + owner.getBbHeight() + HOVER_HEIGHT;
        Vec3 ownerLook = owner.getLookAngle();
        double offsetX = -ownerLook.x * 2.5;
        double offsetZ = -ownerLook.z * 2.5;
        double verticalOffset = Math.sin(dragon.tickCount * 0.2) * 0.25;

        double targetX = owner.getX() + offsetX;
        double targetZ = owner.getZ() + offsetZ;

        double distanceToTargetSq = dragon.distanceToSqr(targetX, targetY, targetZ);
        if (distanceToTargetSq > 1.0) {
            dragon.getMoveControl().setWantedPosition(
                    targetX,
                    targetY + verticalOffset,
                    targetZ,
                    dragon.getFlightSpeed()
            );
        } else {
            dragon.getNavigation().stop();
        }
    }

    private void handleGroundFollowing(LivingEntity owner, double distance) {
        // Check if we should stop moving
        if (distance <= STOP_FOLLOW_DIST) {
            // Only update state if we were moving before
            if (dragon.getGroundMoveState() > 0) {
                dragon.getNavigation().stop();
                dragon.setRunning(false);
                dragon.setGroundMoveStateFromAI(0);
            }
            pathRecalcCooldown = 0;
            return;
        }

        boolean shouldRun = distance > RUN_DIST;
        dragon.setRunning(shouldRun);

        // Set appropriate animation state (0=idle, 1=walking, 2=running)
        int moveState = shouldRun ? 2 : 1;
        dragon.setGroundMoveStateFromAI(moveState);

        // Adjust speed based on movement style and distance
        double baseSpeed = shouldRun ? 1.1 : 0.7;
        double speed = baseSpeed * (1.0 + (distance / 40.0));
        speed = Math.min(speed, shouldRun ? 1.6 : 1.0); // Cap max speed

        updateGroundPath(owner, speed, distance, shouldRun);

        // Handle getting stuck
        if (dragon.getNavigation().isStuck()) {
            dragon.getJumpControl().jump();
            dragon.getNavigation().stop();
            pathRecalcCooldown = 0;
        }
    }

    private boolean shouldTriggerFlight(LivingEntity owner, double distance) {
        if (dragon.isFlying()) {
            return !(distance < LANDING_DISTANCE && (owner.getY() - dragon.getY()) < FLIGHT_HEIGHT_DIFF);
        }

        if (dragon.isHovering() || !canTriggerFlight()) {
            return false;
        }

        if (distance < STOP_FOLLOW_DIST * 1.5) {
            return false;
        }

        boolean farAway = distance > FLIGHT_TRIGGER_DIST;
        boolean ownerAbove = (owner.getY() - dragon.getY()) > FLIGHT_HEIGHT_DIFF;
        return farAway || ownerAbove;
    }

    private boolean canTriggerFlight() {
        return !dragon.isOrderedToSit()
                && !dragon.isBaby()
                && (dragon.onGround() || dragon.isInWater())
                && dragon.getPassengers().isEmpty()
                && dragon.getControllingPassenger() == null
                && !dragon.isPassenger();
    }

    private void updateGroundPath(LivingEntity owner, double speed, double distance, boolean running) {
        if (pathRecalcCooldown > 0) {
            pathRecalcCooldown--;
        }

        boolean ownerMoved = ownerMovedSignificantly(owner);
        boolean navIdle = dragon.getNavigation().isDone() || !dragon.getNavigation().isInProgress();

        if (navIdle || ownerMoved || pathRecalcCooldown <= 0) {
            if (!dragon.getNavigation().moveTo(owner, speed)) {
                dragon.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), speed);
            }
            rememberOwnerPosition(owner);
            pathRecalcCooldown = computeRepathCooldown(distance, running);
        }
    }

    private int computeRepathCooldown(double distance, boolean running) {
        int base = (int) Math.ceil(distance * (running ? 0.35 : 0.45));
        return Mth.clamp(base, running ? 4 : 6, running ? 16 : 22);
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
        return dx * dx + dy * dy + dz * dz > 1.2D;
    }

    private void resetPathTracking() {
        this.pathRecalcCooldown = 0;
        this.lastOwnerX = Double.NaN;
        this.lastOwnerY = Double.NaN;
        this.lastOwnerZ = Double.NaN;
    }
}
