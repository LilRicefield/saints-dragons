package com.leon.saintsdragons.server.ai.navigation;

import com.leon.saintsdragons.server.ai.pathfinding.AsyncDragonPathfinder;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Future;

public class DragonAIMovementController {
    private static final int GROUND_PATH_FAILURE_RETRY_TICKS = 20;

    private final RideableDragonBase dragon;
    private @Nullable QueuedWaypoint currentWaypoint;
    private GroundPathState groundPathState = GroundPathState.IDLE;
    private long groundPathRequestGeneration;
    private @Nullable Future<?> groundPathRequest;
    private int groundPathFailureRetryTicks;
    private @Nullable Vec3 lastFailedGroundTarget;

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
        if (groundPathState == GroundPathState.FOLLOWING && dragon.getNavigation().isDone()) {
            double arrivalDistance = Math.max(1.5D, dragon.getBbWidth() * 0.75D);
            boolean reached = currentWaypoint != null
                    && dragon.distanceToSqr(currentWaypoint.target()) <= arrivalDistance * arrivalDistance;
            if (reached) {
                groundPathState = GroundPathState.ARRIVED;
                currentWaypoint = null;
            } else {
                recordGroundPathFailure(currentWaypoint != null ? currentWaypoint.target() : null);
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
        return target != null && setWaypoint(target.position(), speed);
    }

    public boolean setWaypoint(Vec3 target, double speed) {
        if (target == null || dragon.level().isClientSide) {
            return false;
        }
        return startWaypoint(new QueuedWaypoint(target, speed, false, MovementMode.AUTO));
    }

    public boolean setWaypoint(LivingEntity target, double speed, boolean running) {
        setGroundMoveState(running);
        return setWaypoint(target, speed);
    }

    public boolean setWaypoint(Vec3 target, double speed, boolean running) {
        setGroundMoveState(running);
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

    public boolean moveToProgressiveGroundPosition(Vec3 target, double speed, boolean running) {
        if (target == null || !canUseGroundNavigation()) {
            clearGroundPath();
            return false;
        }
        ensureGroundNavigation();
        return startWaypoint(new QueuedWaypoint(target, speed, running, MovementMode.PROGRESSIVE_GROUND));
    }

    public void setLandingWaypoint(@Nullable LivingEntity target, double speed) {
        trySetLandingWaypoint(target, speed);
    }

    public boolean trySetLandingWaypoint(@Nullable LivingEntity target, double speed) {
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

        Vec3 landingTarget = findLandingTarget(target);
        if (landingTarget == null) {
            return false;
        }
        return trySetLandingWaypoint(landingTarget, speed, flightCapable);
    }

    public boolean trySetLandingWaypoint(@Nullable Vec3 landingTarget, double speed) {
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
        return trySetLandingWaypoint(landingTarget, speed, flightCapable);
    }

    private boolean trySetLandingWaypoint(@Nullable Vec3 landingTarget, double speed, DragonFlightCapable flightCapable) {
        if (landingTarget == null) {
            return false;
        }

        flightCapable.beginAiLanding();
        return startWaypoint(new QueuedWaypoint(landingTarget, speed, false, MovementMode.LANDING));
    }

    public @Nullable Vec3 findLandingTarget(@Nullable LivingEntity target) {
        BlockPos origin = target != null && target.isAlive() ? target.blockPosition() : dragon.blockPosition();
        double currentAltitude = Math.max(0.0D, dragon.getY()
                - dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dragon.getBlockX(), dragon.getBlockZ()));
        double minHorizontalDistanceSqr = currentAltitude > 6.0D
                ? 6.0D * 6.0D
                : 0.0D;

        for (int radius = 0; radius <= 32; radius += 8) {
            for (int attempt = 0; attempt < 14; attempt++) {
                int dx = radius == 0 ? 0 : dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                int dz = radius == 0 ? 0 : dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                if (dx * dx + dz * dz < minHorizontalDistanceSqr) {
                    continue;
                }

                BlockPos column = origin.offset(dx, 0, dz);
                if (!dragon.level().hasChunkAt(column)) {
                    continue;
                }

                BlockPos ground = findLandingGround(dragon, column, origin.getY());
                if (ground != null && isValidLandingSurface(dragon, ground)) {
                    return new Vec3(column.getX() + 0.5D, ground.getY() + 1.0D, column.getZ() + 0.5D);
                }
            }
        }
        return null;
    }

    public @Nullable Vec3 findTacticalLandingTarget(LivingEntity target,
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
                if (isTacticalLandingTargetValid(
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

    public boolean isTacticalLandingTargetValid(Vec3 landingTarget,
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
        currentWaypoint = null;
        resetGroundPathState();
        dragon.getNavigation().stop();
        if (dragon instanceof RideableFlyingDragon flyingDragon) {
            flyingDragon.clearAiFlightTarget();
        }
    }

    public void stop() {
        currentWaypoint = null;
        resetGroundPathState();
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
        currentWaypoint = null;
        resetGroundPathState();
        dragon.getNavigation().stop();
        if (dragon instanceof RideableFlyingDragon flyingDragon) {
            flyingDragon.clearAiFlightTarget();
        }
        if (!dragon.isVehicle()) {
            dragon.setAccelerating(false);
        }
        setGroundIdle();
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
        return groundPathState == GroundPathState.FAILED
                || (!shouldUseAirMovement()
                && canUseGroundNavigation()
                && dragon.getNavigation().isStuck());
    }

    public void clearGroundPathFailureRetry() {
        groundPathFailureRetryTicks = 0;
        lastFailedGroundTarget = null;
    }

    public String getDebugMovementMode() {
        return currentWaypoint == null ? "NONE" : currentWaypoint.mode().name();
    }

    public @Nullable Vec3 getDebugMovementTarget() {
        return currentWaypoint == null ? null : currentWaypoint.target();
    }

    public double getDebugMovementSpeed() {
        return currentWaypoint == null ? 0.0D : currentWaypoint.speed();
    }

    private boolean shouldUseAirMovement() {
        if (dragon instanceof RideableFlyingDragon flyingDragon && flyingDragon.isAiFlightPathing()) {
            return true;
        }
        return dragon.isFlying() || dragon.isTakeoff() || dragon.isHovering() || dragon.isLanding();
    }

    private void clearGroundPath() {
        dragon.getNavigation().stop();
    }

    private boolean startWaypoint(QueuedWaypoint waypoint) {
        if (!waypoint.mode().usesAir()
                && groundPathFailureRetryTicks > 0
                && lastFailedGroundTarget != null
                && lastFailedGroundTarget.distanceToSqr(waypoint.target()) < 1.0D) {
            return false;
        }
        if (!waypoint.mode().usesAir()
                && currentWaypoint != null
                && !currentWaypoint.mode().usesAir()
                && currentWaypoint.target().distanceToSqr(waypoint.target()) < 1.0D
                && (groundPathState == GroundPathState.CALCULATING
                || groundPathState == GroundPathState.FOLLOWING)) {
            currentWaypoint = waypoint;
            return true;
        }

        currentWaypoint = waypoint;
        if (!waypoint.mode().usesGroundPath()) {
            resetGroundPathState();
        }
        if (waypoint.running()) {
            setGroundRun();
        }

        if (waypoint.mode().usesAir() || (waypoint.mode() == MovementMode.AUTO && shouldUseAirMovement())) {
            resetGroundPathState();
            if (dragon instanceof RideableFlyingDragon flyingDragon) {
                flyingDragon.trackAiFlightTarget(waypoint.target(), waypoint.speed());
                return true;
            }
            return dragon.getNavigation().moveTo(waypoint.target().x, waypoint.target().y, waypoint.target().z, waypoint.speed());
        }

        if (!canUseGroundNavigation()) {
            clearGroundPath();
            currentWaypoint = null;
            groundPathState = waypoint.mode().usesGroundPath()
                    ? GroundPathState.FAILED
                    : GroundPathState.IDLE;
            return false;
        }
        startGroundPathAsync(waypoint);
        return true;
    }

    private void startGroundPathAsync(QueuedWaypoint waypoint) {
        ensureGroundNavigation();
        boolean replacingActivePath = groundPathState == GroundPathState.FOLLOWING
                && !dragon.getNavigation().isDone();
        if (!replacingActivePath) {
            dragon.getNavigation().stop();
        }
        long requestGeneration = ++groundPathRequestGeneration;
        groundPathState = GroundPathState.CALCULATING;
        groundPathRequest = AsyncDragonPathfinder.calculateGroundPathAsync(dragon, waypoint.target(), path -> {
            if (requestGeneration != groundPathRequestGeneration
                    || currentWaypoint == null
                    || currentWaypoint.mode().usesAir()
                    || !canUseGroundNavigation()) {
                return;
            }
            groundPathRequest = null;
            if (path == null || path.getNodeCount() == 0) {
                recordGroundPathFailure(currentWaypoint.target());
                return;
            }

            if (!path.canReach() && currentWaypoint.mode().requiresCompletePath()) {
                recordGroundPathFailure(currentWaypoint.target());
                return;
            }

            Path resolvedPath = path;
            boolean started = dragon.getNavigation().moveTo(resolvedPath, currentWaypoint.speed());
            if (!started) {
                recordGroundPathFailure(currentWaypoint.target());
                return;
            }
            groundPathFailureRetryTicks = 0;
            lastFailedGroundTarget = null;
            setGroundMoveState(currentWaypoint.running());
            groundPathState = GroundPathState.FOLLOWING;
        });
    }

    private void recordGroundPathFailure(@Nullable Vec3 target) {
        currentWaypoint = null;
        groundPathState = GroundPathState.FAILED;
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

    private record QueuedWaypoint(Vec3 target, double speed, boolean running, MovementMode mode) {
    }

    private enum MovementMode {
        AUTO,
        GROUND,
        PROGRESSIVE_GROUND,
        LANDING;

        private boolean usesAir() {
            return this == LANDING;
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
