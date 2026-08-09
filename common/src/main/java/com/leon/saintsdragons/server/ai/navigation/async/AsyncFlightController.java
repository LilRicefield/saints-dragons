package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import java.util.List;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncFlightController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncFlightController.class);

    private final Mob host;
    private final DragonFlightCapable flightCapable;
    private final AsyncFlightWaypointQueue waypointQueue = new AsyncFlightWaypointQueue();
    private final AsyncFlightPathResolver pathResolver;
    private final AsyncFlightMovementExecutor movementExecutor;
    private final AsyncFlightStuckDetector stuckDetector;

    private Vec3 currentWaypoint;
    private WaypointArrivalCallback currentArrivalCallback;
    private boolean currentGroundTransition;
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
        this.flightCapable = (DragonFlightCapable) host;
        this.pathResolver = new AsyncFlightPathResolver(host, this);
        this.movementExecutor = new AsyncFlightMovementExecutor(host, this.flightCapable);
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
        if (this.state == PathState.STUCK) {
            if (!this.stuckDetector.isInBackoff()) {
                if (this.currentWaypoint == null) {
                    this.state = PathState.FAILED;
                } else {
                    this.pathResolver.startFlyingPathAsync(this.currentWaypoint);
                }
            }
            return;
        }
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

        boolean groundTransition = this.currentGroundTransition;
        if (groundTransition && this.flightCapable.isLanding() && this.movementExecutor.hasLandingContact()) {
            this.clearAllWaypoints();
            this.flightCapable.markLandedNow();
            return;
        }
        double arrivalDist = this.calculateArrivalDistance(groundTransition);
        double distSq = this.host.position().distanceToSqr(this.currentWaypoint);
        if (this.hasReachedWaypoint(distSq, arrivalDist, groundTransition)) {
            this.onArrived();
            return;
        }

        if (this.state == PathState.FOLLOWING || this.state == PathState.CALCULATING) {
            Vec3 movementTarget = this.pathResolver.calculateLookAheadPoint(this.flyingLookAhead);
            if (movementTarget == null) {
                movementTarget = this.pathResolver.calculateSafeDirectLookAhead(
                        this.currentWaypoint,
                        this.flyingLookAhead
                );
            }
            if (movementTarget != null) {
                this.movementExecutor.executeMovement(
                        movementTarget,
                        this.currentWaypoint,
                        this.speedModifier,
                        arrivalDist,
                        this.waypointQueue.isEmpty(),
                        groundTransition
                );
            } else {
                this.movementExecutor.applyIdleFriction();
            }
        }

        if (this.stuckDetector.check(this.state, this.stuckMovementThreshold, this.stuckThresholdTicks)) {
            this.handleStuck(this.currentWaypoint);
        }
        if (this.state == PathState.FAILED) {
            return;
        }

        if (this.state == PathState.FOLLOWING && this.pathResolver.shouldExtendPartialPath()) {
            this.pathResolver.forceRecalculatePath(this.currentWaypoint);
        } else {
            this.pathResolver.tickRecalc();
        }
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

    public void setGroundTransitionWaypoint(Vec3 target, double speed) {
        this.setWaypoint(target, speed, null, true);
    }

    public void trackMovingWaypoint(Vec3 target, double speed) {
        if (this.currentWaypoint == null
                || this.state == PathState.IDLE
                || this.state == PathState.ARRIVED
                || this.state == PathState.FAILED
                || this.state == PathState.STUCK
                || this.currentGroundTransition) {
            this.setWaypoint(target, speed, null);
            return;
        }

        this.waypointQueue.clear();
        this.currentWaypoint = target;
        this.currentArrivalCallback = null;
        this.currentGroundTransition = false;
        this.speedModifier = speed;
        if (this.pathResolver.needsRefresh(target, this.liveRetargetRefreshDistanceSq)) {
            if (!this.pathResolver.hasActivePathRequest()) {
                this.pathResolver.forceRecalculatePath(target);
            }
        } else if (!this.pathResolver.retargetPathEndpoint(target)
                && !this.pathResolver.hasActivePathRequest()) {
            this.pathResolver.forceRecalculatePath(target);
        }
    }

    public void setWaypoint(Vec3 target, double speed, WaypointArrivalCallback onArrival) {
        this.setWaypoint(target, speed, onArrival, false);
    }

    private void setWaypoint(Vec3 target,
                             double speed,
                             WaypointArrivalCallback onArrival,
                             boolean groundTransition) {
        if (this.currentWaypoint != null
                && target.distanceToSqr(this.currentWaypoint) < 1.0
                && (this.state == PathState.CALCULATING || this.state == PathState.FOLLOWING)) {
            this.currentWaypoint = target;
            this.speedModifier = speed;
            this.currentArrivalCallback = onArrival;
            this.currentGroundTransition = groundTransition;
            if (this.state == PathState.FOLLOWING && !this.pathResolver.retargetPathEndpoint(target)
                    && !this.pathResolver.hasActivePathRequest()) {
                this.pathResolver.forceRecalculatePath(target);
            }
            return;
        }

        double retargetDistSq = this.currentWaypoint == null ? -1.0D : target.distanceToSqr(this.currentWaypoint);
        if (this.currentWaypoint != null
                && retargetDistSq >= 1.0D
                && retargetDistSq < this.liveRetargetRefreshDistanceSq
                && (this.state == PathState.CALCULATING || this.state == PathState.FOLLOWING)) {
            Vec3 previousWaypoint = this.currentWaypoint;
            boolean forceRecalculate = this.shouldForceRecalculateForRetarget(
                    previousWaypoint,
                    this.currentGroundTransition,
                    target,
                    groundTransition
            );
            this.currentWaypoint = target;
            this.speedModifier = speed;
            this.currentArrivalCallback = onArrival;
            this.currentGroundTransition = groundTransition;
            if (forceRecalculate && !this.pathResolver.hasActivePathRequest()) {
                this.pathResolver.forceRecalculatePath(target);
            } else if (this.state == PathState.FOLLOWING) {
                if (!this.pathResolver.retargetPathEndpoint(target)
                        && !this.pathResolver.hasActivePathRequest()) {
                    this.pathResolver.forceRecalculatePath(target);
                }
            }
            return;
        }

        this.waypointQueue.clear();
        double distToTarget = target.distanceTo(this.host.position());
        if (distToTarget > this.maxSegmentDistance) {
            this.resetPathingState();
            this.state = PathState.IDLE;
            this.currentWaypoint = null;
            this.currentGroundTransition = false;
            this.pathResolver.clearPathNodes();

            Vec3 startPos = this.host.position();
            Vec3 direction = target.subtract(startPos).normalize();
            int segments = (int) Math.floor(distToTarget / this.maxSegmentDistance);
            for (int i = 1; i <= segments && i * this.maxSegmentDistance < distToTarget; ++i) {
                Vec3 segmentPos = startPos.add(direction.scale(i * this.maxSegmentDistance));
                this.addWaypoint(segmentPos, speed, null, false);
            }
            this.addWaypoint(target, speed, onArrival, groundTransition);
            return;
        }

        this.currentWaypoint = target;
        this.currentArrivalCallback = onArrival;
        this.currentGroundTransition = groundTransition;
        this.speedModifier = speed;
        this.resetPathingState();
        this.pathResolver.startPathing(this.currentWaypoint);
    }

    public void addWaypoint(Vec3 target, double speed, WaypointArrivalCallback onArrival) {
        this.addWaypoint(target, speed, onArrival, false);
    }

    private void addWaypoint(Vec3 target,
                             double speed,
                             WaypointArrivalCallback onArrival,
                             boolean groundTransition) {
        this.waypointQueue.add(new AsyncFlightWaypointQueue.QueuedWaypoint(
                target,
                speed,
                onArrival,
                groundTransition
        ));
        if (this.state == PathState.IDLE || this.state == PathState.ARRIVED) {
            this.advanceToNextWaypoint();
        }
    }

    public void clearAllWaypoints() {
        this.waypointQueue.clear();
        this.currentWaypoint = null;
        this.currentArrivalCallback = null;
        this.currentGroundTransition = false;
        this.state = PathState.IDLE;
        this.invalidatePathRequests();
        this.pathResolver.clearPathNodes();
        this.resetPathingState();
        this.movementExecutor.zeroVelocity();
    }

    public void onArrived() {
        this.invalidatePathRequests();
        this.pathResolver.cancelActivePathRequest();
        this.pathResolver.clearPathNodes();
        this.state = PathState.ARRIVED;
        WaypointArrivalCallback arrivalCallback = this.currentArrivalCallback;
        this.currentArrivalCallback = null;
        this.currentWaypoint = null;
        this.currentGroundTransition = false;
        if (arrivalCallback != null) {
            try {
                arrivalCallback.onArrival(this.host);
            } catch (Exception exception) {
                LOGGER.error("Async flight arrival callback failed for {}", this.host.getStringUUID(), exception);
            }
        }

        if (this.currentWaypoint == null && !this.waypointQueue.isEmpty()) {
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
        this.currentGroundTransition = next.groundTransition();
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
            this.currentGroundTransition = false;
            this.waypointQueue.clear();
            this.invalidatePathRequests();
            this.pathResolver.cancelActivePathRequest();
            this.pathResolver.clearPathNodes();
            this.movementExecutor.zeroVelocity();
        } else if (currentWaypoint != null) {
            this.state = PathState.STUCK;
            this.pathResolver.clearPathNodes();
        }
    }

    private void resetPathingState() {
        this.pathResolver.reset();
        this.stuckDetector.reset();
    }

    public double calculateArrivalDistance() {
        return this.calculateArrivalDistance(this.currentGroundTransition);
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

    public boolean hasFailed() {
        return this.state == PathState.FAILED;
    }

    public Vec3 getCurrentWaypoint() {
        return this.currentWaypoint;
    }

    boolean isGroundTransition() {
        return this.currentGroundTransition;
    }

    public List<AsyncFlightWaypointQueue.QueuedWaypoint> getQueuedWaypoints() {
        return this.waypointQueue.stream().toList();
    }

    public DebugSnapshot getDebugSnapshot() {
        return new DebugSnapshot(
                this.state,
                this.currentWaypoint,
                this.pathResolver.getDebugPathNodes(),
                this.pathResolver.getDebugCurrentPathIndex()
        );
    }

    private boolean shouldForceRecalculateForRetarget(Vec3 previousTarget,
                                                      boolean previousGroundTransition,
                                                      Vec3 newTarget,
                                                      boolean newGroundTransition) {
        if (previousTarget == null) {
            return false;
        }

        if (previousGroundTransition != newGroundTransition) {
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

    public interface WaypointArrivalCallback {
        void onArrival(Mob dragon);
    }

    public record DebugSnapshot(PathState state,
                                @Nullable Vec3 waypoint,
                                List<Vec3> pathNodes,
                                int pathIndex) {
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
