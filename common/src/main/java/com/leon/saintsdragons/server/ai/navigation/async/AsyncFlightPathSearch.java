package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.ai.pathfinding.DragonPathSearchDebug;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A worker-only A* search over an immutable voxel snapshot. */
final class AsyncFlightPathSearch {
    private static final int MAX_VISITED_NODES = 5000;
    private static final double SQRT_TWO = Math.sqrt(2.0D);
    private static final double SQRT_THREE = Math.sqrt(3.0D);
    private static final int[][] NEIGHBOR_OFFSETS = createNeighborOffsets();

    private final CollisionView collisionView;
    private final Vec3 origin;
    private final Vec3 target;
    private final Vec3 requestedTarget;
    private final AABB relativeBounds;
    private final BlockPos startNode;
    private final BlockPos targetNode;
    private final BlockPos requestedTargetNode;
    private final BlockPos minNode;
    private final BlockPos maxNode;
    private final boolean completeRoute;
    private final BooleanSupplier cancelled;

    AsyncFlightPathSearch(ImmutableBlockSnapshot blocks,
                          Vec3 origin,
                          Vec3 target,
                          Vec3 requestedTarget,
                          AABB relativeBounds,
                          BlockPos minNode,
                          BlockPos maxNode,
                          BooleanSupplier cancelled) {
        this(
                (startBox, movement) -> VoxelAabbSweeper.isClear(blocks, startBox, movement),
                origin,
                target,
                requestedTarget,
                relativeBounds,
                minNode,
                maxNode,
                cancelled
        );
    }

    AsyncFlightPathSearch(CollisionView collisionView,
                          Vec3 origin,
                          Vec3 target,
                          AABB relativeBounds,
                          BlockPos minNode,
                          BlockPos maxNode,
                          BooleanSupplier cancelled) {
        this(
                collisionView,
                origin,
                target,
                target,
                relativeBounds,
                minNode,
                maxNode,
                cancelled
        );
    }

    private AsyncFlightPathSearch(CollisionView collisionView,
                                  Vec3 origin,
                                  Vec3 target,
                                  Vec3 requestedTarget,
                                  AABB relativeBounds,
                                  BlockPos minNode,
                                  BlockPos maxNode,
                                  BooleanSupplier cancelled) {
        this.collisionView = collisionView;
        this.origin = origin;
        this.target = target;
        this.requestedTarget = requestedTarget;
        this.relativeBounds = relativeBounds;
        this.startNode = BlockPos.containing(origin);
        this.targetNode = BlockPos.containing(target);
        this.requestedTargetNode = BlockPos.containing(requestedTarget);
        this.minNode = minNode;
        this.maxNode = maxNode;
        this.completeRoute = target.distanceToSqr(requestedTarget) < 1.0E-8D;
        this.cancelled = cancelled;
    }

    @Nullable
    Path findPath(@Nullable UUID debugDragonId) {
        long startedNanos = System.nanoTime();
        long startKey = this.startNode.asLong();
        PriorityQueue<OpenNode> open = new PriorityQueue<>(
                Comparator.comparingDouble(OpenNode::fScore)
                        .thenComparingDouble(OpenNode::hScore)
        );
        Map<Long, Long> cameFrom = new HashMap<>();
        Map<Long, Double> gScore = new HashMap<>();
        Set<Long> closed = debugDragonId == null ? new HashSet<>() : new LinkedHashSet<>();
        Map<Long, Boolean> clearNodes = new HashMap<>();
        Set<EdgeKey> blockedEdges = new HashSet<>();

        double startHeuristic = heuristic(this.startNode);
        gScore.put(startKey, 0.0D);
        open.add(new OpenNode(startKey, 0.0D, startHeuristic, startHeuristic));
        long bestKey = startKey;
        double bestHeuristic = startHeuristic;
        boolean reached = false;
        int visited = 0;

        while (!open.isEmpty() && visited < MAX_VISITED_NODES) {
            if (this.cancelled.getAsBoolean()) {
                return null;
            }

            OpenNode current = open.poll();
            double knownScore = gScore.getOrDefault(current.key(), Double.POSITIVE_INFINITY);
            if (current.gScore() > knownScore || !closed.add(current.key())) {
                continue;
            }
            visited++;

            BlockPos currentPos = BlockPos.of(current.key());
            double currentHeuristic = heuristic(currentPos);
            if (currentHeuristic < bestHeuristic) {
                bestHeuristic = currentHeuristic;
                bestKey = current.key();
            }
            if (currentPos.equals(this.targetNode) && canFinishAtTarget(current.key())) {
                bestKey = current.key();
                reached = true;
                break;
            }

            for (int[] offset : NEIGHBOR_OFFSETS) {
                if (this.cancelled.getAsBoolean()) {
                    return null;
                }
                int nextX = currentPos.getX() + offset[0];
                int nextY = currentPos.getY() + offset[1];
                int nextZ = currentPos.getZ() + offset[2];
                if (!withinBounds(nextX, nextY, nextZ)) {
                    continue;
                }

                BlockPos nextPos = new BlockPos(nextX, nextY, nextZ);
                long nextKey = nextPos.asLong();
                if (closed.contains(nextKey)
                        || !clearNodes.computeIfAbsent(nextKey, ignored -> isNodeClear(nextPos))) {
                    continue;
                }

                EdgeKey edge = new EdgeKey(current.key(), nextKey);
                if (blockedEdges.contains(edge) || !isEdgeClear(current.key(), nextPos)) {
                    blockedEdges.add(edge);
                    continue;
                }

                double stepCost = Math.sqrt(
                        offset[0] * offset[0]
                                + offset[1] * offset[1]
                                + offset[2] * offset[2]
                );
                double tentativeScore = knownScore + stepCost;
                if (tentativeScore >= gScore.getOrDefault(nextKey, Double.POSITIVE_INFINITY)) {
                    continue;
                }

                cameFrom.put(nextKey, current.key());
                gScore.put(nextKey, tentativeScore);
                double nextHeuristic = heuristic(nextPos);
                open.add(new OpenNode(
                        nextKey,
                        tentativeScore,
                        nextHeuristic,
                        tentativeScore + nextHeuristic
                ));
            }
        }

        boolean reachedRequestedTarget = reached && this.completeRoute;
        Path path = buildPath(cameFrom, bestKey, reachedRequestedTarget);
        if (debugDragonId != null && !this.cancelled.getAsBoolean()) {
            List<Vec3> closedPositions = closed.stream().map(AsyncFlightPathSearch::nodeCenter).toList();
            List<Vec3> openPositions = gScore.keySet().stream()
                    .filter(key -> !closed.contains(key))
                    .map(AsyncFlightPathSearch::nodeCenter)
                    .toList();
            DragonPathSearchDebug.publishGridSearch(
                    debugDragonId,
                    DragonPathSearchDebug.SearchType.AIR,
                    this.origin,
                    this.requestedTarget,
                    closedPositions,
                    openPositions,
                    List.of(),
                    reachedRequestedTarget,
                    startedNanos
            );
        }
        return path;
    }

    private boolean canFinishAtTarget(long currentKey) {
        Vec3 from = currentKey == this.startNode.asLong() ? this.origin : nodeCenter(currentKey);
        AABB startBox = this.relativeBounds.move(from);
        return this.collisionView.isClear(startBox, this.target.subtract(from));
    }

    private boolean isNodeClear(BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        return this.collisionView.isClear(this.relativeBounds.move(center), Vec3.ZERO);
    }

    private boolean isEdgeClear(long fromKey, BlockPos destination) {
        Vec3 from = fromKey == this.startNode.asLong() ? this.origin : nodeCenter(fromKey);
        Vec3 to = Vec3.atCenterOf(destination);
        return this.collisionView.isClear(this.relativeBounds.move(from), to.subtract(from));
    }

    private boolean withinBounds(int x, int y, int z) {
        return x >= this.minNode.getX() && x <= this.maxNode.getX()
                && y >= this.minNode.getY() && y <= this.maxNode.getY()
                && z >= this.minNode.getZ() && z <= this.maxNode.getZ();
    }

    private double heuristic(BlockPos pos) {
        int dx = Math.abs(pos.getX() - this.targetNode.getX());
        int dy = Math.abs(pos.getY() - this.targetNode.getY());
        int dz = Math.abs(pos.getZ() - this.targetNode.getZ());
        int shortest = Math.min(dx, Math.min(dy, dz));
        int longest = Math.max(dx, Math.max(dy, dz));
        int middle = dx + dy + dz - shortest - longest;
        return shortest * SQRT_THREE
                + (middle - shortest) * SQRT_TWO
                + (longest - middle);
    }

    private Path buildPath(Map<Long, Long> cameFrom, long endKey, boolean reached) {
        List<Node> nodes = new ArrayList<>();
        long current = endKey;
        while (true) {
            BlockPos pos = BlockPos.of(current);
            nodes.add(new Node(pos.getX(), pos.getY(), pos.getZ()));
            Long previous = cameFrom.get(current);
            if (previous == null) {
                break;
            }
            current = previous;
        }
        Collections.reverse(nodes);
        if (nodes.size() > 1) {
            nodes.remove(0);
        }
        return new Path(nodes, this.requestedTargetNode, reached);
    }

    private static Vec3 nodeCenter(long key) {
        return Vec3.atCenterOf(BlockPos.of(key));
    }

    private static int[][] createNeighborOffsets() {
        List<int[]> offsets = new ArrayList<>(26);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        offsets.add(new int[]{x, y, z});
                    }
                }
            }
        }
        return offsets.toArray(int[][]::new);
    }

    private record OpenNode(long key, double gScore, double hScore, double fScore) {
    }

    private record EdgeKey(long from, long to) {
        private EdgeKey {
            if (from > to) {
                long swap = from;
                from = to;
                to = swap;
            }
        }
    }

    @FunctionalInterface
    interface CollisionView {
        boolean isClear(AABB startBox, Vec3 movement);
    }
}
