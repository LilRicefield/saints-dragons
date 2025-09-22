package com.leon.saintsdragons.server.ai.goals.amphithere;

import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import net.minecraft.core.BlockPos;
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

    private BlockPos previousOwnerPos;
    private int repathCooldown;

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
        previousOwnerPos = null;
        repathCooldown = 0;
    }

    @Override
    public void stop() {
        dragon.setRunning(false);
        dragon.getNavigation().stop();
        dragon.setGroundMoveStateFromAI(0);
        previousOwnerPos = null;
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
            return;
        }

        dragon.getLookControl().setLookAt(owner, 10.0f, 10.0f);

        boolean shouldFly = shouldTriggerFlight(owner, distance);
        if (shouldFly && !dragon.isFlying()) {
            dragon.setFlying(true);
            dragon.setTakeoff(true);
            dragon.setLanding(false);
            dragon.setHovering(false);
        } else if (dragon.isFlying() && distance < STOP_FOLLOW_DIST * 1.5) {
            // Properly land the dragon instead of making it hover in mid-air
            dragon.setLanding(true);
            dragon.setFlying(false);
            dragon.setHovering(false);
            dragon.setTakeoff(false);
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
            return;
        }

        // Only update movement state if we're not currently moving or need to repath
        if (dragon.getNavigation().isDone() || !dragon.getNavigation().isInProgress()) {
            // Determine movement style based on distance
            boolean shouldRun = distance > RUN_DIST;
            dragon.setRunning(shouldRun);

            // Set appropriate animation state (0=idle, 1=walking, 2=running)
            int moveState = shouldRun ? 2 : 1;
            dragon.setGroundMoveStateFromAI(moveState);

            // Adjust speed based on movement style and distance
            double baseSpeed = shouldRun ? 1.1 : 0.7;
            // Increase speed slightly based on distance to catch up faster when far away
            double speed = baseSpeed * (1.0 + (distance / 40.0));
            speed = Math.min(speed, shouldRun ? 1.6 : 1.0); // Cap max speed

            // Check if we need to recalculate the path
            boolean navDone = dragon.getNavigation().isDone();
            boolean ownerMoved = previousOwnerPos == null || previousOwnerPos.distSqr(owner.blockPosition()) > 1;
            boolean cooldownExpired = (repathCooldown-- <= 0);

            if (navDone || ownerMoved || cooldownExpired) {
                // Try to move directly to the owner if path is clear
                if (distance < 16.0 && dragon.getNavigation().createPath(owner, 0) != null) {
                    dragon.getNavigation().moveTo(owner, speed);
                } else {
                    // If path is blocked or far away, try to get closer first
                    dragon.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), speed);
                }
                previousOwnerPos = owner.blockPosition();
                repathCooldown = 2; // More frequent updates for better following
            }
        }

        // Handle getting stuck
        if (dragon.getNavigation().isStuck()) {
            dragon.getJumpControl().jump();
            dragon.getNavigation().stop();
            repathCooldown = 0;
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
}