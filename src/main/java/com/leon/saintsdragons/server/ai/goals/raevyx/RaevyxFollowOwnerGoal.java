package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Features smart flight triggering, performance optimizations, and realistic distances... maybe, cuz if the player keeps walking away from Dink, he'd move incrementally, not walk to his owner in an instant.
 */

public class RaevyxFollowOwnerGoal extends Goal {
    private final Raevyx wyvern;

    // Distance constants - tuned for better behavior
    private static final double START_FOLLOW_DIST = 15.0;
    private static final double STOP_FOLLOW_DIST = 10.0; // Increased slightly to prevent constant state changes
    private static final double TELEPORT_DIST = 2000.0;
    private static final double RUN_DIST = 25.0;
    private static final double FLIGHT_TRIGGER_DIST = 30.0;
    private static final double FLIGHT_HEIGHT_DIFF = 8.0;
    private static final double LANDING_DISTANCE = 12.0; // Distance at which to start landing sequence
    private static final double HOVER_HEIGHT = 3.0; // Height above owner when hovering

    // Performance optimization - don't re-path constantly
    private int pathRecalcCooldown = 0;
    private double lastOwnerX = Double.NaN;
    private double lastOwnerY = Double.NaN;
    private double lastOwnerZ = Double.NaN;

    public RaevyxFollowOwnerGoal(Raevyx wyvern) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Basic requirements

        if (!wyvern.isTame() || wyvern.isOrderedToSit() || wyvern.isSleepLocked()) {
            return false;
        }

        // Never follow while actively targeting an enemy
        if (wyvern.getTarget() != null && wyvern.getTarget().isAlive()) {
            return false;
        }

        LivingEntity owner = wyvern.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        // Must be in same dimension
        if (owner.level() != wyvern.level()) {
            return false;
        }

        // Only follow if owner is far enough away
        double ownerDist = wyvern.getCachedDistanceToOwner();
        return ownerDist > START_FOLLOW_DIST * START_FOLLOW_DIST;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity owner = wyvern.getOwner();
        if (owner == null || !owner.isAlive() || wyvern.isOrderedToSit() || wyvern.isSleepLocked()) {
            return false;
        }

        // Suspend following while fighting
        if (wyvern.getTarget() != null && wyvern.getTarget().isAlive()) {
            return false;
        }

        if (owner.level() != wyvern.level()) {
            return false;
        }

        // Keep following until we're close enough
        double dist = wyvern.distanceToSqr(owner);
        return dist > STOP_FOLLOW_DIST * STOP_FOLLOW_DIST;
    }

    @Override
    public void start() {
        // Reset tracking
        resetPathTracking();

        // Don't immediately fly - let tick() decide based on conditions
        LivingEntity owner = wyvern.getOwner();
        double dist = owner != null ? Math.sqrt(wyvern.distanceToSqr(owner)) : -1;
        
    }

    @Override
    public void tick() {
        LivingEntity owner = wyvern.getOwner();
        if (owner == null) return;

        double distance = wyvern.distanceTo(owner);

        // Emergency teleport if owner gets stupidly far away
        if (distance > TELEPORT_DIST) {
            wyvern.teleportTo(owner.getX(), owner.getY() + 3, owner.getZ());
            // transition to flying
            wyvern.setFlying(true);
            wyvern.setTakeoff(false);
            wyvern.setLanding(false);
            wyvern.setHovering(false);
            resetPathTracking();
            

            return;
        }

        // Always look at owner while following
        wyvern.getLookControl().setLookAt(owner, 10.0f, 10.0f);

        // Smart flight decision making
        boolean shouldFly = shouldTriggerFlight(owner, distance);

        // Handle flight state changes
        if (shouldFly && !wyvern.isFlying()) {
            // Take off to follow owner
            wyvern.setFlying(true);
            wyvern.setTakeoff(true);
            wyvern.setLanding(false);
            wyvern.setHovering(false);
            resetPathTracking();
            
        } else if (wyvern.isFlying() && distance < STOP_FOLLOW_DIST * 1.5) {
            // Start landing sequence when close enough to owner
            wyvern.setLanding(true);
            wyvern.setFlying(false);
            wyvern.setHovering(true);
            pathRecalcCooldown = 0;
            
        }

        // Movement logic
        if (wyvern.isFlying()) {
            handleFlightFollowing(owner);
        } else {
            handleGroundFollowing(owner, distance);
        }
    }

    /**
     * Handle following while flying
     */
    private void handleFlightFollowing(LivingEntity owner) {
        // Calculate target position slightly above and behind owner
        double targetY = owner.getY() + owner.getBbHeight() + HOVER_HEIGHT;

        // Get owner's look vector for positioning behind them
        Vec3 ownerLook = owner.getLookAngle();
        double offsetX = -ownerLook.x * 3.0; // Reduced from 4.0 for closer following
        double offsetZ = -ownerLook.z * 3.0;

        // Add slight vertical movement for more natural flight
        double verticalOffset = Math.sin(wyvern.tickCount * 0.2) * 0.3; // Subtle bobbing

        // Calculate target position with smoothing
        double targetX = owner.getX() + offsetX;
        double targetZ = owner.getZ() + offsetZ;

        // Only update position if we're not too close to prevent jitter
        double distanceToTarget = Math.sqrt(wyvern.distanceToSqr(targetX, targetY, targetZ));
        if (distanceToTarget > 1.0) {
            wyvern.getMoveControl().setWantedPosition(
                    targetX,
                    targetY + verticalOffset,
                    targetZ,
                    1.2 // Flight speed
            );
        } else {
            // Hover in place if we're close enough
            wyvern.getNavigation().stop();
        }
    }

    /**
     * Handle following on ground
     */
    private void handleGroundFollowing(LivingEntity owner, double distance) {
        // Check if we should stop moving
        if (distance <= STOP_FOLLOW_DIST) {
            // Only update state if we were moving before
            if (wyvern.getGroundMoveState() > 0) {
                wyvern.getNavigation().stop();
                wyvern.setRunning(false);
                wyvern.setGroundMoveStateFromAI(0);
                
            }
            pathRecalcCooldown = 0;
            return;
        }

        // Determine movement style based on distance
        boolean shouldRun = distance > RUN_DIST;
        wyvern.setRunning(shouldRun);

        // Set appropriate animation state (0=idle, 1=walking, 2=running)
        int moveState = shouldRun ? 2 : 1;
        wyvern.setGroundMoveStateFromAI(moveState);

        // Adjust speed based on movement style and distance
        double baseSpeed = shouldRun ? 1.5 : 0.8;
        double speed = baseSpeed * (1.0 + (distance / 50.0));
        speed = Math.min(speed, shouldRun ? 2.5 : 1.2); // Cap max speed

        updateGroundPath(owner, speed, distance, shouldRun);

        // If stuck, try to jump or find alternative path
        if (wyvern.getNavigation().isStuck()) {
            wyvern.getJumpControl().jump();
            wyvern.getNavigation().stop();
            pathRecalcCooldown = 0; // Force repath next tick
        }
    }

    /**
     * Determine if wyvern should take flight to follow owner
     * Uses Ice and Fire's logic for smarter flight decisions
     */
    private boolean shouldTriggerFlight(LivingEntity owner, double distance) {
        // If already flying, continue flying until we're close to landing
        if (wyvern.isFlying()) {
            // Only stop flying if we're close to the owner and not too high up
            return !(distance < LANDING_DISTANCE && (owner.getY() - wyvern.getY()) < FLIGHT_HEIGHT_DIFF);
        }

        // Don't take off if we can't fly or are already hovering
        if (wyvern.isHovering() || !canTriggerFlight()) {
            return false;
        }

        // Don't take off if we're very close to the owner
        if (distance < STOP_FOLLOW_DIST * 1.5) {
            return false;
        }

        // Fly if owner is far away OR significantly higher up
        boolean farAway = distance > FLIGHT_TRIGGER_DIST;
        boolean ownerAbove = (owner.getY() - wyvern.getY()) > FLIGHT_HEIGHT_DIFF;

        // Check more frequently when we should be flying
        return farAway || ownerAbove;
    }

    /**
     * Check if wyvern is allowed to take flight
     * Uses existing wyvern flight requirements
     */
    private boolean canTriggerFlight() {
        return !wyvern.isOrderedToSit() &&
                !wyvern.isBaby() &&
                (wyvern.onGround() || wyvern.isInWater()) &&
                wyvern.getPassengers().isEmpty() &&
                wyvern.getControllingPassenger() == null &&
                !wyvern.isPassenger() &&
                wyvern.getActiveAbility() == null; // Don't interrupt abilities
    }

    @Override
    public void stop() {
        wyvern.setRunning(false);
        wyvern.getNavigation().stop();
        wyvern.setGroundMoveStateFromAI(0);
        resetPathTracking();
    }

    private void updateGroundPath(LivingEntity owner, double speed, double distance, boolean running) {
        if (pathRecalcCooldown > 0) {
            pathRecalcCooldown--;
        }

        boolean ownerMoved = ownerMovedSignificantly(owner);
        boolean navIdle = wyvern.getNavigation().isDone() || !wyvern.getNavigation().isInProgress();

        if (navIdle || ownerMoved || pathRecalcCooldown <= 0) {
            if (!wyvern.getNavigation().moveTo(owner, speed)) {
                wyvern.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), speed);
            }
            rememberOwnerPosition(owner);
            pathRecalcCooldown = computeRepathCooldown(distance, running);
        }
    }

    private int computeRepathCooldown(double distance, boolean running) {
        int base = (int) Math.ceil(distance * (running ? 0.3 : 0.45));
        return Mth.clamp(base, running ? 4 : 6, running ? 18 : 24);
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
