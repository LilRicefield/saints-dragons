package com.leon.saintsdragons.server.ai.navigation.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

class AsyncFlightPathResolver {
    private static final int PARTIAL_PATH_REPLAN_NODES = 12;
    private static final double STALE_REQUEST_TARGET_DISTANCE_SQ = 4.0D;

    private final Mob dragon;
    private final AsyncFlightController component;
    private final List<Vec3> pathNodes = new ArrayList<>();
    private int currentPathIndex = 0;
    private int ticksSinceRecalc = 0;
    private @Nullable Future<?> activePathRequest;
    private @Nullable Vec3 requestedTarget;
    private @Nullable Vec3 resolvedTarget;
    private boolean resolvedPathCanReach;
    private boolean hasSyntheticEndpoint;

    AsyncFlightPathResolver(Mob dragon, AsyncFlightController component) {
        this.dragon = dragon;
        this.component = component;
    }

    public void startPathing(Vec3 currentWaypoint) {
        if (currentWaypoint == null) {
            return;
        }
        this.startFlyingPathAsync(currentWaypoint);
    }

    public void startFlyingPathAsync(Vec3 currentWaypoint) {
        this.cancelActivePathRequest();
        long requestGeneration = this.component.beginPathRequest();
        Vec3 requestTarget = currentWaypoint;
        Vec3 requestOrigin = this.dragon.position();
        this.requestedTarget = requestTarget;
        if (this.pathNodes.isEmpty()) {
            this.component.setState(AsyncFlightController.PathState.CALCULATING);
        }
        this.activePathRequest = AsyncDragonPathfinder.calculateFlyingPathAsync(this.dragon, requestTarget, path -> {
            if (!this.component.isPathRequestCurrent(requestGeneration)) {
                return;
            }
            this.activePathRequest = null;
            this.requestedTarget = null;
            this.ticksSinceRecalc = 0;

            Vec3 activeWaypoint = this.component.getCurrentWaypoint();
            if (activeWaypoint == null) {
                return;
            }
            if (activeWaypoint.distanceToSqr(requestTarget) > STALE_REQUEST_TARGET_DISTANCE_SQ) {
                this.startFlyingPathAsync(activeWaypoint);
                return;
            }
            double distToTarget = this.dragon.position().distanceTo(activeWaypoint);
            boolean groundTransition = this.component.isGroundTransition();
            double arrivalDistance = this.component.calculateArrivalDistance(groundTransition);
            if (path != null && path.getNodeCount() == 0) {
                if (this.component.hasReachedWaypoint(
                        distToTarget * distToTarget,
                        arrivalDistance,
                        groundTransition
                )) {
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
                double pathDistToTarget = endNodePos.distanceTo(requestTarget);
                double requestDistToTarget = requestOrigin.distanceTo(requestTarget);
                double minimumUsefulProgress = Math.max(1.0D, this.dragon.getBbWidth() * 0.25D);
                boolean usefulPartialPath = path.canReach()
                        || pathDistToTarget + minimumUsefulProgress < requestDistToTarget;
                if (!usefulPartialPath) {
                    this.handlePathCalculationFailure(activeWaypoint);
                    return;
                }

                this.cachePathNodes(path, activeWaypoint, path.canReach());
                this.resolvedTarget = activeWaypoint;
                this.component.setState(AsyncFlightController.PathState.FOLLOWING);
            } else if (groundTransition && this.isSegmentClear(this.dragon.position(), activeWaypoint)) {
                this.component.setState(AsyncFlightController.PathState.FOLLOWING);
            } else {
                this.handlePathCalculationFailure(activeWaypoint);
            }
        });
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
        if (target == null || this.pathNodes.isEmpty()) {
            return false;
        }
        if (this.hasSyntheticEndpoint) {
            this.pathNodes.remove(this.pathNodes.size() - 1);
            this.hasSyntheticEndpoint = false;
        }
        this.resolvedPathCanReach = false;
        if (this.pathNodes.isEmpty()) {
            return false;
        }

        Vec3 pathEnd = this.pathNodes.get(this.pathNodes.size() - 1);
        if (pathEnd.distanceToSqr(target) <= 1.0D) {
            this.resolvedPathCanReach = true;
            return true;
        }
        if (!this.isSegmentClear(pathEnd, target)) {
            return false;
        }
        this.pathNodes.add(target);
        this.hasSyntheticEndpoint = true;
        this.resolvedPathCanReach = true;
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
        return this.findNearestClearPathTarget();
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
        int remainingNodes = this.pathNodes.size() - 1 - this.currentPathIndex;
        return remainingNodes <= PARTIAL_PATH_REPLAN_NODES;
    }

    public void recalculatePath(Vec3 currentWaypoint) {
        if (currentWaypoint == null || this.hasActivePathRequest()) {
            return;
        }
        this.ticksSinceRecalc = 0;
        this.startPathing(currentWaypoint);
    }

    public void forceRecalculatePath(Vec3 currentWaypoint) {
        this.ticksSinceRecalc = 0;
        if (currentWaypoint == null) {
            return;
        }
        this.startFlyingPathAsync(currentWaypoint);
    }

    public void reset() {
        this.cancelActivePathRequest();
        this.requestedTarget = null;
        this.ticksSinceRecalc = 0;
    }

    public boolean needsRefresh(Vec3 target, double refreshDistanceSq) {
        Vec3 planningTarget = this.requestedTarget != null ? this.requestedTarget : this.resolvedTarget;
        return planningTarget == null || planningTarget.distanceToSqr(target) >= refreshDistanceSq;
    }

    public boolean hasActivePathRequest() {
        // A worker may finish while its result is still waiting in the server task queue. The
        // PathRequest itself completes after the callback, and the callback clears this reference.
        return this.activePathRequest != null;
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
        Vec3 movement = end.subtract(start);
        Vec3 boxOffset = start.subtract(this.dragon.position());
        return VoxelAabbSweeper.isClear(
                this.dragon.level(),
                this.dragon,
                this.dragon.getBoundingBox().move(boxOffset),
                movement
        );
    }

    public void tickRecalc() {
        this.ticksSinceRecalc++;
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

    List<Vec3> getDebugPathNodes() {
        return List.copyOf(this.pathNodes);
    }

    int getDebugCurrentPathIndex() {
        return this.currentPathIndex;
    }
}
