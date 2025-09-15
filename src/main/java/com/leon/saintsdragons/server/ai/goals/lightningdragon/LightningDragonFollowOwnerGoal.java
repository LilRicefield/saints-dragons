package com.leon.saintsdragons.server.ai.goals.lightningdragon;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Features smart flight triggering, performance optimizations, and realistic distances... maybe, cuz if the player keeps walking away from Dink, he'd move incrementally, not walk to his owner in an instant.
 */

public class LightningDragonFollowOwnerGoal extends Goal {
    private final LightningDragonEntity dragon;

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
    private BlockPos previousOwnerPos;
    private int repathCooldown = 0;

    public LightningDragonFollowOwnerGoal(LightningDragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Basic requirements

        if (!dragon.isTame() || dragon.isOrderedToSit()) {
            return false;
        }

        // Never follow while actively targeting an enemy
        if (dragon.getTarget() != null && dragon.getTarget().isAlive()) {
            return false;
        }

        LivingEntity owner = dragon.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        // Must be in same dimension
        if (owner.level() != dragon.level()) {
            return false;
        }

        // Only follow if owner is far enough away
        double ownerDist = dragon.getCachedDistanceToOwner();
        return ownerDist > START_FOLLOW_DIST * START_FOLLOW_DIST;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity owner = dragon.getOwner();
        if (owner == null || !owner.isAlive() || dragon.isOrderedToSit()) {
            return false;
        }

        // Suspend following while fighting
        if (dragon.getTarget() != null && dragon.getTarget().isAlive()) {
            return false;
        }

        if (owner.level() != dragon.level()) {
            return false;
        }

        // Keep following until we're close enough
        double dist = dragon.distanceToSqr(owner);
        return dist > STOP_FOLLOW_DIST * STOP_FOLLOW_DIST;
    }

    @Override
    public void start() {
        // Reset tracking
        previousOwnerPos = null;
        repathCooldown = 0;

        // Don't immediately fly - let tick() decide based on conditions
    }

    @Override
    public void tick() {
        LivingEntity owner = dragon.getOwner();
        if (owner == null) return;

        double distance = dragon.distanceTo(owner);

        // Emergency teleport if owner gets stupidly far away
        if (distance > TELEPORT_DIST) {
            dragon.teleportTo(owner.getX(), owner.getY() + 3, owner.getZ());
            // transition to flying
            dragon.setFlying(true);
            dragon.setTakeoff(false);
            dragon.setLanding(false);
            dragon.setHovering(false);

            return;
        }

        // Always look at owner while following
        dragon.getLookControl().setLookAt(owner, 10.0f, 10.0f);

        // Smart flight decision making
        boolean shouldFly = shouldTriggerFlight(owner, distance);

        // Handle flight state changes
        if (shouldFly && !dragon.isFlying()) {
            // Take off to follow owner
            dragon.setFlying(true);
            dragon.setTakeoff(true);
            dragon.setLanding(false);
            dragon.setHovering(false);
        } else if (dragon.isFlying() && distance < STOP_FOLLOW_DIST * 1.5) {
            // Start landing sequence when close enough to owner
            dragon.setLanding(true);
            dragon.setFlying(false);
            dragon.setHovering(true);
        }

        // Movement logic
        if (dragon.isFlying()) {
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
        double verticalOffset = Math.sin(dragon.tickCount * 0.2) * 0.3; // Subtle bobbing
        
        // Calculate target position with smoothing
        double targetX = owner.getX() + offsetX;
        double targetZ = owner.getZ() + offsetZ;
        
        // Only update position if we're not too close to prevent jitter
        double distanceToTarget = Math.sqrt(dragon.distanceToSqr(targetX, targetY, targetZ));
        if (distanceToTarget > 1.0) {
            dragon.getMoveControl().setWantedPosition(
                targetX,
                targetY + verticalOffset,
                targetZ,
                1.2 // Flight speed
            );
        } else {
            // Hover in place if we're close enough
            dragon.getNavigation().stop();
        }
    }

    /**
     * Handle following on ground
     */
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
            double baseSpeed = shouldRun ? 1.5 : 0.8;
            // Increase speed slightly based on distance to catch up faster when far away
            double speed = baseSpeed * (1.0 + (distance / 50.0));
            speed = Math.min(speed, shouldRun ? 2.5 : 1.2); // Cap max speed

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
        
        // If stuck, try to jump or find alternative path
        if (dragon.getNavigation().isStuck()) {
            dragon.getJumpControl().jump();
            dragon.getNavigation().stop();
            repathCooldown = 0; // Force repath next tick
        }
    }

    /**
     * Determine if dragon should take flight to follow owner
     * Uses Ice and Fire's logic for smarter flight decisions
     */
    private boolean shouldTriggerFlight(LivingEntity owner, double distance) {
        // If already flying, continue flying until we're close to landing
        if (dragon.isFlying()) {
            // Only stop flying if we're close to the owner and not too high up
            return !(distance < LANDING_DISTANCE && (owner.getY() - dragon.getY()) < FLIGHT_HEIGHT_DIFF);
        }
        
        // Don't take off if we can't fly or are already hovering
        if (dragon.isHovering() || !canTriggerFlight()) {
            return false;
        }

        // Don't take off if we're very close to the owner
        if (distance < STOP_FOLLOW_DIST * 1.5) {
            return false;
        }

        // Fly if owner is far away OR significantly higher up
        boolean farAway = distance > FLIGHT_TRIGGER_DIST;
        boolean ownerAbove = (owner.getY() - dragon.getY()) > FLIGHT_HEIGHT_DIFF;
        
        // Check more frequently when we should be flying
        if (farAway || ownerAbove) {
            return true;
        }
        
        return false;
    }

    /**
     * Check if dragon is allowed to take flight
     * Uses existing dragon flight requirements
     */
    private boolean canTriggerFlight() {
        return !dragon.isOrderedToSit() &&
                !dragon.isBaby() &&
                (dragon.onGround() || dragon.isInWater()) &&
                dragon.getPassengers().isEmpty() &&
                dragon.getControllingPassenger() == null &&
                !dragon.isPassenger() &&
                dragon.getActiveAbility() == null; // Don't interrupt abilities
    }

    @Override
    public void stop() {
        dragon.setRunning(false);
        dragon.getNavigation().stop();
        dragon.setGroundMoveStateFromAI(0);
        previousOwnerPos = null;
    }
}