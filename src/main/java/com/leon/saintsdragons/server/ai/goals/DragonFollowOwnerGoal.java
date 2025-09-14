package com.leon.saintsdragons.server.ai.goals;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Features smart flight triggering, performance optimizations, and realistic distances... maybe, cuz if the player keeps walking away from Dink, he'd move incrementally, not walk to his owner in an instant.
 */

public class DragonFollowOwnerGoal extends Goal {
    private final LightningDragonEntity dragon;

    // Distance constants - tuned for better behavior
    private static final double START_FOLLOW_DIST = 15.0;
    private static final double STOP_FOLLOW_DIST = 8.0;
    private static final double TELEPORT_DIST = 2000.0; // Way more realistic than 64 blocks
    private static final double RUN_DIST = 25.0;
    private static final double FLIGHT_TRIGGER_DIST = 30.0;
    private static final double FLIGHT_HEIGHT_DIFF = 8.0; // Fly if owner is way above

    // Performance optimization - don't re-path constantly
    private BlockPos previousOwnerPos;
    private int repathCooldown = 0;

    public DragonFollowOwnerGoal(LightningDragonEntity dragon) {
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

        if (shouldFly && !dragon.isFlying()) {
            // Take off to follow owner
            dragon.setFlying(true);
            dragon.setTakeoff(false);
            dragon.setLanding(false);
            dragon.setHovering(false);

        } else if (!shouldFly && dragon.isFlying() && dragon.onGround()) {
            // Land when we don't need to fly anymore
            dragon.setLanding(true);

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
        // Fly slightly above and behind owner
        double targetY = owner.getY() + owner.getBbHeight() + 3.0;

        // Add some offset so dragon doesn't crowd the owner
        Vec3 ownerLook = owner.getLookAngle();
        double offsetX = -ownerLook.x * 4.0; // Behind owner
        double offsetZ = -ownerLook.z * 4.0;

        dragon.getMoveControl().setWantedPosition(
                owner.getX() + offsetX,
                targetY,
                owner.getZ() + offsetZ,
                1.2 // Flight speed
        );
    }

    /**
     * Handle following on ground
     */
    private void handleGroundFollowing(LivingEntity owner, double distance) {
        // Stop if close enough to avoid crowding
        if (distance <= STOP_FOLLOW_DIST) {
            dragon.getNavigation().stop();
            dragon.setRunning(false);
            dragon.setGroundMoveStateFromAI(0);
            return;
        }

        // Run when far, walk when nearer
        boolean shouldRun = distance > RUN_DIST;
        dragon.setRunning(shouldRun);
        dragon.setGroundMoveStateFromAI(shouldRun ? 2 : 1);
        double speed = shouldRun ? 1.65 : 1.0; // slight bump for tighter chase

        // Repath logic: frequent when owner moving or path finished
        boolean navDone = dragon.getNavigation().isDone();
        boolean ownerMoved = previousOwnerPos == null || previousOwnerPos.distSqr(owner.blockPosition()) > 1; // >1 block
        boolean cooldownExpired = (repathCooldown-- <= 0);

        if (navDone || ownerMoved || cooldownExpired) {
            dragon.getNavigation().moveTo(owner, speed);
            previousOwnerPos = owner.blockPosition();
            repathCooldown = shouldRun ? 3 : 7; // lower cadence for snappier path updates
        }
    }

    /**
     * Determine if dragon should take flight to follow owner
     * Uses Ice and Fire's logic for smarter flight decisions
     */
    private boolean shouldTriggerFlight(LivingEntity owner, double distance) {
        // Don't fly if already flying or can't fly
        if (dragon.isFlying() || dragon.isHovering() || !canTriggerFlight()) {
            return false;
        }

        // Fly if owner is far away OR significantly higher up
        boolean farAway = distance > FLIGHT_TRIGGER_DIST;
        boolean ownerAbove = owner.getY() - dragon.getY() > FLIGHT_HEIGHT_DIFF;

        return farAway || ownerAbove;
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
        previousOwnerPos = null;

    }
}
