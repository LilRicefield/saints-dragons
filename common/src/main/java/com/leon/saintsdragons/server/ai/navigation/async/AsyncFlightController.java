package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.core.BlockPos;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import java.util.List;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncFlightController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncFlightController.class);

    private final Mob host;
    private final AsyncFlightWaypointQueue waypointQueue = new AsyncFlightWaypointQueue();
    private final AsyncFlightPathResolver pathResolver;
    private final AsyncFlightMovementExecutor movementExecutor;
    private final AsyncFlightStuckDetector stuckDetector;

    private Vec3 currentWaypoint;
    private WaypointArrivalCallback currentArrivalCallback;
    private PathState state = PathState.IDLE;
    private double speedModifier = 1.0;
    private long pathRequestGeneration = 0L;
    private final int recalculationInterval = 40;
    private final int maxRetries = 5;
    private final double baseArrivalDistance = 1.5;
    private final int stuckThresholdTicks = 20;
    private final double stuckMovementThreshold = 0.5;
    private final double maxSegmentDistance = 64.0;
    private final double flyingLookAhead = 6.0;
    private final double liveRetargetRefreshDistanceSq = 16.0D;
    private final double liveRetargetMeaningfulVerticalDelta = 2.0D;
    private final double liveRetargetMeaningfulHeadingDot = 0.75D;

    public AsyncFlightController(Mob host) {
        this.host = host;
        this.pathResolver = new AsyncFlightPathResolver(host, this);
        this.movementExecutor = new AsyncFlightMovementExecutor(host, (DragonFlightCapable) host);
        this.stuckDetector = new AsyncFlightStuckDetector(host);
    }

    public void serverTick() {
        if (this.host.isVehicle()) {
            return;
        }
        if (this.state == PathState.IDLE || this.state == PathState.FAILED) {
            this.movementExecutor.applyIdleFriction();
            return;
        }
        this.stuckDetector.tickBackoff();
        if (this.stuckDetector.isInBackoff()) {
            return;
        }
        if (this.currentWaypoint == null) {
            if (!this.waypointQueue.isEmpty()) {
                this.advanceToNextWaypoint();
            } else {
                this.state = PathState.IDLE;
                this.movementExecutor.applyIdleFriction();
            }
            return;
        }

        boolean landingTarget = this.isLandingTarget(this.currentWaypoint);
        double arrivalDist = this.calculateArrivalDistance(landingTarget);
        double distSq = this.host.position().distanceToSqr(this.currentWaypoint);
        if (this.hasReachedWaypoint(distSq, arrivalDist, landingTarget)) {
            this.onArrived();
            return;
        }

        if (this.state == PathState.FOLLOWING || this.state == PathState.CALCULATING) {
            this.movementExecutor.executeMovement(
                    this.state == PathState.CALCULATING
                            ? this.currentWaypoint
                            : this.pathResolver.calculateLookAheadPoint(this.flyingLookAhead),
                    this.currentWaypoint,
                    this.speedModifier,
                    arrivalDist,
                    this.waypointQueue.isEmpty(),
                    landingTarget
            );
        }

        if (this.stuckDetector.check(this.state, this.stuckMovementThreshold, this.stuckThresholdTicks)) {
            this.handleStuck(this.currentWaypoint);
        }
        if (this.state == PathState.FAILED) {
            return;
        }

        this.pathResolver.tickRecalc();
        if (this.pathResolver.getTicksSinceRecalc() >= this.recalculationInterval) {
            this.pathResolver.recalculatePath(this.currentWaypoint);
        }
    }

    public void setWaypoint(Vec3 target) {
        this.setWaypoint(target, 1.0, null);
    }

    public void setWaypoint(Vec3 target, double speed) {
        this.setWaypoint(target, speed, null);
    }

    public void trackMovingWaypoint(Vec3 target, double speed) {
        if (this.currentWaypoint == null
                || this.state == PathState.IDLE
                || this.state == PathState.ARRIVED
                || this.state == PathState.FAILED
                || this.isLandingTarget(target)) {
            this.setWaypoint(target, speed, null);
            return;
        }

        this.waypointQueue.clear();
        this.currentWaypoint = target;
        this.currentArrivalCallback = null;
        this.speedModifier = speed;
        this.state = PathState.FOLLOWING;
        this.invalidatePathRequests();
        this.pathResolver.clearPathNodes();
        this.stuckDetector.reset();
    }

    public void setWaypoint(Vec3 target, double speed, WaypointArrivalCallback onArrival) {
        if (this.currentWaypoint != null
                && target.distanceToSqr(this.currentWaypoint) < 1.0
                && (this.state == PathState.CALCULATING || this.state == PathState.FOLLOWING)) {
            this.currentWaypoint = target;
            this.speedModifier = speed;
            this.currentArrivalCallback = onArrival;
            return;
        }

        double retargetDistSq = this.currentWaypoint == null ? -1.0D : target.distanceToSqr(this.currentWaypoint);
        if (this.currentWaypoint != null
                && retargetDistSq >= 1.0D
                && retargetDistSq < this.liveRetargetRefreshDistanceSq
                && (this.state == PathState.CALCULATING || this.state == PathState.FOLLOWING)) {
            Vec3 previousWaypoint = this.currentWaypoint;
            boolean forceRecalculate = this.shouldForceRecalculateForRetarget(previousWaypoint, target);
            this.currentWaypoint = target;
            this.speedModifier = speed;
            this.currentArrivalCallback = onArrival;
            if (forceRecalculate) {
                this.resetPathingState();
                this.pathResolver.forceRecalculatePath(target);
            }
            return;
        }

        this.waypointQueue.clear();
        double distToTarget = target.distanceTo(this.host.position());
        if (distToTarget > this.maxSegmentDistance) {
            this.resetPathingState();
            this.state = PathState.IDLE;
            this.currentWaypoint = null;
            this.pathResolver.clearPathNodes();

            Vec3 startPos = this.host.position();
            Vec3 direction = target.subtract(startPos).normalize();
            int segments = (int) (distToTarget / this.maxSegmentDistance);
            for (int i = 1; i <= segments; ++i) {
                Vec3 segmentPos = startPos.add(direction.scale(i * this.maxSegmentDistance));
                this.addWaypoint(segmentPos, speed, null);
            }
            this.addWaypoint(target, speed, onArrival);
            return;
        }

        this.currentWaypoint = target;
        this.currentArrivalCallback = onArrival;
        this.speedModifier = speed;
        this.resetPathingState();
        this.pathResolver.startPathing(this.currentWaypoint);
    }

    public void addWaypoint(Vec3 target, double speed, WaypointArrivalCallback onArrival) {
        this.waypointQueue.add(new AsyncFlightWaypointQueue.QueuedWaypoint(target, speed, onArrival));
        if (this.state == PathState.IDLE || this.state == PathState.ARRIVED) {
            this.advanceToNextWaypoint();
        }
    }

    public void clearAllWaypoints() {
        this.waypointQueue.clear();
        this.currentWaypoint = null;
        this.currentArrivalCallback = null;
        this.state = PathState.IDLE;
        this.invalidatePathRequests();
        this.pathResolver.clearPathNodes();
        this.resetPathingState();
        this.movementExecutor.zeroVelocity();
    }

    public void onArrived() {
        this.pathResolver.clearPathNodes();
        this.state = PathState.ARRIVED;
        if (this.currentArrivalCallback != null) {
            try {
                this.currentArrivalCallback.onArrival(this.host);
            } catch (Exception exception) {
                LOGGER.error("Async flight arrival callback failed for {}", this.host.getStringUUID(), exception);
            }
            this.currentArrivalCallback = null;
        }

        this.currentWaypoint = null;
        if (!this.waypointQueue.isEmpty()) {
            this.advanceToNextWaypoint();
        }
    }

    public void advanceToNextWaypoint() {
        if (this.waypointQueue.isEmpty()) {
            this.state = PathState.IDLE;
            return;
        }

        AsyncFlightWaypointQueue.QueuedWaypoint next = this.waypointQueue.poll();
        this.currentWaypoint = next.position();
        this.currentArrivalCallback = next.onArrival();
        this.speedModifier = next.speed();
        this.resetPathingState();
        this.pathResolver.startPathing(this.currentWaypoint);
    }

    public void handleStuck(Vec3 currentWaypoint) {
        AsyncFlightStuckDetector.StuckAction action = this.stuckDetector.handleStuck(this.maxRetries);
        if (action == AsyncFlightStuckDetector.StuckAction.FAILED) {
            this.state = PathState.FAILED;
            this.currentWaypoint = null;
            this.currentArrivalCallback = null;
            this.waypointQueue.clear();
            this.invalidatePathRequests();
            this.pathResolver.clearPathNodes();
            this.movementExecutor.zeroVelocity();
        } else if (currentWaypoint != null) {
            this.state = PathState.STUCK;
            this.pathResolver.startFlyingPathAsync(currentWaypoint);
        }
    }

    private void resetPathingState() {
        this.pathResolver.reset();
        this.stuckDetector.reset();
    }

    public double calculateArrivalDistance() {
        return this.calculateArrivalDistance(this.isLandingTarget(this.currentWaypoint));
    }

    public double calculateArrivalDistance(boolean landingTarget) {
        if (landingTarget) {
            return 1.0D;
        }
        double width = this.host.getBbWidth();
        // Use square root scaling for large dragons to prevent excessive arrival distances
        // Small dragons (width <= 2): ~1.5-3.0 blocks
        // Medium dragons (width 4): ~4.5 blocks
        // Large dragons (width 8): ~6.4 blocks instead of 12.0
        double widthScale = Math.max(1.0, Math.sqrt(width * 2.0));
        return Math.max(0.75D, this.baseArrivalDistance * widthScale);
    }

    public PathState getState() {
        return this.state;
    }

    boolean hasReachedWaypoint(double distSq, double arrivalDist, boolean landingTarget) {
        if (landingTarget) {
            return this.host.onGround();
        }
        return distSq <= arrivalDist * arrivalDist;
    }

    void setState(PathState state) {
        this.state = state;
    }

    long beginPathRequest() {
        return ++this.pathRequestGeneration;
    }

    void invalidatePathRequests() {
        this.pathRequestGeneration++;
    }

    boolean isPathRequestCurrent(long requestGeneration) {
        return this.pathRequestGeneration == requestGeneration;
    }

    public boolean isIdle() {
        return this.state == PathState.IDLE
                || this.state == PathState.ARRIVED
                || this.state == PathState.FAILED;
    }

    public Vec3 getCurrentWaypoint() {
        return this.currentWaypoint;
    }

    public List<AsyncFlightWaypointQueue.QueuedWaypoint> getQueuedWaypoints() {
        return this.waypointQueue.stream().toList();
    }

    private boolean shouldForceRecalculateForRetarget(Vec3 previousTarget, Vec3 newTarget) {
        if (previousTarget == null) {
            return false;
        }

        if (this.isLandingTarget(previousTarget) != this.isLandingTarget(newTarget)) {
            return true;
        }

        if (Math.abs(newTarget.y - previousTarget.y) >= this.liveRetargetMeaningfulVerticalDelta) {
            return true;
        }

        Vec3 currentDirection = horizontalDirectionTo(previousTarget);
        Vec3 newDirection = horizontalDirectionTo(newTarget);
        if (currentDirection == null || newDirection == null) {
            return false;
        }

        return currentDirection.dot(newDirection) < this.liveRetargetMeaningfulHeadingDot;
    }

    private Vec3 horizontalDirectionTo(Vec3 target) {
        Vec3 horizontal = target.subtract(this.host.position());
        horizontal = new Vec3(horizontal.x, 0.0D, horizontal.z);
        if (horizontal.lengthSqr() < 1.0D) {
            return null;
        }
        return horizontal.normalize();
    }

    boolean isLandingTarget(Vec3 waypoint) {
        if (waypoint == null) {
            return false;
        }

        BlockPos groundCheck = BlockPos.containing(waypoint.x, waypoint.y, waypoint.z);
        for (int depth = 0; depth < 3; depth++) {
            BlockPos checkPos = groundCheck.below(depth);
            if (!this.host.level().hasChunkAt(checkPos)) {
                continue;
            }

            var state = this.host.level().getBlockState(checkPos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            if (!state.getCollisionShape((BlockGetter) this.host.level(), checkPos).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public interface WaypointArrivalCallback {
        void onArrival(Mob dragon);
    }

    public enum PathState {
        IDLE,
        CALCULATING,
        FOLLOWING,
        ARRIVED,
        STUCK,
        FAILED
    }
}
