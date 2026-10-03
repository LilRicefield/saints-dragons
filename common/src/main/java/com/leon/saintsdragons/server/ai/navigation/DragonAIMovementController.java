package com.leon.saintsdragons.server.ai.navigation;

import com.leon.saintsdragons.server.ai.navigation.async.DragonFlightRequest;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementOwnership;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncDragonPathfinder;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.ai.navigation.async.DragonLandingSites;
import com.leon.saintsdragons.server.ai.navigation.async.DragonFlightSpace;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Future;

public class DragonAIMovementController {
    private final DragonMovementOwnership brainMovement =
            new DragonMovementOwnership();

    public DragonMovementOwnership brainMovement() {
        return brainMovement;
    }
    private static final int GROUND_PATH_FAILURE_RETRY_TICKS = 20;
    private static final int REPEATED_GROUND_PATH_FAILURE_THRESHOLD = 3;
    private static final int FAILED_ROUTE_DETOUR_ALLOWANCE = 40;
    private static final int LANDING_PLAN_FAILURE_RETRY_TICKS = 100;
    private static final float WATER_TURN_SPEED = 8.0F;

    private final RideableDragonBase dragon;
    private final DragonFlightSpace flightSpace;
    private @Nullable QueuedWaypoint currentWaypoint;
    private long movementCommandGeneration;
    private GroundPathState groundPathState = GroundPathState.IDLE;
    private long groundPathRequestGeneration;
    private @Nullable Future<?> groundPathRequest;
    private int groundPathFailureRetryTicks;
    private int groundRepathTicks;
    private @Nullable Vec3 groundSearchTarget;
    private long groundFailureSequence;
    private @Nullable Vec3 lastFailedGroundTarget;
    private int consecutiveGroundPathFailures;
    private @Nullable Vec3 groundPathFailureOrigin;
    private final GroundRouteProgress groundRouteProgress = new GroundRouteProgress();
    private boolean ignoreInheritedGroundNavigationStuck;
    private String groundPathDebugReason = "idle";
    private int landingPlanRetryTicks;
    private String landingPlanDebugReason = "not-requested";
    private int lastWaterControllerTick = Integer.MIN_VALUE;

    public DragonAIMovementController(RideableDragonBase dragon) {
        this.dragon = dragon;
        this.flightSpace = new DragonFlightSpace(dragon);
    }

    public DragonFlightSpace flightSpace() {
        return flightSpace;
    }

    public void serverTick() {
        if (dragon.level().isClientSide) {
            return;
        }
        if (dragon.isVehicle() || dragon.isPassenger()) {
            clearAllWaypoints();
            return;
        }
        if (groundPathFailureRetryTicks > 0) {
            groundPathFailureRetryTicks--;
        }
        if (groundRepathTicks > 0) {
            groundRepathTicks--;
        }
        if (landingPlanRetryTicks > 0) {
            landingPlanRetryTicks--;
        }
        if (currentWaypoint != null && currentWaypoint.mode() == MovementMode.LANDING) {
            hasActiveLandingTransition(); // Retire failed routes and resume ordinary flight before retrying.
        }
        if (currentWaypoint != null
                && !currentWaypoint.mode().usesWater()
                && shouldUseWaterMovement()
                && !handoffCurrentWaypointToWater()) {
            return;
        }
        if (currentWaypoint != null && currentWaypoint.mode().usesWater()) {
            if (!shouldUseWaterMovement()) {
                dragon.getAiSwimController().stop();
                currentWaypoint = null;
                return;
            }
            if (hasArrived()) {
                dragon.getAiSwimController().stop();
                currentWaypoint = null;
                return;
            }
            tickWaterController();
            return;
        }
        if (ignoreInheritedGroundNavigationStuck && !dragon.getNavigation().isStuck()) {
            ignoreInheritedGroundNavigationStuck = false;
        }
        if (groundPathState == GroundPathState.WAITING && groundPathFailureRetryTicks == 0
                && currentWaypoint != null && canUseGroundNavigation()) {
            // The request already owns its command generation; resuming it is not a new command.
            tryStartGroundPath(currentWaypoint);
        }
        if (groundPathState == GroundPathState.FOLLOWING && hasReachedGroundWaypoint()) {
            completeGroundArrival();
        } else if (groundPathState == GroundPathState.FOLLOWING
                && !ignoreInheritedGroundNavigationStuck
                && dragon.getNavigation().isStuck()) {
            recordGroundPathFailure(
                    currentWaypoint != null ? currentWaypoint.target() : null,
                    "navigation-stuck"
            );
        } else if (groundPathState == GroundPathState.FOLLOWING && dragon.getNavigation().isDone()) {
            if (currentWaypoint != null
                    && currentWaypoint.mode() == MovementMode.PROGRESSIVE_GROUND) {
                completeProgressiveGroundSegment();
            } else {
                recordGroundPathFailure(
                        currentWaypoint != null ? currentWaypoint.target() : null,
                        "navigation-finished-short"
                );
            }
        } else if (currentWaypoint != null && hasArrived()) {
            currentWaypoint = null;
        }
        if (!shouldUseAirMovement() && !canUseGroundNavigation()) {
            currentWaypoint = null;
            resetGroundPathState();
            dragon.getNavigation().stop();
        }
    }

    public boolean setWaypoint(LivingEntity target, double speed) {
        return target != null && setWaypoint(resolveTargetPosition(target), speed);
    }

    public boolean setWaypoint(Vec3 target, double speed) {
        if (target == null || dragon.level().isClientSide) {
            return false;
        }
        return startWaypoint(new QueuedWaypoint(target, speed, false, MovementMode.AUTO));
    }

    public boolean setAsyncAirWaypoint(Vec3 target, double speed) {
        if (target == null
                || dragon.level().isClientSide
                || !(dragon instanceof RideableFlyingDragon)) {
            return false;
        }
        return startWaypoint(new QueuedWaypoint(target, speed, false, MovementMode.AIR));
    }

    public boolean requestFlight(DragonFlightRequest request) {
        if (dragon.level().isClientSide || !(dragon instanceof RideableFlyingDragon)) return false;
        return startWaypoint(new QueuedWaypoint(request.target(), request.speedModifier(), false, MovementMode.AIR,
                null, request));
    }

    public boolean setWaypoint(LivingEntity target, double speed, boolean running) {
        if (target == null || dragon.level().isClientSide) {
            return false;
        }
        return startWaypoint(new QueuedWaypoint(resolveTargetPosition(target), speed, running, MovementMode.AUTO));
    }

    public boolean setWaypoint(Vec3 target, double speed, boolean running) {
        if (target == null || dragon.level().isClientSide) {
            return false;
        }
        return startWaypoint(new QueuedWaypoint(target, speed, running, MovementMode.AUTO));
    }

    /**
     * May be refreshed every Brain tick. True means accepted (including waiting for retry),
     * not arrived; completion and failure are reported by the controller's state.
     */
    public boolean requestGroundMovement(DragonGroundRequest request) {
        if (!brainMovement.canMutate(movementCommandGeneration) || !canUseGroundNavigation()) {
            return false;
        }
        ensureGroundNavigation();
        return startWaypoint(groundWaypoint(request));
    }

    /** Adopt a route already evaluated while choosing a destination (for example a drinking site). */
    public boolean followGroundPath(Path path, DragonGroundRequest request) {
        if (!brainMovement.canMutate(movementCommandGeneration) || !canUseGroundNavigation()) {
            return false;
        }
        ensureGroundNavigation();
        resetGroundPathState();
        dragon.getNavigation().stop();
        invalidateMovementCommand();
        currentWaypoint = groundWaypoint(request);
        groundSearchTarget = request.target();
        return beginFollowingGroundPath(path);
    }

    /** Hand off to a swim behaviour without leaving a ground search or an old auto waypoint active. */
    public boolean beginSwimMovement() {
        if (dragon.level().isClientSide || !brainMovement.canMutate(movementCommandGeneration)) return false;
        boolean retireWaypoint = currentWaypoint != null
                && (currentWaypoint.mode().usesGroundPath() || currentWaypoint.mode().usesWater());
        if (groundPathState != GroundPathState.IDLE || groundPathRequest != null || retireWaypoint) {
            invalidateMovementCommand();
            resetGroundPathState();
            if (retireWaypoint) currentWaypoint = null;
        }
        dragon.getNavigation().stop();
        brainMovement.commanded(movementCommandGeneration, dragon.level().getGameTime(), "swim-handoff", false);
        return true;
    }

    public void moveOntoShore(Vec3 landPosition) {
        moveAcrossShore(landPosition, false);
    }

    public void moveIntoWater(Vec3 waterPosition) {
        moveAcrossShore(waterPosition, true);
    }

    /** Short physical handoff across the shoreline; destination selection stays in the behaviour. */
    private void moveAcrossShore(Vec3 target, boolean enteringWater) {
        if (dragon.level().isClientSide || !brainMovement.canMutate(movementCommandGeneration) || dragon.isVehicle()
                || dragon.isPassenger() || dragon.isAerial()) return;
        Vec3 horizontal = new Vec3(target.x - dragon.getX(), 0.0D, target.z - dragon.getZ());
        if (horizontal.lengthSqr() < 1.0E-4D) return;
        invalidateMovementCommand();
        currentWaypoint = null;
        resetGroundPathState();
        dragon.getNavigation().stop();
        Vec3 direction = horizontal.normalize();
        Vec3 velocity = dragon.getDeltaMovement();
        double drag = enteringWater ? 0.4D : 0.45D;
        double boost = enteringWater
                ? (dragon.horizontalCollision ? 0.45D : 0.34D)
                : (dragon.horizontalCollision ? 0.48D : 0.36D);
        double upward = enteringWater
                ? (dragon.onGround() ? 0.20D : velocity.y)
                : (dragon.horizontalCollision ? 0.58D : 0.34D);
        dragon.setDeltaMovement(velocity.x * drag + direction.x * boost,
                Math.max(velocity.y, upward), velocity.z * drag + direction.z * boost);
        dragon.getMoveControl().setWantedPosition(target.x, target.y - (enteringWater ? 0.35D : 0.0D),
                target.z, enteringWater ? 1.1D : 1.15D);
        dragon.hasImpulse = true;
    }

    private static QueuedWaypoint groundWaypoint(DragonGroundRequest request) {
        MovementMode mode = request.route() == DragonGroundRequest.Route.PROGRESSIVE
                ? MovementMode.PROGRESSIVE_GROUND : MovementMode.GROUND;
        return new QueuedWaypoint(request.target(), request.speed(), request.running(), mode,
                request.arrival(), null);
    }

    public boolean requestGroundTransition(@Nullable LivingEntity target, double speed) {
        if (!brainMovement.canMutate(movementCommandGeneration)) {
            landingPlanDebugReason = "movement-owned-by-action";
            return false;
        }
        if (!dragon.canFly() || !(dragon instanceof DragonFlightCapable flightCapable)) {
            landingPlanDebugReason = "flight-unavailable";
            return false;
        }
        if (canCompleteWaterLanding()) {
            completeWaterLanding();
            return true;
        }
        if (dragon.onGround()) {
            landingPlanDebugReason = "already-grounded";
            if (dragon.isAerial()) {
                flightCapable.completeAiLanding();
                clearAllWaypoints();
                return true;
            }
            return false;
        }
        if (hasActiveLandingTransition()) {
            landingPlanDebugReason = "active";
            return true;
        }
        if (landingPlanRetryTicks > 0) {
            landingPlanDebugReason = "retry-cooldown";
            resumeLandingRecovery(speed);
            return false;
        }

        Vec3 landingTarget = DragonLandingSites.find(dragon, target);
        if (landingTarget == null) return landingUnavailable("no-safe-site", speed);
        landingPlanRetryTicks = 0;
        return beginGroundTransition(landingTarget, speed);
    }

    public boolean requestGroundTransition(@Nullable Vec3 landingTarget, double speed) {
        if (!brainMovement.canMutate(movementCommandGeneration)) {
            landingPlanDebugReason = "movement-owned-by-action";
            return false;
        }
        if (!dragon.canFly() || !(dragon instanceof DragonFlightCapable flightCapable)) {
            landingPlanDebugReason = "flight-unavailable";
            return false;
        }
        if (canCompleteWaterLanding()) {
            completeWaterLanding();
            return true;
        }
        if (dragon.onGround()) {
            landingPlanDebugReason = "already-grounded";
            if (dragon.isAerial()) {
                flightCapable.completeAiLanding();
                clearAllWaypoints();
                return true;
            }
            return false;
        }
        if (hasActiveLandingTransition()) {
            landingPlanDebugReason = "active";
            return true;
        }
        if (landingPlanRetryTicks > 0) {
            landingPlanDebugReason = "retry-cooldown";
            resumeLandingRecovery(speed);
            return false;
        }
        if (landingTarget == null) {
            landingPlanDebugReason = "no-target";
            return false;
        }
        Vec3 touchdown = DragonLandingSites.isValid(dragon, landingTarget)
                ? landingTarget : DragonLandingSites.findNear(dragon, landingTarget);
        if (touchdown == null) return landingUnavailable("no-safe-site", speed);
        landingPlanRetryTicks = 0;
        return beginGroundTransition(touchdown, speed);
    }

    public boolean requestOwnerFollowLanding(Vec3 ownerPosition, double stopDistance,
                                              double maxDistance, double maxVerticalDelta, double speed) {
        if (!brainMovement.canMutate(movementCommandGeneration) || !dragon.canFly()
                || !dragon.isAerial() || dragon.onGround()) {
            landingPlanDebugReason = "follow-landing-ineligible";
            return false;
        }
        if (hasActiveLandingTransition()) {
            landingPlanDebugReason = "active";
            return true;
        }
        if (landingPlanRetryTicks > 0) {
            landingPlanDebugReason = "retry-cooldown";
            resumeLandingRecovery(speed);
            return false;
        }
        Vec3 touchdown = DragonLandingSites.findForOwner(
                dragon, ownerPosition, stopDistance, maxDistance, maxVerticalDelta);
        if (touchdown == null) return landingUnavailable("no-safe-site", speed);
        landingPlanRetryTicks = 0;
        return beginGroundTransition(touchdown, speed);
    }

    public boolean hasActiveLandingTransition() {
        if (currentWaypoint == null || currentWaypoint.mode() != MovementMode.LANDING) {
            return false;
        }
        if (canCompleteWaterLanding()) {
            completeWaterLanding();
            return false;
        }
        if (dragon instanceof RideableFlyingDragon flyingDragon && flyingDragon.isAiFlightDone()) {
            double speed = currentWaypoint.speed();
            currentWaypoint = null;
            if (!dragon.onGround()) landingUnavailable("route-ended-before-contact", speed);
            return false;
        }
        return true;
    }

    public @Nullable Vec3 getActiveLandingTarget() {
        return hasActiveLandingTransition() ? currentWaypoint.target() : null;
    }

    public String getLandingPlanDebugSummary() {
        return "last=" + landingPlanDebugReason + ",retry=" + landingPlanRetryTicks
                + ",touchdown=" + (currentWaypoint != null && currentWaypoint.mode() == MovementMode.LANDING
                ? currentWaypoint.target() : "none");
    }

    /** A new destination must not inherit the backoff from an unrelated landing search. */
    public void clearLandingPlanRetry() {
        landingPlanRetryTicks = 0;
    }

    private boolean beginGroundTransition(Vec3 touchdown, double speed) {
        boolean accepted = startWaypoint(new QueuedWaypoint(touchdown, speed, false, MovementMode.LANDING));
        landingPlanDebugReason = accepted
                ? DragonLandingSites.isWaterSurface(dragon, touchdown) ? "accepted-water" : "accepted-ground"
                : "movement-rejected";
        return accepted;
    }

    private boolean canCompleteWaterLanding() {
        return DragonLandingSites.canLandOnWater(dragon) && dragon.isInWaterOrBubble()
                && !dragon.isInLava() && !dragon.isVehicle() && !dragon.isPassenger();
    }

    private void completeWaterLanding() {
        invalidateMovementCommand();
        currentWaypoint = null;
        resetGroundPathState();
        landingPlanRetryTicks = 0;
        landingPlanDebugReason = "water-contact";
        if (dragon instanceof RideableFlyingDragon flying) flying.completeAiWaterHandoff();
        // The landing has arrived.
        //Let swimming behaviors choose their own destination,rather than giving the swim pathfinder the old endpoint above the water.
        dragon.getAiSwimController().stop();
    }

    private boolean landingUnavailable(String reason, double speed) {
        landingPlanDebugReason = reason;
        landingPlanRetryTicks = LANDING_PLAN_FAILURE_RETRY_TICKS;
        resumeLandingRecovery(speed);
        return false;
    }

    private void resumeLandingRecovery(double speed) {
        if (dragon instanceof RideableFlyingDragon flying && dragon.isAerial() && !dragon.onGround()
                && !dragon.isInWaterOrBubble() && !dragon.isInLava()
                && !dragon.isVehicle() && !dragon.isPassenger()
                && brainMovement.canMutate(movementCommandGeneration)) {
            flying.beginAiFlight();
            if (!flying.isAiFlightPathing()) {
                Vec3 reposition = flightSpace.findLandingRecoveryTarget();
                if (reposition != null) requestFlight(DragonFlightRequest.cruise(reposition, speed));
            }
        }
    }

    public @Nullable Vec3 findGroundTransitionTarget(@Nullable LivingEntity target) {
        if (landingPlanRetryTicks > 0) {
            landingPlanDebugReason = "retry-cooldown";
            return null;
        }
        Vec3 touchdown = DragonLandingSites.find(dragon, target);
        landingPlanDebugReason = touchdown == null ? "no-safe-site" : "selected";
        if (touchdown == null) landingUnavailable("no-safe-site", 1.0D);
        return touchdown;
    }

    public @Nullable Vec3 findGroundWaypointBelow(Vec3 target) {
        if (target == null) {
            return null;
        }
        BlockPos column = BlockPos.containing(target);
        if (!dragon.level().hasChunkAt(column)) {
            return null;
        }
        BlockPos ground = findLandingGround(dragon, column, dragon.getBlockY());
        if (ground == null || !isValidLandingSurface(dragon, ground)) {
            return null;
        }
        return new Vec3(column.getX() + 0.5D, ground.getY() + 1.0D, column.getZ() + 0.5D);
    }

    public @Nullable Vec3 findTacticalGroundTransitionTarget(LivingEntity target,
                                                              int maxSearchRadius,
                                                              double maxVerticalDelta) {
        if (target == null || !target.isAlive()) {
            return null;
        }

        BlockPos origin = target.blockPosition();
        for (int radius = 0; radius <= Math.max(0, maxSearchRadius); radius += 4) {
            int attempts = radius == 0 ? 1 : 18;
            for (int attempt = 0; attempt < attempts; attempt++) {
                int dx = radius == 0 ? 0 : dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                int dz = radius == 0 ? 0 : dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                BlockPos column = origin.offset(dx, 0, dz);
                if (!dragon.level().hasChunkAt(column)) {
                    continue;
                }

                BlockPos ground = findLandingGround(dragon, column, origin.getY());
                if (ground == null) {
                    continue;
                }
                Vec3 landingTarget = new Vec3(
                        column.getX() + 0.5D,
                        ground.getY() + 1.0D,
                        column.getZ() + 0.5D
                );
                if (isTacticalGroundTransitionTargetValid(
                        landingTarget,
                        target,
                        maxSearchRadius,
                        maxVerticalDelta
                )) {
                    return landingTarget;
                }
            }
        }
        return null;
    }

    public boolean isTacticalGroundTransitionTargetValid(Vec3 landingTarget,
                                                          LivingEntity target,
                                                          double maxHorizontalDistance,
                                                          double maxVerticalDelta) {
        if (landingTarget == null
                || target == null
                || !target.isAlive()) {
            return false;
        }
        double dx = landingTarget.x - target.getX();
        double dz = landingTarget.z - target.getZ();
        if (dx * dx + dz * dz > maxHorizontalDistance * maxHorizontalDistance
                || Math.abs(landingTarget.y - target.getY()) > maxVerticalDelta) {
            return false;
        }
        if (DragonLandingSites.isWaterSurface(dragon, landingTarget)) {
            return DragonLandingSites.isValid(dragon, landingTarget);
        }
        BlockPos ground = BlockPos.containing(
                landingTarget.x,
                landingTarget.y - 1.0D,
                landingTarget.z
        );
        return hasTacticalLandingFootprint(landingTarget, ground.getY());
    }

    public void clearAllWaypoints() {
        if (!brainMovement.canMutate(movementCommandGeneration)) return;
        invalidateMovementCommand();
        currentWaypoint = null;
        resetGroundPathState();
        dragon.getNavigation().stop();
        dragon.getAiSwimController().clear();
        if (dragon instanceof RideableFlyingDragon flyingDragon) {
            flyingDragon.clearAiFlightTarget();
        }
    }

    public void stop() {
        if (!brainMovement.canMutate(movementCommandGeneration)) return;
        invalidateMovementCommand();
        boolean wasUsingWater = currentWaypoint != null && currentWaypoint.mode().usesWater();
        if (groundPathState == GroundPathState.FOLLOWING
                && !ignoreInheritedGroundNavigationStuck
                && dragon.getNavigation().isStuck()) {
            groundPathDebugReason = "navigation-stuck";
        }
        currentWaypoint = null;
        resetGroundPathState();
        if (wasUsingWater) {
            dragon.getAiSwimController().stop();
        }
        if (shouldUseAirMovement()) {
            clearGroundPath();
            if (!(dragon instanceof RideableFlyingDragon)) {
                dragon.getNavigation().stop();
            }
        } else {
            dragon.getNavigation().stop();
            if (dragon instanceof RideableFlyingDragon flyingDragon) {
                flyingDragon.clearAiFlightTarget();
            }
            setGroundIdle();
        }
    }

    public void stopAndClearAllMovement() {
        if (!brainMovement.canMutate(movementCommandGeneration)) return;
        invalidateMovementCommand();
        currentWaypoint = null;
        resetGroundPathState();
        dragon.getNavigation().stop();
        dragon.getAiSwimController().clear();
        if (dragon instanceof RideableFlyingDragon flyingDragon) {
            flyingDragon.clearAiFlightTarget();
        }
        if (!dragon.isVehicle()) {
            dragon.setAccelerating(false);
        }
        setGroundIdle();
    }

    public long getMovementCommandGeneration() {
        return movementCommandGeneration;
    }

    public boolean isMovementCommandCurrent(long generation) {
        return generation == movementCommandGeneration;
    }

    public boolean stopIfMovementCommandCurrent(long generation) {
        if (!isMovementCommandCurrent(generation)) {
            return false;
        }
        stop();
        return true;
    }

    public void setGroundIdle() {
        dragon.setRunning(false);
        dragon.setSprinting(false);
        dragon.setGroundMoveStateFromAI(0);
    }

    public void setGroundWalk() {
        dragon.setRunning(false);
        dragon.setSprinting(false);
        dragon.setGroundMoveStateFromAI(1);
    }

    public void setGroundRun() {
        dragon.setRunning(true);
        dragon.setSprinting(true);
        dragon.setGroundMoveStateFromAI(2);
    }

    public void setGroundMoveState(boolean running) {
        if (running) {
            setGroundRun();
        } else {
            setGroundWalk();
        }
    }

    public boolean isPathing() {
        if (currentWaypoint != null && currentWaypoint.mode().usesWater()) {
            return !hasArrived();
        }
        if (groundPathState == GroundPathState.WAITING || groundPathState == GroundPathState.CALCULATING
                || groundPathState == GroundPathState.FOLLOWING) {
            return true;
        }
        if (currentWaypoint != null && hasArrived()) {
            return false;
        }
        if (shouldUseAirMovement()) {
            if (dragon instanceof RideableFlyingDragon flyingDragon) {
                return flyingDragon.isAiFlightPathing();
            }
            return dragon.getNavigation().isInProgress();
        }
        return canUseGroundNavigation() && dragon.getNavigation().isInProgress();
    }

    public boolean hasArrived() {
        if (currentWaypoint != null
                && (currentWaypoint.mode().usesAir() || currentWaypoint.mode() == MovementMode.AUTO)
                && dragon instanceof RideableFlyingDragon flyingDragon
                && flyingDragon.isFlightControllerFailed()) {
            return false;
        }
        if (currentWaypoint != null && currentWaypoint.mode().usesWater()) {
            double arrivalDistance = waterArrivalDistance();
            return dragon.distanceToSqr(currentWaypoint.target()) <= arrivalDistance * arrivalDistance;
        }
        if (groundPathState == GroundPathState.ARRIVED) {
            return true;
        }
        if (groundPathState == GroundPathState.WAITING || groundPathState == GroundPathState.CALCULATING
                || groundPathState == GroundPathState.FOLLOWING
                || groundPathState == GroundPathState.FAILED) {
            return false;
        }
        if (currentWaypoint != null && currentWaypoint.mode().usesAir()) {
            if (currentWaypoint.mode() == MovementMode.LANDING) {
                return dragon.onGround();
            }
            if (dragon instanceof RideableFlyingDragon flyingDragon) return flyingDragon.isAiFlightDone();
            double arrivalDistance = Math.max(2.0D, dragon.getBbWidth());
            return dragon.distanceToSqr(currentWaypoint.target()) <= arrivalDistance * arrivalDistance;
        }
        if (shouldUseAirMovement()) {
            if (dragon instanceof RideableFlyingDragon flyingDragon) {
                return flyingDragon.isAiFlightDone();
            }
            return dragon.getNavigation().isDone();
        }
        // An idle/stopped navigator is not evidence that a requested destination was reached.
        return false;
    }

    public boolean hasFailed() {
        if (currentWaypoint != null
                && (currentWaypoint.mode().usesAir() || currentWaypoint.mode() == MovementMode.AUTO)
                && dragon instanceof RideableFlyingDragon flyingDragon
                && flyingDragon.isFlightControllerFailed()) {
            return true;
        }
        if (groundPathState == GroundPathState.FAILED) {
            return true;
        }
        // serverTick records navigation failures once, including retry timing and failure sequence.
        return false;
    }

    public long getGroundFailureSequence() {
        return groundFailureSequence;
    }

    public GroundPathState getGroundPathState() {
        return groundPathState;
    }

    public void clearGroundPathFailureRetry() {
        groundPathFailureRetryTicks = 0;
    }

    public boolean hasRepeatedGroundPathFailures() {
        return consecutiveGroundPathFailures >= REPEATED_GROUND_PATH_FAILURE_THRESHOLD;
    }

    public void clearGroundPathFailureHistory() {
        consecutiveGroundPathFailures = 0;
        groundPathFailureOrigin = null;
        lastFailedGroundTarget = null;
    }

    public String getDebugMovementMode() {
        return currentWaypoint == null ? "NONE" : currentWaypoint.mode().name();
    }

    public String getDebugGroundPathState() {
        return groundPathState.name();
    }

    public @Nullable Vec3 getDebugMovementTarget() {
        return currentWaypoint == null ? null : currentWaypoint.target();
    }

    public double getDebugMovementSpeed() {
        return currentWaypoint == null ? 0.0D : currentWaypoint.speed();
    }

    public String getDebugGroundPathDetails() {
        return "reason=" + groundPathDebugReason
                + ",failures=" + consecutiveGroundPathFailures
                + ",retry=" + groundPathFailureRetryTicks
                + ",repath=" + groundRepathTicks
                + ",arrival=" + (currentWaypoint == null || !currentWaypoint.mode().usesGroundPath()
                        ? "none" : groundArrival(currentWaypoint))
                + ",inheritedStuck=" + ignoreInheritedGroundNavigationStuck;
    }

    private boolean shouldUseAirMovement() {
        if (dragon instanceof RideableFlyingDragon flyingDragon && flyingDragon.isAiFlightPathing()) {
            return true;
        }
        return dragon.isFlying() || dragon.isTakeoff() || dragon.isHovering() || dragon.isLanding();
    }

    private boolean shouldUseWaterMovement() {
        return dragon instanceof SemiAquaticDragon
                && dragon.canSwim()
                && dragon.isInWaterOrBubble()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && dragon.isAlive();
    }

    private void tickWaterController() {
        if (lastWaterControllerTick == dragon.tickCount) {
            return;
        }
        lastWaterControllerTick = dragon.tickCount;
        dragon.getAiSwimController().serverTick();
    }

    private boolean handoffCurrentWaypointToWater() {
        QueuedWaypoint previousWaypoint = this.currentWaypoint;
        if (previousWaypoint == null) {
            return false;
        }

        resetGroundPathState();
        dragon.getNavigation().stop();
        if (dragon instanceof RideableFlyingDragon flyingDragon) {
            flyingDragon.completeAiWaterHandoff();
        }

        this.currentWaypoint = new QueuedWaypoint(
                previousWaypoint.target(),
                previousWaypoint.speed(),
                false,
                MovementMode.WATER
        );
        boolean accepted = dragon.getAiSwimController().trackTarget(
                previousWaypoint.target(),
                previousWaypoint.speed(),
                WATER_TURN_SPEED
        );
        if (!accepted) {
            dragon.getAiSwimController().stop();
            this.currentWaypoint = null;
            return false;
        }
        tickWaterController();
        return true;
    }

    private double waterArrivalDistance() {
        return Math.max(2.0D, dragon.getBbWidth() * 0.75D);
    }

    private boolean hasReachedGroundWaypoint() {
        return currentWaypoint != null && currentWaypoint.mode().usesGroundPath()
                && groundArrival(currentWaypoint).reached(dragon.position(), currentWaypoint.target());
    }

    private Vec3 resolveTargetPosition(LivingEntity target) {
        if (shouldUseWaterMovement() && target.isInWaterOrBubble()) {
            return target.position().add(0.0D, target.getBbHeight() * 0.35D, 0.0D);
        }
        return target.position();
    }

    private void clearGroundPath() {
        dragon.getNavigation().stop();
    }

    private boolean startWaypoint(QueuedWaypoint waypoint) {
        if (!brainMovement.canMutate(movementCommandGeneration)) return false;
        if (waypoint.mode() == MovementMode.AIR
                || (waypoint.mode() == MovementMode.AUTO && shouldUseAirMovement() && !shouldUseWaterMovement())) {
            Vec3 fitted = flightSpace.fitDestination(waypoint.target());
            if (fitted == null) return false;
            waypoint = new QueuedWaypoint(fitted, waypoint.speed(), waypoint.running(), waypoint.mode(),
                    waypoint.groundArrival(),
                    waypoint.flightRequest() == null ? null : waypoint.flightRequest().withTarget(fitted));
        }
        if (waypoint.mode() == MovementMode.AUTO && shouldUseWaterMovement()) {
            waypoint = new QueuedWaypoint(
                    waypoint.target(),
                    waypoint.speed(),
                    waypoint.running(),
                    MovementMode.WATER
            );
        }
        if (waypoint.mode() == MovementMode.AUTO && !shouldUseAirMovement()) {
            waypoint = groundWaypoint(DragonGroundRequest.travel(waypoint.target(), waypoint.speed(),
                    waypoint.running(), DragonGroundRequest.Arrival.near(dragon)));
        }
        if (waypoint.mode().usesGroundPath()) {
            if (!canUseGroundNavigation()) return false;
            if (groundArrival(waypoint).reached(dragon.position(), waypoint.target())) {
                invalidateMovementCommand();
                completeGroundArrival();
                return true;
            }
            if (groundPathFailureRetryTicks > 0) {
                if (groundPathState != GroundPathState.WAITING) invalidateMovementCommand();
                currentWaypoint = waypoint;
                groundPathState = GroundPathState.WAITING;
                groundPathDebugReason = "waiting-for-retry";
                brainMovement.commanded(movementCommandGeneration, dragon.level().getGameTime(), "ground-request", false);
                return true;
            }

            boolean active = groundPathState == GroundPathState.CALCULATING
                    || groundPathState == GroundPathState.FOLLOWING;
            boolean sameContract = currentWaypoint != null && currentWaypoint.mode() == waypoint.mode()
                    && groundArrival(currentWaypoint).equals(groundArrival(waypoint));
            if (active && sameContract && groundSearchTarget != null
                    && (groundPathState == GroundPathState.CALCULATING || groundRepathTicks > 0
                    || groundSearchTarget.distanceToSqr(waypoint.target()) < 1.0D)) {
                currentWaypoint = waypoint;
                brainMovement.commanded(movementCommandGeneration, dragon.level().getGameTime(), "ground-request", false);
                // Updating a request must also update the executing path, even without a new search.
                dragon.getNavigation().setSpeedModifier(waypoint.speed());
                if (!dragon.getNavigation().isDone()) {
                    configureFinalGroundWaypointTolerance(dragon.getNavigation().getPath(), waypoint);
                    setGroundMoveState(waypoint.running());
                }
                return true;
            }
        }

        invalidateMovementCommand();
        currentWaypoint = waypoint;
        if (waypoint.mode() != MovementMode.PROGRESSIVE_GROUND) {
            groundRouteProgress.reset();
        }
        if (!waypoint.mode().usesGroundPath()) {
            resetGroundPathState();
        }
        if (waypoint.running()) {
            setGroundRun();
        }

        if (waypoint.mode().usesWater()) {
            resetGroundPathState();
            dragon.getNavigation().stop();
            if (dragon instanceof RideableFlyingDragon flyingDragon) {
                flyingDragon.clearAiFlightTarget();
            }
            AsyncSwimController controller = dragon.getAiSwimController();
            boolean accepted = controller.trackTarget(
                    waypoint.target(),
                    waypoint.speed(),
                    WATER_TURN_SPEED
            );
            if (accepted) {
                tickWaterController();
            }
            return accepted;
        }

        if (waypoint.mode().usesAir() || (waypoint.mode() == MovementMode.AUTO && shouldUseAirMovement())) {
            resetGroundPathState();
            if (dragon instanceof RideableFlyingDragon flyingDragon) {
                if (waypoint.mode() == MovementMode.LANDING) {
                    flyingDragon.pathAiGroundTransitionTo(waypoint.target(), waypoint.speed());
                } else {
                    DragonFlightRequest request = waypoint.flightRequest() != null ? waypoint.flightRequest()
                            : waypoint.mode() == MovementMode.AIR
                                ? DragonFlightRequest.cruise(waypoint.target(), waypoint.speed())
                                : DragonFlightRequest.track(waypoint.target(), waypoint.speed());
                    flyingDragon.requestAiFlight(request);
                }
                return true;
            }
            return dragon.getNavigation().moveTo(waypoint.target().x, waypoint.target().y, waypoint.target().z, waypoint.speed());
        }

        if (!canUseGroundNavigation()) {
            clearGroundPath();
            currentWaypoint = null;
            ignoreInheritedGroundNavigationStuck = false;
            groundPathState = waypoint.mode().usesGroundPath()
                    ? GroundPathState.FAILED
                    : GroundPathState.IDLE;
            groundPathDebugReason = "ground-navigation-unavailable";
            return false;
        }
        return tryStartGroundPath(waypoint);
    }

    private boolean tryStartGroundPath(QueuedWaypoint waypoint) {
        if (groundArrival(waypoint).reached(dragon.position(), waypoint.target())) {
            completeGroundArrival();
            return true;
        }
        if (!dragon.level().noCollision(dragon, dragon.getBoundingBox().deflate(1.0E-3D))) {
            recordGroundPathFailure(waypoint.target(), "invalid-start-body");
            return false;
        }
        startGroundPathAsync(waypoint);
        return true;
    }

    private void invalidateMovementCommand() {
        movementCommandGeneration++;
        brainMovement.commanded(movementCommandGeneration, dragon.level().getGameTime(), "command", false);
    }

    private void startGroundPathAsync(QueuedWaypoint waypoint) {
        ensureGroundNavigation();
        if (groundPathRequest != null) {
            groundPathRequest.cancel(true);
            groundPathRequest = null;
        }
        int detourAllowance = groundPathDetourAllowance(waypoint.target());
        boolean replacingActivePath = groundPathState == GroundPathState.FOLLOWING
                && !dragon.getNavigation().isDone();
        if (!replacingActivePath) {
            dragon.getNavigation().stop();
        }
        long requestGeneration = ++groundPathRequestGeneration;
        groundSearchTarget = waypoint.target();
        double distance = dragon.position().distanceTo(waypoint.target());
        groundRepathTicks = Mth.clamp((int)Math.ceil(distance * (waypoint.running() ? 0.3D : 0.45D)),
                waypoint.running() ? 4 : 6, waypoint.running() ? 18 : 24);
        groundPathState = GroundPathState.CALCULATING;
        groundPathDebugReason = detourAllowance > 0
                ? "calculating-detour-" + detourAllowance
                : "calculating";
        double radialArrivalDistance = groundArrivalDistance(waypoint);
        int goalAccuracy = Math.max(0, Mth.floor(radialArrivalDistance / Math.sqrt(2.0D)));
        groundPathRequest = AsyncDragonPathfinder.calculateGroundPathAsync(
                dragon,
                waypoint.target(),
                goalAccuracy,
                false,
                detourAllowance,
                path -> {
                    if (requestGeneration != groundPathRequestGeneration
                            || currentWaypoint == null
                            || !currentWaypoint.mode().usesGroundPath()
                            || !canUseGroundNavigation()) {
                        return;
                    }
                    groundPathRequest = null;
                    if (beginFollowingGroundPath(path) && groundPathState == GroundPathState.FOLLOWING
                            && detourAllowance > 0) {
                        groundPathDebugReason += "-detour-" + detourAllowance;
                    }
                }
        );
    }

    private boolean beginFollowingGroundPath(@Nullable Path path) {
        if (hasReachedGroundWaypoint()) {
            completeGroundArrival();
            return true;
        }
        if (path == null || path.getNodeCount() == 0) {
            recordGroundPathFailure(currentWaypoint.target(), "empty-path");
            return false;
        }
        Vec3 endpoint = path.getEntityPosAtNode(dragon, path.getNodeCount() - 1);
        boolean endpointReachesTarget = groundArrival(currentWaypoint).reached(endpoint, currentWaypoint.target());
        if (path.getNodeCount() == 1 && dragon.position().distanceToSqr(endpoint) < 1.0E-6D) {
            recordGroundPathFailure(currentWaypoint.target(), "zero-progress-path");
            return false;
        }
        // A supplied candidate route may have been retargeted to its validated endpoint.
        if (!path.canReach() && !endpointReachesTarget && (currentWaypoint.mode().requiresCompletePath()
                || !hasUsefulPartialGroundProgress(path, currentWaypoint.target()))) {
            recordGroundPathFailure(currentWaypoint.target(), "incomplete-path");
            return false;
        }
        if (!dragon.getNavigation().moveTo(path, currentWaypoint.speed())) {
            recordGroundPathFailure(currentWaypoint.target(), "navigation-rejected-path");
            return false;
        }
        configureFinalGroundWaypointTolerance(path, currentWaypoint);
        groundPathFailureRetryTicks = 0;
        if (currentWaypoint.mode() == MovementMode.PROGRESSIVE_GROUND) {
            groundRouteProgress.beginSegment(dragon.position(), endpoint,
                    currentWaypoint.target(), groundPathLength(path), minimumGroundProgress());
        }
        ignoreInheritedGroundNavigationStuck = dragon.getNavigation().isStuck();
        setGroundMoveState(currentWaypoint.running());
        groundPathState = GroundPathState.FOLLOWING;
        groundPathDebugReason = (path.canReach() ? "following-complete-path" : "following-partial-path")
                + (path instanceof DragonGroundPath groundPath && groundPath.endsAtSearchBoundary() ? "-frontier" : "");
        return true;
    }

    private boolean hasUsefulPartialGroundProgress(Path path, Vec3 target) {
        if (path == null || path.getNodeCount() == 0 || target == null) {
            return false;
        }

        Vec3 endpoint = path.getEntityPosAtNode(dragon, path.getNodeCount() - 1);
        return groundRouteProgress.canFollowPartial(dragon.position(), endpoint, target,
                path instanceof DragonGroundPath groundPath && groundPath.endsAtSearchBoundary(),
                minimumGroundProgress(), groundPathLength(path));
    }

    private double groundPathLength(Path path) {
        double length = 0.0D;
        Vec3 previous = dragon.position();
        for (int i = 0; i < path.getNodeCount(); i++) {
            Vec3 point = path.getEntityPosAtNode(dragon, i);
            length += previous.distanceTo(point);
            previous = point;
        }
        return length;
    }

    private void completeProgressiveGroundSegment() {
        QueuedWaypoint waypoint = currentWaypoint;
        if (waypoint == null) {
            recordGroundPathFailure(null, "missing-progressive-waypoint");
            return;
        }

        if (!groundRouteProgress.completeSegment(dragon.position(), groundArrival(waypoint))) {
            recordGroundPathFailure(waypoint.target(), "progressive-segment-no-progress");
            return;
        }

        startGroundPathAsync(waypoint);
    }

    private void completeGroundArrival() {
        resetGroundPathState();
        currentWaypoint = null;
        groundRouteProgress.reset();
        ignoreInheritedGroundNavigationStuck = false;
        groundPathState = GroundPathState.ARRIVED;
        groundPathDebugReason = "arrived";
        groundPathFailureRetryTicks = 0;
        clearGroundPathFailureHistory();
        dragon.getNavigation().stop();
        setGroundIdle();
    }

    private double minimumGroundProgress() {
        return Math.max(0.75D, Math.min(2.0D, dragon.getBbWidth() * 0.25D));
    }

    private int groundPathDetourAllowance(Vec3 target) {
        if (consecutiveGroundPathFailures <= 0
                || lastFailedGroundTarget == null
                || lastFailedGroundTarget.distanceToSqr(target) >= 1.0D) {
            clearGroundPathFailureHistory();
            return 0;
        }

        double resetDistance = Math.max(2.0D, dragon.getBbWidth());
        if (groundPathFailureOrigin == null
                || groundPathFailureOrigin.distanceToSqr(dragon.position())
                > resetDistance * resetDistance) {
            clearGroundPathFailureHistory();
            return 0;
        }
        return FAILED_ROUTE_DETOUR_ALLOWANCE;
    }

    private void configureFinalGroundWaypointTolerance(Path path, QueuedWaypoint waypoint) {
        if (path.getNodeCount() == 0
                || !(dragon.getNavigation() instanceof PathNavigateGround groundNavigation)) {
            return;
        }

        if (!groundArrival(waypoint).groundPosition()
                && waypoint.mode() == MovementMode.PROGRESSIVE_GROUND
                && !path.canReach()) {
            return;
        }

        Vec3 endpoint = path.getEntityPosAtNode(dragon, path.getNodeCount() - 1);
        double endpointOffset = horizontalDistance(endpoint, waypoint.target());
        double remainingArrivalRadius = Math.max(
                0.05D,
                groundArrivalDistance(waypoint) - endpointOffset
        );
        groundNavigation.setFinalWaypointTolerance(remainingArrivalRadius / Math.sqrt(2.0D));
    }

    private double groundArrivalDistance(QueuedWaypoint waypoint) {
        return groundArrival(waypoint).radius();
    }

    private DragonGroundRequest.Arrival groundArrival(QueuedWaypoint waypoint) {
        return waypoint.groundArrival() != null ? waypoint.groundArrival() : DragonGroundRequest.Arrival.near(dragon);
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void recordGroundPathFailure(@Nullable Vec3 target, String reason) {
        resetGroundPathState();
        groundFailureSequence++;
        double resetDistance = Math.max(2.0D, dragon.getBbWidth());
        if (groundPathFailureOrigin == null
                || groundPathFailureOrigin.distanceToSqr(dragon.position())
                > resetDistance * resetDistance) {
            consecutiveGroundPathFailures = 0;
            groundPathFailureOrigin = dragon.position();
        }
        consecutiveGroundPathFailures++;
        currentWaypoint = null;
        ignoreInheritedGroundNavigationStuck = false;
        groundPathState = GroundPathState.FAILED;
        groundPathDebugReason = reason;
        groundPathFailureRetryTicks = GROUND_PATH_FAILURE_RETRY_TICKS;
        lastFailedGroundTarget = target;
        dragon.getNavigation().stop();
        setGroundIdle();
    }

    private void invalidateGroundPathRequest() {
        groundPathRequestGeneration++;
    }

    private void resetGroundPathState() {
        if (groundPathRequest != null) {
            groundPathRequest.cancel(true);
            groundPathRequest = null;
        }
        invalidateGroundPathRequest();
        groundRouteProgress.reset();
        groundSearchTarget = null;
        groundRepathTicks = 0;
        ignoreInheritedGroundNavigationStuck = false;
        groundPathState = GroundPathState.IDLE;
    }

    private boolean canUseGroundNavigation() {
        return !dragon.level().isClientSide
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && dragon.isAlive()
                && !dragon.isFlying()
                && !dragon.isTakeoff()
                && !dragon.isHovering()
                && !dragon.isLanding()
                && !dragon.isInWaterOrBubble()
                && !dragon.isInLava();
    }

    private void ensureGroundNavigation() {
        if (dragon instanceof RideableFlyingDragon flyingDragon) {
            flyingDragon.switchToGroundNavigation();
        }
    }

    private static @Nullable BlockPos findLandingGround(Mob dragon, BlockPos column, int originY) {
        return DragonFlightSpace.findLandingGround(dragon, column, originY);
    }

    private static boolean isValidLandingSurface(Mob dragon, BlockPos ground) {
        if (!dragon.level().hasChunkAt(ground)) {
            return false;
        }

        var state = dragon.level().getBlockState(ground);
        if (state.isAir() || !state.getFluidState().isEmpty() || !state.isFaceSturdy(dragon.level(), ground, Direction.UP)) {
            return false;
        }

        BlockPos above = ground.above();
        BlockPos aboveTwo = above.above();
        var aboveState = dragon.level().getBlockState(above);
        var aboveTwoState = dragon.level().getBlockState(aboveTwo);
        return aboveState.getCollisionShape(dragon.level(), above).isEmpty()
                && aboveState.getFluidState().isEmpty()
                && aboveTwoState.getCollisionShape(dragon.level(), aboveTwo).isEmpty()
                && aboveTwoState.getFluidState().isEmpty();
    }

    private boolean hasTacticalLandingFootprint(Vec3 landingTarget, int groundY) {
        double halfWidth = dragon.getBbWidth() * 0.5D;
        int minX = (int)Math.floor(landingTarget.x - halfWidth + 0.05D);
        int maxX = (int)Math.floor(landingTarget.x + halfWidth - 0.05D);
        int minZ = (int)Math.floor(landingTarget.z - halfWidth + 0.05D);
        int maxZ = (int)Math.floor(landingTarget.z + halfWidth - 0.05D);
        int clearanceHeight = Math.max(2, (int)Math.ceil(dragon.getBbHeight()));

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos support = new BlockPos(x, groundY, z);
                if (!dragon.level().hasChunkAt(support)) {
                    return false;
                }
                var supportState = dragon.level().getBlockState(support);
                if (supportState.isAir()
                        || !supportState.getFluidState().isEmpty()
                        || !supportState.isFaceSturdy(dragon.level(), support, Direction.UP)) {
                    return false;
                }

                for (int dy = 1; dy <= clearanceHeight; dy++) {
                    BlockPos clearance = support.above(dy);
                    var clearanceState = dragon.level().getBlockState(clearance);
                    if (!clearanceState.getCollisionShape(dragon.level(), clearance).isEmpty()
                            || !clearanceState.getFluidState().isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private record QueuedWaypoint(Vec3 target,
                                  double speed,
                                  boolean running,
                                  MovementMode mode,
                                  @Nullable DragonGroundRequest.Arrival groundArrival,
                                  @Nullable DragonFlightRequest flightRequest) {
        private QueuedWaypoint(Vec3 target, double speed, boolean running, MovementMode mode) {
            this(target, speed, running, mode, null, null);
        }
    }

    private enum MovementMode {
        AUTO,
        AIR,
        WATER,
        GROUND,
        PROGRESSIVE_GROUND,
        LANDING;

        private boolean usesAir() {
            return this == AIR || this == LANDING;
        }

        private boolean usesWater() {
            return this == WATER;
        }

        private boolean usesGroundPath() {
            return this == GROUND || this == PROGRESSIVE_GROUND;
        }

        private boolean requiresCompletePath() {
            return this == GROUND;
        }
    }

    public enum GroundPathState {
        IDLE,
        WAITING,
        CALCULATING,
        FOLLOWING,
        ARRIVED,
        FAILED
    }
}
