package com.leon.saintsdragons.server.ai.navigation.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

class AsyncFlightPathResolver {
    private static final double PARTIAL_PATH_REPLAN_DISTANCE = 16.0D;

    private final Mob dragon;
    private final AsyncFlightController component;
    private final PathRequester requester;
    private final SegmentChecker segments;
    private boolean awaitingResult;
    private @Nullable Vec3 pendingTarget;
    private long lastRequestTick = Long.MIN_VALUE;
    private final List<Vec3> pathNodes = new ArrayList<>();
    private int currentPathIndex = 0;
    private int ticksSinceRecalc = 0;
    private @Nullable Future<?> activePathRequest;
    private @Nullable Vec3 requestedTarget;
    private @Nullable Vec3 resolvedTarget;
    private boolean resolvedPathCanReach;
    private boolean hasSyntheticEndpoint;

    AsyncFlightPathResolver(Mob dragon, AsyncFlightController component) {
        this(dragon, component, AsyncDragonPathfinder::calculateFlyingPathAsync,
                (start, end) -> clearLiveSegment(dragon, start, end));
    }

    AsyncFlightPathResolver(Mob dragon, AsyncFlightController component, PathRequester requester, SegmentChecker segments) {
        this.dragon = dragon;
        this.component = component;
        this.requester = requester;
        this.segments = segments;
    }

    public void startPathing(Vec3 target) {
        this.forceRecalculatePath(target);
    }

    public void startFlyingPathAsync(Vec3 target) {
        this.forceRecalculatePath(target);
    }

    public void updateTarget(Vec3 target) {
        if (target == null) return;
        Vec3 planningTarget = this.requestedTarget != null ? this.requestedTarget : this.resolvedTarget;
        if (!FlightRoutePolicy.needsNewRoute(this.dragon.position(), planningTarget, target)) {
            if (this.awaitingResult
                    || (!this.resolvedPathCanReach && this.hasUsableRemainingPath() && !this.shouldExtendPartialPath())
                    || this.retargetPathEndpoint(target)) {
                this.pendingTarget = null;
                return;
            }
        } else if (!this.pathNodes.isEmpty() && this.resolvedTarget != null
                && !FlightRoutePolicy.resultStillUseful(this.dragon.position(), this.resolvedTarget, target,
                        this.pathNodes.get(this.pathNodes.size() - 1), this.dragon.getBbWidth())) {
            this.clearPathNodes();
            this.component.setState(AsyncFlightController.PathState.CALCULATING);
        }
        this.forceRecalculatePath(target);
    }

    private void flushPendingRequest() {
        if (this.pendingTarget == null || !FlightRoutePolicy.mayRequest(this.dragon.tickCount, this.lastRequestTick, this.awaitingResult)) return;
        Vec3 requestTarget = this.pendingTarget;
        Vec3 requestOrigin = this.dragon.position();
        this.pendingTarget = null;
        this.requestedTarget = requestTarget;
        this.lastRequestTick = this.dragon.tickCount;
        this.ticksSinceRecalc = 0;
        this.awaitingResult = true;
        long requestGeneration = this.component.beginPathRequest();
        if (this.pathNodes.isEmpty()) this.component.setState(AsyncFlightController.PathState.CALCULATING);
        Future<?> request = this.requester.request(this.dragon, requestTarget, path -> {
            if (!this.component.isPathRequestCurrent(requestGeneration)) return;
            this.activePathRequest = null;
            this.awaitingResult = false;
            this.requestedTarget = null;
            this.acceptResult(path, requestOrigin, requestTarget);
        });
        // Capacity rejection may deliver its callback synchronously before request() returns.
        this.activePathRequest = this.awaitingResult && this.component.isPathRequestCurrent(requestGeneration) ? request : null;
    }

    private void acceptResult(@Nullable Path path, Vec3 requestOrigin, Vec3 requestTarget) {
        Vec3 activeWaypoint = this.component.getCurrentWaypoint();
        if (activeWaypoint == null) return;
        boolean targetMoved = FlightRoutePolicy.needsNewRoute(requestOrigin, requestTarget, activeWaypoint);
        if (targetMoved && (path == null || path.getNodeCount() == 0
                || !FlightRoutePolicy.resultStillUseful(requestOrigin, requestTarget, activeWaypoint,
                        Vec3.atCenterOf(path.getEndNode().asBlockPos()), this.dragon.getBbWidth()))) {
            this.pendingTarget = activeWaypoint;
            if (this.pathNodes.isEmpty()) this.component.setState(AsyncFlightController.PathState.CALCULATING);
            return;
        }

        double distToTarget = this.dragon.position().distanceTo(activeWaypoint);
        boolean groundTransition = this.component.isGroundTransition();
        double arrivalDistance = this.component.calculateArrivalDistance(groundTransition);
        if (path != null && path.getNodeCount() == 0) {
            if (this.component.hasReachedWaypoint(distToTarget * distToTarget, arrivalDistance, groundTransition)) {
                this.component.onArrived();
                return;
            }
            if (groundTransition && this.isSegmentClear(this.dragon.position(), activeWaypoint)) {
                this.component.setState(AsyncFlightController.PathState.FOLLOWING);
                return;
            }
        }

        if (path != null && path.getNodeCount() > 0) {
            Vec3 endNodePos = Vec3.atCenterOf(path.getEndNode().asBlockPos());
            double minimumUsefulProgress = Math.max(1.0D, this.dragon.getBbWidth() * 0.25D);
            if (!path.canReach() && endNodePos.distanceTo(requestTarget) + minimumUsefulProgress >= requestOrigin.distanceTo(requestTarget)) {
                this.handlePathCalculationFailure(activeWaypoint);
                return;
            }
            this.cachePathNodes(path, requestTarget, path.canReach());
            this.resolvedTarget = requestTarget;
            this.component.setState(AsyncFlightController.PathState.FOLLOWING);
            if (targetMoved || (path.canReach() && !this.retargetPathEndpoint(activeWaypoint))) {
                this.resolvedPathCanReach = false;
                this.pendingTarget = activeWaypoint;
            } else {
                this.pendingTarget = null;
            }
        } else if (groundTransition && this.isSegmentClear(this.dragon.position(), activeWaypoint)) {
            this.component.setState(AsyncFlightController.PathState.FOLLOWING);
        } else {
            this.handlePathCalculationFailure(activeWaypoint);
        }
    }

    public void cachePathNodes(Path path, Vec3 currentWaypoint, boolean canReach) {
        this.pathNodes.clear();
        this.currentPathIndex = 0;
        this.hasSyntheticEndpoint = false;
        this.resolvedPathCanReach = canReach;

        for (int i = 0; i < path.getNodeCount(); ++i) {
            Node node = path.getNode(i);
            double x = node.x + 0.5;
            double y = node.y + 0.5;
            double z = node.z + 0.5;
            this.pathNodes.add(new Vec3(x, y, z));
        }

        if (canReach
                && !this.pathNodes.isEmpty()
                && this.pathNodes.get(this.pathNodes.size() - 1).distanceToSqr(currentWaypoint) > 1.0D
                && this.isSegmentClear(this.pathNodes.get(this.pathNodes.size() - 1), currentWaypoint)) {
            this.pathNodes.add(currentWaypoint);
            this.hasSyntheticEndpoint = true;
        }
    }

    public void clearPathNodes() {
        this.pathNodes.clear();
        this.currentPathIndex = 0;
        this.hasSyntheticEndpoint = false;
        this.resolvedPathCanReach = false;
        this.resolvedTarget = null;
    }

    public boolean retargetPathEndpoint(Vec3 target) {
        if (target == null || this.pathNodes.isEmpty()) return false;
        Vec3 currentEnd = this.pathNodes.get(this.pathNodes.size() - 1);
        if (this.resolvedPathCanReach && currentEnd.distanceToSqr(target) < 1.0E-8D) return true;
        int anchorIndex = this.pathNodes.size() - (this.hasSyntheticEndpoint ? 2 : 1);
        if (anchorIndex < 0) return false;
        Vec3 pathEnd = this.pathNodes.get(anchorIndex);
        if (pathEnd.distanceToSqr(target) > FlightRoutePolicy.MAX_ENDPOINT_EXTENSION * FlightRoutePolicy.MAX_ENDPOINT_EXTENSION
                || !this.isSegmentClear(pathEnd, target)) return false;
        if (this.hasSyntheticEndpoint) this.pathNodes.remove(this.pathNodes.size() - 1);
        this.hasSyntheticEndpoint = pathEnd.distanceToSqr(target) > 1.0E-8D;
        if (this.hasSyntheticEndpoint) this.pathNodes.add(target);
        this.resolvedPathCanReach = true;
        this.currentPathIndex = Math.min(this.currentPathIndex, this.pathNodes.size() - 1);
        return true;
    }

    public Vec3 calculateLookAheadPoint(double flyingLookAhead) {
        if (this.pathNodes.isEmpty()) {
            return null;
        }

        AsyncFlightPathGeometry.LookAheadResult result = AsyncFlightPathGeometry.calculateLookAhead(
                this.pathNodes,
                this.currentPathIndex,
                this.dragon.position(),
                flyingLookAhead,
                0.15D
        );
        if (result == null) {
            return null;
        }
        this.currentPathIndex = result.pathIndex();
        if (this.isSegmentClear(this.dragon.position(), result.target())) {
            return result.target();
        }
        Vec3 fallback = this.findNearestClearPathTarget();
        if (fallback == null) this.forceRecalculatePath(this.component.getCurrentWaypoint());
        return fallback;
    }

    private void handlePathCalculationFailure(Vec3 currentWaypoint) {
        if (this.hasUsableRemainingPath()) {
            this.component.setState(AsyncFlightController.PathState.FOLLOWING);
            return;
        }
        this.clearPathNodes();
        this.component.handleStuck(currentWaypoint);
    }

    public boolean shouldExtendPartialPath() {
        if (this.resolvedPathCanReach
                || this.pathNodes.isEmpty()
                || this.hasActivePathRequest()) {
            return false;
        }
        return AsyncFlightPathGeometry.remainingDistance(this.pathNodes, this.currentPathIndex,
                this.dragon.position()) <= PARTIAL_PATH_REPLAN_DISTANCE;
    }

    public void forceRecalculatePath(Vec3 target) {
        if (target == null) return;
        this.pendingTarget = target;
        this.flushPendingRequest();
    }

    public void tickPathing(Vec3 target) {
        this.ticksSinceRecalc++;
        if (this.pendingTarget != null) this.pendingTarget = target;
        this.flushPendingRequest();
    }

    public void reset() {
        this.cancelActivePathRequest();
        this.ticksSinceRecalc = 0;
    }

    public boolean hasActivePathRequest() {
        return this.awaitingResult;
    }

    public @Nullable Vec3 calculateSafeDirectLookAhead(Vec3 target, double maxDistance) {
        if (target == null) {
            return null;
        }
        Vec3 position = this.dragon.position();
        Vec3 offset = target.subtract(position);
        double distance = offset.length();
        if (distance < 1.0E-4D) {
            return target;
        }
        Vec3 localTarget = distance <= maxDistance
                ? target
                : position.add(offset.scale(maxDistance / distance));
        return this.isSegmentClear(position, localTarget) ? localTarget : null;
    }

    public void cancelActivePathRequest() {
        if (this.activePathRequest != null) {
            this.activePathRequest.cancel(true);
            this.activePathRequest = null;
        }
        this.requestedTarget = null;
        this.pendingTarget = null;
        this.awaitingResult = false;
        this.component.invalidatePathRequests();
    }

    private @Nullable Vec3 findNearestClearPathTarget() {
        Vec3 position = this.dragon.position();
        int segmentIndex = Math.max(0, Math.min(this.currentPathIndex, this.pathNodes.size() - 1));
        int nextIndex = Math.min(segmentIndex + 1, this.pathNodes.size() - 1);
        Vec3 nextNode = this.pathNodes.get(nextIndex);
        if (this.isSegmentClear(position, nextNode)) {
            return nextNode;
        }
        if (segmentIndex < this.pathNodes.size() - 1) {
            Vec3 projection = AsyncFlightPathGeometry.closestPointOnSegment(
                    position,
                    this.pathNodes.get(segmentIndex),
                    nextNode
            );
            if (position.distanceToSqr(projection) > 0.0625D
                    && this.isSegmentClear(position, projection)) {
                return projection;
            }
        }
        return null;
    }

    private boolean isSegmentClear(Vec3 start, Vec3 end) {
        return this.segments.isClear(start, end);
    }

    private static boolean clearLiveSegment(Mob dragon, Vec3 start, Vec3 end) {
        Vec3 movement = end.subtract(start);
        int count = Math.max(1, (int) Math.ceil(movement.length() / 4.0D));
        Vec3 cursor = start;
        for (int segment = 1; segment <= count; segment++) {
            Vec3 next = segment == count ? end : start.add(movement.scale((double) segment / count));
            if (!VoxelAabbSweeper.isClear(dragon.level(), dragon,
                    dragon.getBoundingBox().move(cursor.subtract(dragon.position())), next.subtract(cursor))) return false;
            cursor = next;
        }
        return true;
    }

    private boolean hasUsableRemainingPath() {
        if (this.pathNodes.isEmpty()) {
            return false;
        }
        int remainingNodes = this.pathNodes.size() - 1 - this.currentPathIndex;
        if (remainingNodes >= 3) {
            return true;
        }
        double minimumRemainingDistance = Math.max(2.0D, this.dragon.getBbWidth() * 0.5D);
        return this.dragon.position().distanceToSqr(this.pathNodes.get(this.pathNodes.size() - 1))
                > minimumRemainingDistance * minimumRemainingDistance;
    }

    public int getTicksSinceRecalc() {
        return this.ticksSinceRecalc;
    }

    @FunctionalInterface
    interface PathRequester {
        Future<?> request(Mob dragon, Vec3 target, Consumer<Path> callback);
    }

    @FunctionalInterface
    interface SegmentChecker {
        boolean isClear(Vec3 from, Vec3 to);
    }

    List<Vec3> getDebugPathNodes() {
        return List.copyOf(this.pathNodes);
    }

    int getDebugCurrentPathIndex() {
        return this.currentPathIndex;
    }
}
