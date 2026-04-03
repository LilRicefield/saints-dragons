package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.core.BlockPos;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import java.util.List;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flying-only port of the Book of Dragons async movement stack.
 */
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
    private final int recalculationInterval = 40;
    private final int maxRetries = 5;
    private final double baseArrivalDistance = 1.5;
    private final int stuckThresholdTicks = 60;
    private final double stuckMovementThreshold = 0.5;
    private final double maxSegmentDistance = 64.0;
    private final double flyingLookAhead = 6.0;

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
        if (this.stuckDetector.isInBackoff() || this.state == PathState.CALCULATING) {
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
        if (distSq <= arrivalDist * arrivalDist) {
            this.onArrived();
            return;
        }

        if (this.state == PathState.FOLLOWING) {
            this.movementExecutor.executeMovement(
                    this.pathResolver.calculateLookAheadPoint(this.flyingLookAhead),
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

    public void setWaypoint(Vec3 target, double speed, WaypointArrivalCallback onArrival) {
        if (this.currentWaypoint != null
                && target.distanceToSqr(this.currentWaypoint) < 1.0
                && (this.state == PathState.CALCULATING || this.state == PathState.FOLLOWING)) {
            this.currentWaypoint = target;
            this.speedModifier = speed;
            this.currentArrivalCallback = onArrival;
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
            this.pathResolver.clearPathNodes();
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
        double widthScale = Math.max(1.0, this.host.getBbWidth());
        double base = landingTarget ? 0.75D : this.baseArrivalDistance;
        return Math.max(0.75D, base * widthScale);
    }

    public PathState getState() {
        return this.state;
    }

    void setState(PathState state) {
        this.state = state;
    }

    public boolean isIdle() {
        return this.state == PathState.IDLE || this.state == PathState.ARRIVED;
    }

    public Vec3 getCurrentWaypoint() {
        return this.currentWaypoint;
    }

    public List<AsyncFlightWaypointQueue.QueuedWaypoint> getQueuedWaypoints() {
        return this.waypointQueue.stream().toList();
    }

    private boolean isLandingTarget(Vec3 waypoint) {
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
