package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.ai.pathfinding.AsyncDragonPathfinder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

class AsyncFlightPathResolver {
    private final Mob dragon;
    private final AsyncFlightController component;
    private final List<Vec3> pathNodes = new ArrayList<>();
    private int currentPathIndex = 0;
    private int ticksSinceRecalc = 0;

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
        long requestGeneration = this.component.beginPathRequest();
        this.component.setState(AsyncFlightController.PathState.CALCULATING);
        AsyncDragonPathfinder.calculateFlyingPathAsync(this.dragon, currentWaypoint, path -> {
            if (!this.component.isPathRequestCurrent(requestGeneration)) {
                return;
            }

            Vec3 activeWaypoint = this.component.getCurrentWaypoint();
            if (activeWaypoint == null) {
                return;
            }
            double distToTarget = this.dragon.position().distanceTo(activeWaypoint);
            boolean landingTarget = this.component.isLandingTarget(activeWaypoint);
            double arrivalDistance = this.component.calculateArrivalDistance(landingTarget);
            if (path != null && path.getNodeCount() == 0) {
                if (this.component.hasReachedWaypoint(distToTarget * distToTarget, arrivalDistance, landingTarget)) {
                    this.component.onArrived();
                    return;
                }
                if (landingTarget) {
                    this.clearPathNodes();
                    this.component.setState(AsyncFlightController.PathState.FOLLOWING);
                    return;
                }
            }

            if (path != null && path.getNodeCount() > 0) {
                Vec3 endNodePos = Vec3.atBottomCenterOf(path.getEndNode().asBlockPos());
                double pathDistToTarget = endNodePos.distanceTo(activeWaypoint);
                double arrivalThreshold = this.dragon.getBbWidth() + 4.0;
                if (pathDistToTarget > arrivalThreshold) {
                    this.handlePathCalculationFailure(activeWaypoint);
                    return;
                }

                this.cachePathNodes(path, activeWaypoint);
                this.component.setState(AsyncFlightController.PathState.FOLLOWING);
            } else if (landingTarget) {
                this.clearPathNodes();
                this.component.setState(AsyncFlightController.PathState.FOLLOWING);
            } else {
                this.handlePathCalculationFailure(activeWaypoint);
            }
        });
    }

    public void cachePathNodes(Path path, Vec3 currentWaypoint) {
        this.pathNodes.clear();
        this.currentPathIndex = 0;

        for (int i = 0; i < path.getNodeCount(); ++i) {
            Node node = path.getNode(i);
            double x = node.x + 0.5;
            boolean isLastNode = i == path.getNodeCount() - 1;
            double y = node.y + (isLastNode ? 0.0 : 0.5);
            double z = node.z + 0.5;
            this.pathNodes.add(new Vec3(x, y, z));
        }

        if (!this.pathNodes.isEmpty() && this.pathNodes.get(this.pathNodes.size() - 1).distanceToSqr(currentWaypoint) > 1.0) {
            this.pathNodes.add(currentWaypoint);
        }
    }

    public void clearPathNodes() {
        this.pathNodes.clear();
        this.currentPathIndex = 0;
    }

    public void retargetPathEndpoint(Vec3 target) {
        if (target == null || this.pathNodes.isEmpty()) {
            return;
        }
        this.pathNodes.set(this.pathNodes.size() - 1, target);
    }

    public Vec3 calculateLookAheadPoint(double flyingLookAhead) {
        if (this.pathNodes.isEmpty()) {
            return null;
        }

        Vec3 dragonPos = this.dragon.position();
        double bestDistSq = Double.MAX_VALUE;
        int bestIndex = this.currentPathIndex;
        int searchStart = Math.max(0, this.currentPathIndex - 2);
        int searchEnd = Math.min(this.pathNodes.size() - 1, this.currentPathIndex + 6);

        for (int i = searchStart; i < searchEnd; ++i) {
            Vec3 segStart = this.pathNodes.get(i);
            Vec3 segEnd = this.pathNodes.get(i + 1);
            Vec3 closest = closestPointOnSegment(dragonPos, segStart, segEnd);
            double dSq = dragonPos.distanceToSqr(closest);
            if (dSq < bestDistSq) {
                bestDistSq = dSq;
                bestIndex = i;
            }
        }

        this.currentPathIndex = bestIndex;
        Vec3 startPoint = closestPointOnSegment(
                dragonPos,
                this.pathNodes.get(this.currentPathIndex),
                this.currentPathIndex + 1 < this.pathNodes.size()
                        ? this.pathNodes.get(this.currentPathIndex + 1)
                        : this.pathNodes.get(this.currentPathIndex)
        );

        double sharpCornerDot = 0.15;
        int idx = this.currentPathIndex;
        for (double remainingDist = flyingLookAhead; remainingDist > 0.0 && idx + 1 < this.pathNodes.size(); idx++) {
            Vec3 cornerNode = this.pathNodes.get(idx + 1);
            double segRemaining = startPoint.distanceTo(cornerNode);
            if (segRemaining >= remainingDist) {
                Vec3 dir = cornerNode.subtract(startPoint).normalize();
                return startPoint.add(dir.scale(remainingDist));
            }

            remainingDist -= segRemaining;
            if (idx + 2 >= this.pathNodes.size()) {
                continue;
            }

            Vec3 inDir = cornerNode.subtract(startPoint).normalize();
            Vec3 outDir = this.pathNodes.get(idx + 2).subtract(cornerNode).normalize();
            Vec3 inH = new Vec3(inDir.x, 0.0, inDir.z);
            Vec3 outH = new Vec3(outDir.x, 0.0, outDir.z);
            if (inH.lengthSqr() > 0.001 && outH.lengthSqr() > 0.001) {
                double dot = inH.normalize().dot(outH.normalize());
                if (dot < sharpCornerDot) {
                    return cornerNode;
                }
            }
        }

        return this.pathNodes.get(this.pathNodes.size() - 1);
    }

    private static Vec3 closestPointOnSegment(Vec3 point, Vec3 segStart, Vec3 segEnd) {
        Vec3 seg = segEnd.subtract(segStart);
        double segLenSq = seg.lengthSqr();
        if (segLenSq < 1.0E-4) {
            return segStart;
        }
        double t = Math.max(0.0, Math.min(1.0, point.subtract(segStart).dot(seg) / segLenSq));
        return segStart.add(seg.scale(t));
    }

    private void handlePathCalculationFailure(Vec3 currentWaypoint) {
        this.component.handleStuck(currentWaypoint);
    }

    public void recalculatePath(Vec3 currentWaypoint) {
        this.ticksSinceRecalc = 0;
        if (currentWaypoint == null || this.component.getState() == AsyncFlightController.PathState.CALCULATING) {
            return;
        }
        this.startPathing(currentWaypoint);
    }

    public void forceRecalculatePath(Vec3 currentWaypoint) {
        this.ticksSinceRecalc = 0;
        if (currentWaypoint == null) {
            return;
        }
        this.clearPathNodes();
        this.startFlyingPathAsync(currentWaypoint);
    }

    public void reset() {
        this.ticksSinceRecalc = 0;
    }

    public void tickRecalc() {
        this.ticksSinceRecalc++;
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
