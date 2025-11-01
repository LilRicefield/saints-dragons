package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Follow-owner behaviour tuned for Amphitheres.
 * Mirrors the Lightning dragon implementation but with glider-friendly constants.
 */
public class CindervaneFollowOwnerGoal extends Goal {
    private final Cindervane amphithere;

    private static final double START_FOLLOW_DIST = 8.8;
    private static final double STOP_FOLLOW_DIST = 8.0;
    private static final double TELEPORT_DIST = 500.0;
    private static final double RUN_DIST = 10.0;
    private static final double FLIGHT_TRIGGER_DIST = 24.0;
    private static final double FLIGHT_HEIGHT_DIFF = 6.0;
    private static final double LANDING_DISTANCE = 10.0;
    private static final double HOVER_HEIGHT = 2.5;

    private int pathRecalcCooldown;
    private double lastOwnerX = Double.NaN;
    private double lastOwnerY = Double.NaN;
    private double lastOwnerZ = Double.NaN;

    public CindervaneFollowOwnerGoal(Cindervane dragon) {
        this.amphithere = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!amphithere.isTame() || amphithere.isOrderedToSit()) {
            return false;
        }

        // Don't start following while sitting down (but standing up is OK)
        if (amphithere.isSittingDownAnimation()) {
            return false;
        }

        // Only disable for Sit command (1), allow for both Follow (0) and Wander (2)
        if (amphithere.getCommand() == 1) {
            return false;
        }

        if (amphithere.getTarget() != null && amphithere.getTarget().isAlive()) {
            return false;
        }

        LivingEntity owner = amphithere.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        if (owner.level() != amphithere.level()) {
            return false;
        }

        double distSq = amphithere.distanceToSqr(owner);
        return distSq > START_FOLLOW_DIST * START_FOLLOW_DIST;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity owner = amphithere.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        // Stop following while sitting down (but standing up is OK)
        // Only disable for Sit command (1), allow for both Follow (0) and Wander (2)
        if (amphithere.isOrderedToSit() || amphithere.isSittingDownAnimation() || amphithere.getCommand() == 1) {
            return false;
        }

        if (amphithere.getTarget() != null && amphithere.getTarget().isAlive()) {
            return false;
        }

        if (owner.level() != amphithere.level()) {
            return false;
        }

        double distSq = amphithere.distanceToSqr(owner);
        return distSq > STOP_FOLLOW_DIST * STOP_FOLLOW_DIST;
    }

    @Override
    public void start() {
        resetPathTracking();
    }

    @Override
    public void stop() {
        amphithere.setRunning(false);
        amphithere.getNavigation().stop();
        amphithere.setGroundMoveStateFromAI(0);
        resetPathTracking();
    }

    @Override
    public void tick() {
        LivingEntity owner = amphithere.getOwner();
        if (owner == null) {
            return;
        }

        double distance = amphithere.distanceTo(owner);

        if (distance > TELEPORT_DIST) {
            amphithere.teleportTo(owner.getX(), owner.getY() + 2.0, owner.getZ());
            amphithere.setFlying(true);
            amphithere.setTakeoff(false);
            amphithere.setLanding(false);
            amphithere.setHovering(false);
            resetPathTracking();
            return;
        }

        amphithere.getLookControl().setLookAt(owner, 10.0f, 10.0f);

        boolean shouldFly = shouldTriggerFlight(owner, distance);
        if (shouldFly && !amphithere.isFlying()) {
            amphithere.setFlying(true);
            amphithere.setTakeoff(true);
            amphithere.setLanding(false);
            amphithere.setHovering(false);
            resetPathTracking();
        } else if (amphithere.isFlying() && distance < STOP_FOLLOW_DIST * 1.5) {
            // Properly land the dragon instead of making it hover in mid-air
            amphithere.setLanding(true);
            amphithere.setFlying(false);
            amphithere.setHovering(false);
            amphithere.setTakeoff(false);
            pathRecalcCooldown = 0;
        }

        if (amphithere.isFlying()) {
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
        double verticalOffset = Math.sin(amphithere.tickCount * 0.2) * 0.25;

        double targetX = owner.getX() + offsetX;
        double targetZ = owner.getZ() + offsetZ;

        double distanceToTargetSq = amphithere.distanceToSqr(targetX, targetY, targetZ);
        if (distanceToTargetSq > 1.0) {
            amphithere.getMoveControl().setWantedPosition(
                    targetX,
                    targetY + verticalOffset,
                    targetZ,
                    amphithere.getFlightSpeed()
            );
        } else {
            amphithere.getNavigation().stop();
        }
    }

    private void handleGroundFollowing(LivingEntity owner, double distance) {
        // Check if we should stop moving
        if (distance <= STOP_FOLLOW_DIST) {
            // Only update state if we were moving before
            if (amphithere.getGroundMoveState() > 0) {
                amphithere.getNavigation().stop();
                amphithere.setRunning(false);
                amphithere.setGroundMoveStateFromAI(0);
            }
            pathRecalcCooldown = 0;
            return;
        }

        boolean shouldRun = distance > RUN_DIST;
        amphithere.setRunning(shouldRun);

        // Set appropriate animation state (0=idle, 1=walking, 2=running)
        int moveState = shouldRun ? 2 : 1;
        amphithere.setGroundMoveStateFromAI(moveState);

        // Adjust speed based on movement style and distance
        double baseSpeed = shouldRun ? 1.1 : 0.7;
        double speed = baseSpeed * (1.0 + (distance / 40.0));
        speed = Math.min(speed, shouldRun ? 1.6 : 1.0); // Cap max speed

        updateGroundPath(owner, speed, distance, shouldRun);

        // Handle getting stuck
        if (amphithere.getNavigation().isStuck()) {
            amphithere.getJumpControl().jump();
            amphithere.getNavigation().stop();
            pathRecalcCooldown = 0;
        }
    }

    private boolean shouldTriggerFlight(LivingEntity owner, double distance) {
        if (amphithere.isFlying()) {
            return !(distance < LANDING_DISTANCE && (owner.getY() - amphithere.getY()) < FLIGHT_HEIGHT_DIFF);
        }

        if (amphithere.isHovering() || !canTriggerFlight()) {
            return false;
        }

        if (distance < STOP_FOLLOW_DIST * 1.5) {
            return false;
        }

        boolean farAway = distance > FLIGHT_TRIGGER_DIST;
        boolean ownerAbove = (owner.getY() - amphithere.getY()) > FLIGHT_HEIGHT_DIFF;
        return farAway || ownerAbove;
    }

    private boolean canTriggerFlight() {
        return !amphithere.isOrderedToSit()
                && !amphithere.isBaby()
                && (amphithere.onGround() || amphithere.isInWater())
                && amphithere.getPassengers().isEmpty()
                && amphithere.getControllingPassenger() == null
                && !amphithere.isPassenger();
    }

    private void updateGroundPath(LivingEntity owner, double speed, double distance, boolean running) {
        if (pathRecalcCooldown > 0) {
            pathRecalcCooldown--;
        }

        boolean ownerMoved = ownerMovedSignificantly(owner);
        boolean navIdle = amphithere.getNavigation().isDone() || !amphithere.getNavigation().isInProgress();

        if (navIdle || ownerMoved || pathRecalcCooldown <= 0) {
            if (!amphithere.getNavigation().moveTo(owner, speed)) {
                amphithere.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), speed);
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
