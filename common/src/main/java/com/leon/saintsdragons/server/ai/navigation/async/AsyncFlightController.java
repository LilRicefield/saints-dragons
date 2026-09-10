package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncFlightController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncFlightController.class);
    private static final double MIN_LANDING_SPEED_MODIFIER = 1.0D;
    private static final double LANDING_SPEED_BOOST = 1.15D;

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
    private @Nullable DragonLandingPlan landingPlan;
    private LandingPhase landingPhase = LandingPhase.NONE;
    private double landingSpeed = 1.0D;
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

        LandingPhase activeLandingPhase = this.landingPhase;
        boolean groundTransition = this.currentGroundTransition;
        if (activeLandingPhase.isCommitted()
                && this.flightCapable.isLanding()
                && (this.host.onGround()
                    || (activeLandingPhase == LandingPhase.TOUCHDOWN
                        && this.movementExecutor.hasLandingContact()))) {
            this.clearAllWaypoints();
            this.flightCapable.completeAiLanding();
            return;
        }
        double arrivalDist = this.calculateArrivalDistance(activeLandingPhase, groundTransition);
        double distSq = this.host.position().distanceToSqr(this.currentWaypoint);
        if (this.hasReachedWaypoint(distSq, arrivalDist, activeLandingPhase, groundTransition)) {
            if (activeLandingPhase.advancesLandingPlan()) {
                this.advanceLandingPhase();
                if (this.currentWaypoint == null) return;
                activeLandingPhase = this.landingPhase;
                groundTransition = this.currentGroundTransition;
                arrivalDist = this.calculateArrivalDistance(activeLandingPhase, groundTransition);
            } else {
                this.onArrived();
                return;
            }
        }

        if (this.state == PathState.FOLLOWING || this.state == PathState.CALCULATING) {
            Vec3 movementTarget;
            if (activeLandingPhase.usesDirectCorridor()) {
                if (!this.isCurrentLandingSegmentClear()) {
                    this.beginLandingGoAround();
                    return;
                }
                movementTarget = this.currentWaypoint;
            } else {
                double lookAhead = Mth.clamp(6.0D + this.host.getDeltaMovement().length() * 8.0D,
                        6.0D, 24.0D);
                movementTarget = this.pathResolver.calculateLookAheadPoint(lookAhead);
                if (movementTarget == null) {
                    movementTarget = this.pathResolver.calculateSafeDirectLookAhead(
                            this.currentWaypoint,
                            lookAhead
                    );
                }
            }
            if (movementTarget != null) {
                this.movementExecutor.executeMovement(
                        movementTarget,
                        this.currentWaypoint,
                        this.speedModifier,
                        arrivalDist,
                        this.waypointQueue.isEmpty(),
                        this.currentFlightRequest == null ? DragonFlightRequest.Arrival.BRAKE : this.currentFlightRequest.arrival(),
                        this.currentFlightRequest != null
                                && this.currentFlightRequest.purpose() == DragonFlightRequest.Purpose.DIVE,
                        activeLandingPhase
                );
            } else {
                this.movementExecutor.applyIdleFriction();
            }
        }

        if (this.stuckDetector.check(this.state, this.stuckMovementThreshold, this.stuckThresholdTicks)) {
            if (activeLandingPhase.isCommitted()) {
                this.beginLandingGoAround();
            } else {
                this.handleStuck(this.currentWaypoint);
            }
        }
        if (this.state == PathState.FAILED) {
            return;
        }

        if (activeLandingPhase.usesDirectCorridor()) {
            return;
        }
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

    public void setGroundTransitionWaypoint(Vec3 target, double speed) {
        DragonLandingPlan plan = DragonLandingPlanner.findPlanNear(this.host, target);
        if (plan != null) {
            this.setLandingPlan(plan, speed);
        }
    }

    public void setLandingPlan(DragonLandingPlan plan, double speed) {
        if (plan == null) {
            return;
        }
        this.movementExecutor.resetSteering();

        this.waypointQueue.clear();
        this.invalidatePathRequests();
        this.pathResolver.cancelActivePathRequest();
        this.pathResolver.clearPathNodes();
        this.landingPlan = plan;
        this.landingPhase = LandingPhase.APPROACH;
        this.landingSpeed = Math.max(MIN_LANDING_SPEED_MODIFIER, speed * LANDING_SPEED_BOOST);
        this.currentWaypoint = plan.approach();
        this.currentFlightRequest = null;
        this.currentArrivalCallback = null;
        this.currentGroundTransition = false;
        this.speedModifier = this.landingSpeed;
        this.state = PathState.CALCULATING;
        this.stuckDetector.reset();
        this.flightCapable.beginAiFlight();
        this.pathResolver.startPathing(this.currentWaypoint);
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
        this.cancelLandingPlanForNewFlightCommand();
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
        this.cancelLandingPlanForNewFlightCommand();
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
        this.currentFlightRequest = null;
        this.waypointQueue.clear();
        this.currentWaypoint = null;
        this.currentArrivalCallback = null;
        this.currentGroundTransition = false;
        this.clearLandingPlanState();
        this.state = PathState.IDLE;
        this.invalidatePathRequests();
        this.pathResolver.clearPathNodes();
        this.resetPathingState();
        this.movementExecutor.zeroVelocity();
    }

    private void advanceLandingPhase() {
        if (this.landingPlan == null
                || !DragonLandingPlanner.isTouchdownStillValid(this.host, this.landingPlan)) {
            this.beginLandingGoAround();
            return;
        }

        switch (this.landingPhase) {
            case APPROACH -> {
                this.flightCapable.beginAiLanding();
                this.beginDirectLandingPhase(LandingPhase.GLIDE, this.landingPlan.glide());
            }
            case GLIDE -> this.beginDirectLandingPhase(LandingPhase.FLARE, this.landingPlan.flare());
            case FLARE -> this.beginDirectLandingPhase(LandingPhase.TOUCHDOWN, this.landingPlan.touchdown());
            default -> this.beginLandingGoAround();
        }
    }

    private void beginDirectLandingPhase(LandingPhase phase, Vec3 target) {
        this.movementExecutor.resetSteering();
        this.invalidatePathRequests();
        this.pathResolver.cancelActivePathRequest();
        this.pathResolver.clearPathNodes();
        this.landingPhase = phase;
        this.currentWaypoint = target;
        this.currentArrivalCallback = null;
        this.currentGroundTransition = phase == LandingPhase.TOUCHDOWN;
        this.speedModifier = this.landingSpeed;
        this.state = PathState.FOLLOWING;
        this.stuckDetector.reset();
    }

    private void beginLandingGoAround() {
        Vec3 horizontalVelocity = this.host.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        Vec3 heading;
        if (horizontalVelocity.lengthSqr() > 0.04D) {
            heading = horizontalVelocity.normalize();
        } else {
            double yawRadians = Math.toRadians(this.host.getYRot());
            heading = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        }

        double climbDistance = Math.max(8.0D, this.host.getBbWidth() * 2.0D);
        double climbHeight = Math.max(6.0D, this.host.getBbHeight() * 1.5D);
        Vec3 climbTarget = this.host.position()
                .add(heading.scale(climbDistance))
                .add(0.0D, climbHeight, 0.0D);
        DragonFlightSpace space = this.host instanceof RideableDragonBase dragon
                ? dragon.getAIMovement().flightSpace() : new DragonFlightSpace(this.host);
        climbTarget = space.fitDestination(climbTarget);
        if (climbTarget == null || !space.corridorClear(this.host.position(), climbTarget)) {
            climbTarget = space.findCruiseTarget(360.0D, 8.0D, 12.0D, 12.0D, true);
        }
        if (climbTarget == null) {
            // Leave replanning to the brain without forcing an unchecked climb into a roof.
            this.clearAllWaypoints();
            this.flightCapable.beginAiFlight();
            this.state = PathState.FAILED;
            return;
        }

        this.invalidatePathRequests();
        this.pathResolver.cancelActivePathRequest();
        this.pathResolver.clearPathNodes();
        this.landingPlan = null;
        this.landingPhase = LandingPhase.GO_AROUND;
        this.currentWaypoint = climbTarget;
        this.currentGroundTransition = false;
        this.speedModifier = Math.max(0.8D, this.landingSpeed);
        this.currentArrivalCallback = dragon -> {
            this.clearLandingPlanState();
            this.flightCapable.beginAiFlight();
            this.state = PathState.FAILED;
        };
        this.state = PathState.CALCULATING;
        this.stuckDetector.reset();
        this.pathResolver.startPathing(climbTarget);
    }

    private boolean isCurrentLandingSegmentClear() {
        if (this.currentWaypoint == null) {
            return false;
        }
        return VoxelAabbSweeper.isClear(
                this.host.level(),
                this.host,
                this.host.getBoundingBox(),
                this.currentWaypoint.subtract(this.host.position())
        );
    }

    private void cancelLandingPlanForNewFlightCommand() {
        if (this.landingPhase == LandingPhase.NONE) {
            return;
        }
        this.movementExecutor.resetSteering();

        this.waypointQueue.clear();
        this.currentWaypoint = null;
        this.currentArrivalCallback = null;
        this.currentGroundTransition = false;
        this.state = PathState.IDLE;
        this.invalidatePathRequests();
        this.pathResolver.cancelActivePathRequest();
        this.pathResolver.clearPathNodes();
        this.stuckDetector.reset();
        this.clearLandingPlanState();
        this.currentFlightRequest = null;
        if (this.flightCapable.isLanding()) {
            this.flightCapable.beginAiFlight();
        }
    }

    private void clearLandingPlanState() {
        this.landingPlan = null;
        this.landingPhase = LandingPhase.NONE;
        this.landingSpeed = 1.0D;
    }

    public void onArrived() {
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
        AsyncFlightStuckDetector.StuckAction action = this.stuckDetector.handleStuck(this.maxRetries);
        if (action == AsyncFlightStuckDetector.StuckAction.FAILED) {
            boolean abandonedLanding = this.landingPhase != LandingPhase.NONE;
            this.state = PathState.FAILED;
            this.currentWaypoint = null;
            this.currentArrivalCallback = null;
            this.currentGroundTransition = false;
            this.clearLandingPlanState();
            this.waypointQueue.clear();
            this.invalidatePathRequests();
            this.pathResolver.cancelActivePathRequest();
            this.pathResolver.clearPathNodes();
            this.movementExecutor.zeroVelocity();
            if (abandonedLanding && this.flightCapable.isLanding()) {
                this.flightCapable.beginAiFlight();
            }
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

    private double calculateArrivalDistance(LandingPhase phase, boolean groundTransition) {
        return switch (phase) {
            case GLIDE -> Math.max(1.5D, this.host.getBbWidth() * 0.5D);
            case FLARE -> Math.max(0.9D, this.host.getBbWidth() * 0.3D);
            case TOUCHDOWN -> 1.0D;
            default -> this.calculateArrivalDistance(groundTransition);
        };
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

    private boolean hasReachedWaypoint(double distSq,
                                       double arrivalDist,
                                       LandingPhase phase,
                                       boolean groundTransition) {
        if (phase == LandingPhase.TOUCHDOWN) {
            return this.host.onGround();
        }
        return this.hasReachedWaypoint(distSq, arrivalDist, groundTransition);
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
                && this.landingPhase == LandingPhase.NONE && !this.host.isVehicle()
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
        return this.movementExecutor.steeringSummary() + ",phase=" + this.landingPhase
                + (this.currentFlightRequest == null ? "" : ",purpose=" + this.currentFlightRequest.purpose()
                    + ",arrival=" + this.currentFlightRequest.arrival() + ",speed=" + this.speedModifier)
                + (this.landingPlan == null ? "" : ",touchdown=" + this.landingPlan.touchdown());
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

    public enum LandingPhase {
        NONE(false, false, false),
        APPROACH(false, false, true),
        GLIDE(true, true, true),
        FLARE(true, true, true),
        TOUCHDOWN(true, true, false),
        GO_AROUND(false, false, false);

        private final boolean committed;
        private final boolean directCorridor;
        private final boolean advancesLandingPlan;

        LandingPhase(boolean committed, boolean directCorridor, boolean advancesLandingPlan) {
            this.committed = committed;
            this.directCorridor = directCorridor;
            this.advancesLandingPlan = advancesLandingPlan;
        }

        public boolean isCommitted() {
            return this.committed;
        }

        public boolean usesDirectCorridor() {
            return this.directCorridor;
        }

        public boolean advancesLandingPlan() {
            return this.advancesLandingPlan;
        }
    }
}
