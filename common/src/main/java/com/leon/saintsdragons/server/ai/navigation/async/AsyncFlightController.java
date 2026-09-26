package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import java.util.List;
import net.minecraft.util.Mth;
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
    private @Nullable DragonFlightRequest currentFlightRequest;
    private WaypointArrivalCallback currentArrivalCallback;
    private boolean currentGroundTransition;
    private boolean currentWaterTouchdown;
    private PathState state = PathState.IDLE;
    private double speedModifier = 1.0;
    private long pathRequestGeneration = 0L;
    private final int maxRetries = 5;
    private final double baseArrivalDistance = 1.5;
    private final int stuckThresholdTicks = 20;
    private final double stuckMovementThreshold = 0.5;

    public AsyncFlightController(Mob host) {
        this(host, null, null);
    }

    AsyncFlightController(Mob host, @Nullable AsyncFlightPathResolver.PathRequester requester,
                          @Nullable AsyncFlightPathResolver.SegmentChecker segments) {
        this.host = host;
        this.flightCapable = (DragonFlightCapable) host;
        this.pathResolver = requester == null || segments == null ? new AsyncFlightPathResolver(host, this)
                : new AsyncFlightPathResolver(host, this, requester, segments);
        this.movementExecutor = new AsyncFlightMovementExecutor(host, this.flightCapable);
        this.stuckDetector = new AsyncFlightStuckDetector(host);
    }

    public void serverTick() {
        if (this.host.isVehicle()) {
            return;
        }
        if (this.state == PathState.IDLE || this.state == PathState.ARRIVED || this.state == PathState.FAILED) {
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

        boolean touchdown = this.currentGroundTransition
                && FlightLandingMotion.nearTouchdown(this.host.position(), this.currentWaypoint, this.currentWaterTouchdown);
        if (this.currentGroundTransition && !touchdown && this.flightCapable.isLanding()) {
            this.flightCapable.beginAiFlight();
        }
        if (this.currentGroundTransition && this.hasLandingContact()) {
            this.onArrived();
            return;
        }
        if (this.currentGroundTransition && (touchdown || this.host.tickCount % 10 == 0)
                && !DragonLandingSites.isValid(this.host, this.currentWaypoint)) {
            this.failLanding();
            return;
        }
        double arrivalDist = this.calculateArrivalDistance();
        if (this.hasReachedWaypoint(this.host.position().distanceToSqr(this.currentWaypoint),
                arrivalDist, this.currentGroundTransition)) {
            this.onArrived();
            return;
        }

        if (this.state == PathState.FOLLOWING || this.state == PathState.CALCULATING) {
            double lookAhead = Mth.clamp(6.0D + this.host.getDeltaMovement().length() * 8.0D, 6.0D, 24.0D);
            Vec3 movementTarget;
            if (touchdown) {
                movementTarget = this.pathResolver.calculateSafeDirectLookAhead(this.currentWaypoint, 4.0D);
                if (movementTarget == null) {
                    this.failLanding();
                    return;
                }
            } else {
                movementTarget = this.pathResolver.calculateLookAheadPoint(lookAhead);
                if (movementTarget == null) {
                    movementTarget = this.pathResolver.calculateSafeDirectLookAhead(this.currentWaypoint, lookAhead);
                }
            }
            if (movementTarget != null) {
                this.movementExecutor.executeMovement(movementTarget, this.currentWaypoint,
                        this.speedModifier, arrivalDist, this.waypointQueue.isEmpty(),
                        this.currentFlightRequest == null ? DragonFlightRequest.Arrival.BRAKE : this.currentFlightRequest.arrival(),
                        this.currentFlightRequest != null && this.currentFlightRequest.purpose() == DragonFlightRequest.Purpose.DIVE,
                        touchdown);
                if (touchdown) this.flightCapable.beginAiLanding();
            } else {
                this.movementExecutor.applyIdleFriction();
            }
        }
        if (this.stuckDetector.check(this.state, this.stuckMovementThreshold, this.stuckThresholdTicks)) {
            this.handleStuck(this.currentWaypoint);
        }
        if (this.state == PathState.FAILED) return;
        if (this.state == PathState.FOLLOWING && this.pathResolver.shouldExtendPartialPath()) {
            this.pathResolver.forceRecalculatePath(this.currentWaypoint);
        }
        this.pathResolver.tickPathing(this.currentWaypoint);
    }

    public void setWaypoint(Vec3 target) {
        this.setWaypoint(target, 1.0, null);
    }

    public void setWaypoint(Vec3 target, double speed) {
        this.setWaypoint(target, speed, null);
    }

    /** The movement controller has already selected and validated this touchdown position. */
    public void setGroundTransitionWaypoint(Vec3 target, double speed) {
        this.clearRoute();
        this.currentWaypoint = target;
        this.currentGroundTransition = true;
        this.currentWaterTouchdown = DragonLandingSites.isWaterSurface(this.host, target);
        this.speedModifier = speed;
        this.state = PathState.CALCULATING;
        this.flightCapable.beginAiFlight();
        this.pathResolver.startPathing(target);
    }

    public void trackMovingWaypoint(Vec3 target, double speed) {
        this.requestFlight(DragonFlightRequest.track(target, speed));
    }

    public void setWaypoint(Vec3 target, double speed, WaypointArrivalCallback onArrival) {
        this.requestFlight(DragonFlightRequest.cruise(target, speed), onArrival);
    }

    public void requestFlight(DragonFlightRequest request) {
        this.requestFlight(request, null);
    }

    private void requestFlight(DragonFlightRequest request, @Nullable WaypointArrivalCallback onArrival) {
        this.cancelLandingForNewFlightCommand();
        boolean newObjective = FlightRoutePolicy.isNewObjective(this.host.position(), this.currentFlightRequest, request);
        if (this.state == PathState.FAILED && !newObjective) return;
        if (this.state == PathState.ARRIVED && !newObjective
                && this.host.position().distanceToSqr(request.target()) <= Math.pow(request.arrivalDistance(this.host.getBbWidth()), 2)) {
            this.currentFlightRequest = request;
            this.speedModifier = request.speedModifier();
            return;
        }

        boolean changedPurpose = this.currentFlightRequest != null
                && !FlightRoutePolicy.samePurpose(this.currentFlightRequest.purpose(), request.purpose());
        this.currentFlightRequest = request;
        this.waypointQueue.clear();
        this.currentWaypoint = request.target();
        this.currentArrivalCallback = onArrival;
        this.currentGroundTransition = false;
        this.speedModifier = request.speedModifier();
        // Repeated pursuit updates must not cancel a recovery's backoff or replenish its retry budget.
        if (this.state == PathState.STUCK && !changedPurpose) return;
        if (this.state == PathState.IDLE || this.state == PathState.ARRIVED
                || this.state == PathState.FAILED || this.state == PathState.STUCK) {
            this.stuckDetector.reset();
            this.state = PathState.CALCULATING;
        }
        this.pathResolver.updateTarget(request.target());
    }

    public void addWaypoint(Vec3 target, double speed, WaypointArrivalCallback onArrival) {
        this.cancelLandingForNewFlightCommand();
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
                groundTransition,
                DragonFlightRequest.cruise(target, speed)
        ));
        if (this.state == PathState.IDLE || this.state == PathState.ARRIVED) {
            this.advanceToNextWaypoint();
        }
    }

    public void clearAllWaypoints() {
        this.clearRoute();
        this.movementExecutor.zeroVelocity();
    }

    private void clearRoute() {
        boolean wasLanding = this.currentGroundTransition;
        this.currentFlightRequest = null;
        this.waypointQueue.clear();
        this.currentWaypoint = null;
        this.currentArrivalCallback = null;
        this.currentGroundTransition = false;
        this.state = PathState.IDLE;
        this.currentWaterTouchdown = false;
        this.invalidatePathRequests();
        this.pathResolver.clearPathNodes();
        this.resetPathingState();
        this.movementExecutor.resetSteering();
        if (wasLanding && this.flightCapable.isLanding() && !this.host.onGround()
                && !this.host.isInWaterOrBubble() && !this.host.isInLava()) {
            this.flightCapable.beginAiFlight();
        }
    }

    private void failLanding() {
        this.clearAllWaypoints();
        this.flightCapable.beginAiFlight();
        this.state = PathState.FAILED;
    }

    private void cancelLandingForNewFlightCommand() {
        if (this.currentGroundTransition) this.clearRoute();
        if (this.flightCapable.isLanding()) this.flightCapable.beginAiFlight();
    }

    public void onArrived() {
        if (this.currentGroundTransition) {
            // A route endpoint in the air is never a completed landing.
            if (!this.hasLandingContact()) return;
            boolean waterContact = this.currentWaterTouchdown && this.host.isInWaterOrBubble();
            this.clearAllWaypoints();
            if (waterContact && this.host instanceof RideableFlyingDragon flying) flying.completeAiWaterHandoff();
            else this.flightCapable.completeAiLanding();
            return;
        }
        this.movementExecutor.resetSteering();
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
        this.currentFlightRequest = next.flightRequest();
        this.resetPathingState();
        this.pathResolver.startPathing(this.currentWaypoint);
    }

    public void handleStuck(Vec3 currentWaypoint) {
        if (this.currentGroundTransition) {
            this.failLanding();
            return;
        }
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
            this.pathResolver.cancelActivePathRequest();
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
        if (this.currentFlightRequest != null) return this.currentFlightRequest.arrivalDistance(this.host.getBbWidth());
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
            return this.hasLandingContact();
        }
        return distSq <= arrivalDist * arrivalDist;
    }

    private boolean hasLandingContact() {
        return this.host.onGround() || this.currentWaterTouchdown
                && this.host.isInWaterOrBubble() && !this.host.isInLava();
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

    public boolean isSprinting() {
        return (this.state == PathState.FOLLOWING || this.state == PathState.CALCULATING)
                && !this.currentGroundTransition && !this.host.isVehicle()
                && !this.flightCapable.isTakeoff() && !this.flightCapable.isLanding()
                && this.currentFlightRequest != null && this.currentFlightRequest.requestsSprint()
                && this.host.getDeltaMovement().length() > this.flightCapable.getFlightSpeed() * 1.1D;
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

    public String getSteeringDebugSummary() {
        return this.movementExecutor.steeringSummary()
                + ",landing=" + this.currentGroundTransition + ",takeoff=" + this.flightCapable.isTakeoff()
                + (this.currentFlightRequest == null ? "" : ",purpose=" + this.currentFlightRequest.purpose()
                    + ",arrival=" + this.currentFlightRequest.arrival()) + ",speed=" + this.speedModifier
                + (this.currentGroundTransition ? ",touchdown=" + this.currentWaypoint
                    + ",surface=" + (this.currentWaterTouchdown ? "water" : "ground") : "");
    }

    public DebugSnapshot getDebugSnapshot() {
        return new DebugSnapshot(
                this.state,
                this.currentWaypoint,
                this.pathResolver.getDebugPathNodes(),
                this.pathResolver.getDebugCurrentPathIndex()
        );
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
