package com.leon.saintsdragons.server.ai.navigation;

import com.leon.saintsdragons.server.ai.navigation.async.AsyncDragonPathfinder;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.ai.navigation.async.DragonLandingPlan;
import com.leon.saintsdragons.server.ai.navigation.async.DragonLandingPlanner;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Future;

public class DragonAIMovementController {
    private static final int GROUND_PATH_FAILURE_RETRY_TICKS = 20;
    private static final int REPEATED_GROUND_PATH_FAILURE_THRESHOLD = 3;
    private static final int FAILED_ROUTE_DETOUR_ALLOWANCE = 40;
    private static final int LANDING_PLAN_FAILURE_RETRY_TICKS = 20;
    private static final float WATER_TURN_SPEED = 8.0F;

    private final RideableDragonBase dragon;
    private @Nullable QueuedWaypoint currentWaypoint;
    private long movementCommandGeneration;
    private GroundPathState groundPathState = GroundPathState.IDLE;
    private long groundPathRequestGeneration;
    private @Nullable Future<?> groundPathRequest;
    private int groundPathFailureRetryTicks;
    private @Nullable Vec3 lastFailedGroundTarget;
    private int consecutiveGroundPathFailures;
    private @Nullable Vec3 groundPathFailureOrigin;
    private double groundPathSegmentStartDistance = Double.NaN;
    private boolean ignoreInheritedGroundNavigationStuck;
    private String groundPathDebugReason = "idle";
    private int landingPlanRetryTicks;
    private @Nullable DragonLandingPlan pendingLandingPlan;
    private int lastWaterControllerTick = Integer.MIN_VALUE;

    public DragonAIMovementController(RideableDragonBase dragon) {
        this.dragon = dragon;
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
        if (landingPlanRetryTicks > 0) {
            landingPlanRetryTicks--;
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

    public boolean setGroundWaypoint(LivingEntity target, double speed) {
        if (target == null || !canUseGroundNavigation()) {
            clearGroundPath();
            return false;
        }
        return setGroundWaypoint(target.position(), speed);
    }

    public boolean setGroundWaypoint(Vec3 target, double speed) {
        return setGroundWaypoint(target, speed, false);
    }

    private boolean setGroundWaypoint(Vec3 target, double speed, boolean running) {
        if (target == null || !canUseGroundNavigation()) {
            clearGroundPath();
            return false;
        }
        ensureGroundNavigation();
        return startWaypoint(new QueuedWaypoint(target, speed, running, MovementMode.GROUND));
    }

    public boolean moveToGroundTarget(LivingEntity target, double speed, boolean running) {
        return target != null && setGroundWaypoint(target.position(), speed, running);
    }

    public boolean moveToGroundPosition(Vec3 target, double speed, boolean running) {
        return setGroundWaypoint(target, speed, running);
    }

    public boolean moveToPreciseGroundPosition(Vec3 target,
                                               double speed,
                                               boolean running,
                                               double arrivalTolerance) {
        if (target == null || arrivalTolerance <= 0.0D || !canUseGroundNavigation()) {
            clearGroundPath();
            return false;
        }
        ensureGroundNavigation();
        return startWaypoint(new QueuedWaypoint(
                target,
                speed,
                running,
                MovementMode.GROUND,
                arrivalTolerance
        ));
    }

    public boolean followGroundPath(Path path, Vec3 target, double speed, boolean running) {
        return followGroundPath(path, target, speed, running, Double.NaN);
    }

    public boolean followGroundPath(Path path,
                                    Vec3 target,
                                    double speed,
                                    boolean running,
                                    double arrivalTolerance) {
        if (path == null
                || path.getNodeCount() == 0
                || target == null
                || !canUseGroundNavigation()) {
            stop();
            return false;
        }

        ensureGroundNavigation();
        resetGroundPathState();
        dragon.getNavigation().stop();
        invalidateMovementCommand();
        currentWaypoint = new QueuedWaypoint(
                target,
                speed,
                running,
                MovementMode.GROUND,
                arrivalTolerance
        );
        if (!dragon.getNavigation().moveTo(path, speed)) {
            recordGroundPathFailure(target, "navigation-rejected-path");
            return false;
        }
        configureFinalGroundWaypointTolerance(path, currentWaypoint);

        groundPathFailureRetryTicks = 0;
        lastFailedGroundTarget = null;
        ignoreInheritedGroundNavigationStuck = dragon.getNavigation().isStuck();
        setGroundMoveState(running);
        groundPathState = GroundPathState.FOLLOWING;
        groundPathDebugReason = "following-supplied-path";
        return true;
    }

    public boolean moveToProgressiveGroundPosition(Vec3 target, double speed, boolean running) {
        return moveToProgressiveGroundPosition(target, speed, running, Double.NaN);
    }

    public boolean moveToProgressiveGroundPosition(Vec3 target,
                                                   double speed,
                                                   boolean running,
                                                   double arrivalTolerance) {
        if (target == null || !canUseGroundNavigation()) {
            clearGroundPath();
            return false;
        }
        ensureGroundNavigation();
        return startWaypoint(new QueuedWaypoint(
                target,
                speed,
                running,
                MovementMode.PROGRESSIVE_GROUND,
                arrivalTolerance
        ));
    }

    public boolean moveToProgressiveGroundTarget(LivingEntity target, double speed, boolean running) {
        return target != null && moveToProgressiveGroundPosition(target.position(), speed, running);
    }

    public boolean requestGroundTransition(@Nullable LivingEntity target, double speed) {
        if (!dragon.canFly() || !(dragon instanceof DragonFlightCapable flightCapable)) {
            return false;
        }
        if (dragon.onGround()) {
            if (dragon.isAerial()) {
                flightCapable.markLandedNow();
                clearAllWaypoints();
                return true;
            }
            return false;
        }
        if (hasActiveLandingTransition()) {
            return true;
        }
        if (landingPlanRetryTicks > 0) {
            return false;
        }

        this.pendingLandingPlan = null;
        DragonLandingPlan landingPlan = DragonLandingPlanner.findPlan(dragon, target);
        if (landingPlan == null) {
            landingPlanRetryTicks = LANDING_PLAN_FAILURE_RETRY_TICKS;
            return false;
        }
        landingPlanRetryTicks = 0;
        return beginGroundTransition(landingPlan, speed);
    }

    public boolean requestGroundTransition(@Nullable Vec3 landingTarget, double speed) {
        if (!dragon.canFly() || !(dragon instanceof DragonFlightCapable flightCapable)) {
            return false;
        }
        if (dragon.onGround()) {
            if (dragon.isAerial()) {
                flightCapable.markLandedNow();
                clearAllWaypoints();
                return true;
            }
            return false;
        }
        if (hasActiveLandingTransition()) {
            return true;
        }
        if (landingPlanRetryTicks > 0) {
            return false;
        }
        if (landingTarget == null) {
            return false;
        }
        DragonLandingPlan landingPlan = this.pendingLandingPlan;
        this.pendingLandingPlan = null;
        if (landingPlan == null
                || landingPlan.touchdown().distanceToSqr(landingTarget) >= 1.0D) {
            landingPlan = DragonLandingPlanner.findPlanNear(dragon, landingTarget);
        }
        if (landingPlan == null) {
            landingPlanRetryTicks = LANDING_PLAN_FAILURE_RETRY_TICKS;
            return false;
        }
        landingPlanRetryTicks = 0;
        return beginGroundTransition(landingPlan, speed);
    }

    private boolean hasActiveLandingTransition() {
        if (currentWaypoint == null || currentWaypoint.mode() != MovementMode.LANDING) {
            return false;
        }
        if (dragon instanceof RideableFlyingDragon flyingDragon
                && flyingDragon.isFlightControllerFailed()) {
            currentWaypoint = null;
            return false;
        }
        return true;
    }

    private boolean beginGroundTransition(@Nullable DragonLandingPlan landingPlan, double speed) {
        if (landingPlan == null) {
            return false;
        }
        this.pendingLandingPlan = null;
        return startWaypoint(new QueuedWaypoint(
                landingPlan.touchdown(),
                speed,
                false,
                MovementMode.LANDING,
                landingPlan,
                Double.NaN
        ));
    }

    public @Nullable Vec3 findGroundTransitionTarget(@Nullable LivingEntity target) {
        if (landingPlanRetryTicks > 0) {
            return null;
        }
        DragonLandingPlan plan = DragonLandingPlanner.findPlan(dragon, target);
        this.pendingLandingPlan = plan;
        if (plan == null) {
            landingPlanRetryTicks = LANDING_PLAN_FAILURE_RETRY_TICKS;
        }
        return plan == null ? null : plan.touchdown();
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
        BlockPos ground = BlockPos.containing(
                landingTarget.x,
                landingTarget.y - 1.0D,
                landingTarget.z
        );
        return hasTacticalLandingFootprint(landingTarget, ground.getY());
    }

    public void clearAllWaypoints() {
        invalidateMovementCommand();
        currentWaypoint = null;
        pendingLandingPlan = null;
        resetGroundPathState();
        dragon.getNavigation().stop();
        dragon.getAiSwimController().clear();
        if (dragon instanceof RideableFlyingDragon flyingDragon) {
            flyingDragon.clearAiFlightTarget();
        }
    }

    public void stop() {
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
        if (groundPathState == GroundPathState.CALCULATING
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
        if (groundPathState == GroundPathState.CALCULATING
                || groundPathState == GroundPathState.FOLLOWING
                || groundPathState == GroundPathState.FAILED) {
            return false;
        }
        if (currentWaypoint != null && currentWaypoint.mode().usesAir()) {
            if (currentWaypoint.mode() == MovementMode.LANDING) {
                return dragon.onGround();
            }
            double arrivalDistance = Math.max(2.0D, dragon.getBbWidth());
            return dragon.distanceToSqr(currentWaypoint.target()) <= arrivalDistance * arrivalDistance;
        }
        if (shouldUseAirMovement()) {
            if (dragon instanceof RideableFlyingDragon flyingDragon) {
                return flyingDragon.isAiFlightDone();
            }
            return dragon.getNavigation().isDone();
        }
        return !canUseGroundNavigation() || dragon.getNavigation().isDone();
    }

    public boolean hasFailed() {
        if (currentWaypoint != null
                && dragon instanceof RideableFlyingDragon flyingDragon
                && flyingDragon.isFlightControllerFailed()) {
            return true;
        }
        if (groundPathState == GroundPathState.FAILED) {
            return true;
        }
        if (groundPathState == GroundPathState.CALCULATING) {
            return false;
        }
        if (groundPathState == GroundPathState.FOLLOWING) {
            return !hasReachedGroundWaypoint()
                    && !ignoreInheritedGroundNavigationStuck
                    && dragon.getNavigation().isStuck();
        }
        return !shouldUseAirMovement()
                && canUseGroundNavigation()
                && dragon.getNavigation().isStuck();
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
        if (currentWaypoint == null) {
            return false;
        }
        if (!currentWaypoint.hasPreciseGroundArrival()) {
            double arrivalDistance = Math.max(1.5D, dragon.getBbWidth() * 0.75D);
            return dragon.distanceToSqr(currentWaypoint.target()) <= arrivalDistance * arrivalDistance;
        }

        Vec3 target = currentWaypoint.target();
        double dx = dragon.getX() - target.x;
        double dz = dragon.getZ() - target.z;
        double verticalOffset = dragon.getY() - target.y;
        double arrivalTolerance = currentWaypoint.groundArrivalTolerance();
        return dx * dx + dz * dz <= arrivalTolerance * arrivalTolerance
                && verticalOffset > -1.0D
                && verticalOffset <= 1.5D;
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
        if (waypoint.mode() == MovementMode.AUTO && shouldUseWaterMovement()) {
            waypoint = new QueuedWaypoint(
                    waypoint.target(),
                    waypoint.speed(),
                    waypoint.running(),
                    MovementMode.WATER
            );
        }
        if (!waypoint.mode().usesAir()
                && !waypoint.mode().usesWater()
                && !shouldUseAirMovement()
                && groundPathFailureRetryTicks > 0
                && lastFailedGroundTarget != null
                && lastFailedGroundTarget.distanceToSqr(waypoint.target()) < 1.0D) {
            return false;
        }
        if (!waypoint.mode().usesAir()
                && !waypoint.mode().usesWater()
                && !shouldUseAirMovement()
                && currentWaypoint != null
                && !currentWaypoint.mode().usesAir()
                && currentWaypoint.target().distanceToSqr(waypoint.target()) < 1.0D
                && (groundPathState == GroundPathState.CALCULATING
                || groundPathState == GroundPathState.FOLLOWING)) {
            currentWaypoint = waypoint;
            return true;
        }

        invalidateMovementCommand();
        currentWaypoint = waypoint;
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
                    if (waypoint.landingPlan() == null) {
                        return false;
                    }
                    flyingDragon.pathAiLandingPlan(waypoint.landingPlan(), waypoint.speed());
                } else if (waypoint.mode() == MovementMode.AIR) {
                    flyingDragon.pathAiFlightTo(waypoint.target(), waypoint.speed());
                } else {
                    flyingDragon.trackAiFlightTarget(waypoint.target(), waypoint.speed());
                }
                return true;
            }
            return dragon.getNavigation().moveTo(waypoint.target().x, waypoint.target().y, waypoint.target().z, waypoint.speed());
        }

        if (!canUseGroundNavigation()) {
            clearGroundPath();
            currentWaypoint = null;
            groundPathSegmentStartDistance = Double.NaN;
            ignoreInheritedGroundNavigationStuck = false;
            groundPathState = waypoint.mode().usesGroundPath()
                    ? GroundPathState.FAILED
                    : GroundPathState.IDLE;
            groundPathDebugReason = "ground-navigation-unavailable";
            return false;
        }
        startGroundPathAsync(waypoint);
        return true;
    }

    private void invalidateMovementCommand() {
        movementCommandGeneration++;
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
        groundPathState = GroundPathState.CALCULATING;
        groundPathDebugReason = detourAllowance > 0
                ? "calculating-detour-" + detourAllowance
                : "calculating";
        double radialArrivalDistance = waypoint.hasPreciseGroundArrival()
                ? waypoint.groundArrivalTolerance()
                : Math.max(1.5D, dragon.getBbWidth() * 0.75D);
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
                            || currentWaypoint.mode().usesAir()
                            || !canUseGroundNavigation()) {
                        return;
                    }
                    groundPathRequest = null;
                    if (path == null || path.getNodeCount() == 0) {
                        recordGroundPathFailure(currentWaypoint.target(), "empty-path");
                        return;
                    }
                    if (path.getNodeCount() == 1 && !hasReachedGroundWaypoint()) {
                        recordGroundPathFailure(currentWaypoint.target(), "zero-progress-path");
                        return;
                    }

                    if (!path.canReach()) {
                        if (currentWaypoint.mode().requiresCompletePath()
                                || (currentWaypoint.mode() == MovementMode.PROGRESSIVE_GROUND
                                && !hasUsefulPartialGroundProgress(path, currentWaypoint.target()))) {
                            recordGroundPathFailure(currentWaypoint.target(), "incomplete-path");
                            return;
                        }
                    }

                    Path resolvedPath = path;
                    boolean started = dragon.getNavigation().moveTo(resolvedPath, currentWaypoint.speed());
                    if (!started) {
                        recordGroundPathFailure(currentWaypoint.target(), "navigation-rejected-path");
                        return;
                    }
                    configureFinalGroundWaypointTolerance(resolvedPath, currentWaypoint);
                    groundPathFailureRetryTicks = 0;
                    groundPathSegmentStartDistance = dragon.position().distanceTo(currentWaypoint.target());
                    ignoreInheritedGroundNavigationStuck = dragon.getNavigation().isStuck();
                    setGroundMoveState(currentWaypoint.running());
                    groundPathState = GroundPathState.FOLLOWING;
                    groundPathDebugReason = (path.canReach()
                            ? "following-complete-path"
                            : "following-partial-path")
                            + (detourAllowance > 0 ? "-detour-" + detourAllowance : "");
                }
        );
    }

    private boolean hasUsefulPartialGroundProgress(Path path, Vec3 target) {
        if (path == null || path.getNodeCount() == 0 || target == null) {
            return false;
        }

        Vec3 endpoint = path.getEntityPosAtNode(dragon, path.getNodeCount() - 1);
        double progress = horizontalDistance(dragon.position(), target)
                - horizontalDistance(endpoint, target);
        return progress >= minimumGroundProgress();
    }

    private void completeProgressiveGroundSegment() {
        QueuedWaypoint waypoint = currentWaypoint;
        if (waypoint == null) {
            recordGroundPathFailure(null, "missing-progressive-waypoint");
            return;
        }

        double currentDistance = dragon.position().distanceTo(waypoint.target());
        if (Double.isNaN(groundPathSegmentStartDistance)
                || groundPathSegmentStartDistance - currentDistance < minimumGroundProgress()) {
            recordGroundPathFailure(waypoint.target(), "progressive-segment-no-progress");
            return;
        }

        startGroundPathAsync(waypoint);
    }

    private void completeGroundArrival() {
        currentWaypoint = null;
        groundPathSegmentStartDistance = Double.NaN;
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
        if (!waypoint.hasPreciseGroundArrival()
                || path.getNodeCount() == 0
                || !(dragon.getNavigation() instanceof PathNavigateGround groundNavigation)) {
            return;
        }

        Vec3 endpoint = path.getEntityPosAtNode(dragon, path.getNodeCount() - 1);
        double endpointOffset = horizontalDistance(endpoint, waypoint.target());
        double remainingArrivalRadius = Math.max(
                0.05D,
                waypoint.groundArrivalTolerance() - endpointOffset
        );
        // Path goal accuracy and navigator tolerance share one radial arrival budget.
        groundNavigation.setFinalWaypointTolerance(remainingArrivalRadius / Math.sqrt(2.0D));
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void recordGroundPathFailure(@Nullable Vec3 target, String reason) {
        double resetDistance = Math.max(2.0D, dragon.getBbWidth());
        if (groundPathFailureOrigin == null
                || groundPathFailureOrigin.distanceToSqr(dragon.position())
                > resetDistance * resetDistance) {
            consecutiveGroundPathFailures = 0;
            groundPathFailureOrigin = dragon.position();
        }
        consecutiveGroundPathFailures++;
        currentWaypoint = null;
        groundPathSegmentStartDistance = Double.NaN;
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
        groundPathSegmentStartDistance = Double.NaN;
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
        if (!dragon.level().dimensionType().hasCeiling()) {
            int surfaceY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            return new BlockPos(column.getX(), surfaceY - 1, column.getZ());
        }

        int minY = dragon.level().getMinBuildHeight();
        int maxY = dragon.level().getMaxBuildHeight() - 1;
        int startY = Math.min(maxY, Math.max(minY, originY + 8));
        for (int y = startY; y >= minY; y--) {
            BlockPos ground = new BlockPos(column.getX(), y, column.getZ());
            if (isValidLandingSurface(dragon, ground)) {
                return ground;
            }
        }
        return null;
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
                                  @Nullable DragonLandingPlan landingPlan,
                                  double groundArrivalTolerance) {
        private QueuedWaypoint(Vec3 target, double speed, boolean running, MovementMode mode) {
            this(target, speed, running, mode, null, Double.NaN);
        }

        private QueuedWaypoint(Vec3 target,
                               double speed,
                               boolean running,
                               MovementMode mode,
                               double groundArrivalTolerance) {
            this(target, speed, running, mode, null, groundArrivalTolerance);
        }

        private boolean hasPreciseGroundArrival() {
            return Double.isFinite(groundArrivalTolerance) && groundArrivalTolerance > 0.0D;
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

    private enum GroundPathState {
        IDLE,
        CALCULATING,
        FOLLOWING,
        ARRIVED,
        FAILED
    }
}
