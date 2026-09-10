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
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class AsyncFlightPathSearch {
    private static final int MAX_VISITED_NODES = 5000;
    private static final double MAX_SHORTCUT_DISTANCE = 24.0D;
    private static final double SWEEP_SEGMENT_LENGTH = 4.0D;
    private static final double HEURISTIC_WEIGHT = 1.25D;
    private static final int GOAL_SWEEP_INTERVAL = 16;
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
                          Predicate<BlockState> ignoredBlocks,
                          Vec3 origin,
                          Vec3 target,
                          Vec3 requestedTarget,
                          AABB relativeBounds,
                          BlockPos minNode,
                          BlockPos maxNode,
                          BooleanSupplier cancelled) {
        this(
                (startBox, movement) -> VoxelAabbSweeper.isClear(blocks, startBox, movement, ignoredBlocks),
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

    AsyncFlightPathSearch(CollisionView collisionView,
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
    Path findPath(@Nullable DragonPathSearchDebug.SearchSession debugSession) {
        long startedNanos = System.nanoTime();
        long startKey = this.startNode.asLong();
        PriorityQueue<OpenNode> open = new PriorityQueue<>(
                Comparator.comparingDouble(OpenNode::fScore)
                        .thenComparingDouble(OpenNode::hScore)
        );
        Map<Long, Long> cameFrom = new HashMap<>();
        Map<Long, Double> gScore = new HashMap<>();
        Set<Long> closed = debugSession == null ? new HashSet<>() : new LinkedHashSet<>();
        Map<Long, Boolean> clearNodes = new HashMap<>();
        Map<EdgeKey, Boolean> clearEdges = new HashMap<>();

        double startHeuristic = heuristic(this.startNode);
        gScore.put(startKey, 0.0D);
        open.add(new OpenNode(startKey, 0.0D, startHeuristic, HEURISTIC_WEIGHT * startHeuristic));
        long bestKey = startKey;
        double bestHeuristic = startHeuristic;
        boolean reached = false;
        int visited = 0;

        long targetKey = this.targetNode.asLong();
        if (targetKey != startKey && withinBounds(this.targetNode.getX(), this.targetNode.getY(), this.targetNode.getZ())
                && isSegmentClear(this.origin, nodePosition(targetKey))
                && canFinishAtTarget(targetKey)) {
            cameFrom.put(targetKey, startKey);
            gScore.put(targetKey, this.origin.distanceTo(nodePosition(targetKey)));
            bestKey = targetKey;
            reached = true;
            closed.add(startKey);
            closed.add(targetKey);
            open.clear();
        }

        while (!reached && !open.isEmpty() && visited < MAX_VISITED_NODES) {
            if (this.cancelled.getAsBoolean()) {
                return null;
            }

            OpenNode current = open.poll();
            double knownScore = gScore.getOrDefault(current.key(), Double.POSITIVE_INFINITY);
            if (Double.compare(current.gScore(), knownScore) != 0 || closed.contains(current.key())) {
                continue;
            }

            // Only confirmed connections may enter the closed set or a returned partial route.
            Long parentKey = cameFrom.get(current.key());
            if (parentKey != null && !isEdgeClear(parentKey, current.key(), clearEdges)) {
                Parent repair = repairParent(current.key(), closed, gScore, clearEdges);
                if (this.cancelled.getAsBoolean()) return null;
                if (repair == null) {
                    cameFrom.remove(current.key());
                    gScore.remove(current.key());
                    continue;
                }
                cameFrom.put(current.key(), repair.key());
                gScore.put(current.key(), repair.gScore());
                if (repair.gScore() > knownScore) {
                    open.add(new OpenNode(current.key(), repair.gScore(), current.hScore(),
                            repair.gScore() + HEURISTIC_WEIGHT * current.hScore()));
                    continue;
                }
                knownScore = repair.gScore();
            }
            if (this.cancelled.getAsBoolean()) return null;
            closed.add(current.key());
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

            if (visited % GOAL_SWEEP_INTERVAL == 0 && !closed.contains(targetKey)
                    && withinBounds(this.targetNode.getX(), this.targetNode.getY(), this.targetNode.getZ())
                    && isSegmentClear(nodePosition(current.key()), nodePosition(targetKey))
                    && canFinishAtTarget(targetKey)) {
                cameFrom.put(targetKey, current.key());
                gScore.put(targetKey, knownScore + currentHeuristic);
                closed.add(targetKey);
                bestKey = targetKey;
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

                double stepCost = nodePosition(current.key()).distanceTo(nodePosition(nextKey));
                double tentativeScore = knownScore + stepCost;
                long nextParent = current.key();
                Long ancestor = cameFrom.get(current.key());
                if (ancestor != null) {
                    double shortcutLength = nodePosition(ancestor).distanceTo(nodePosition(nextKey));
                    double shortcutScore = gScore.get(ancestor) + shortcutLength;
                    if (shortcutLength <= MAX_SHORTCUT_DISTANCE && shortcutScore <= tentativeScore
                            && !Boolean.FALSE.equals(clearEdges.get(new EdgeKey(ancestor, nextKey)))) {
                        // Defer the expensive sweep until this candidate is selected for expansion.
                        tentativeScore = shortcutScore;
                        nextParent = ancestor;
                    }
                }
                if (tentativeScore >= gScore.getOrDefault(nextKey, Double.POSITIVE_INFINITY)) {
                    continue;
                }

                cameFrom.put(nextKey, nextParent);
                gScore.put(nextKey, tentativeScore);
                double nextHeuristic = heuristic(nextPos);
                open.add(new OpenNode(
                        nextKey,
                        tentativeScore,
                        nextHeuristic,
                        tentativeScore + HEURISTIC_WEIGHT * nextHeuristic
                ));
            }
        }

        if (this.cancelled.getAsBoolean()) return null;
        boolean reachedRequestedTarget = reached && this.completeRoute;
        Path path = buildPath(cameFrom, bestKey, reachedRequestedTarget);
        if (debugSession != null && !this.cancelled.getAsBoolean()) {
            List<Vec3> closedPositions = closed.stream().map(AsyncFlightPathSearch::nodeCenter).toList();
            List<Vec3> openPositions = gScore.keySet().stream()
                    .filter(key -> !closed.contains(key))
                    .map(AsyncFlightPathSearch::nodeCenter)
                    .toList();
            DragonPathSearchDebug.publishGridSearch(
                    debugSession,
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
        return isSegmentClear(nodePosition(currentKey), this.target);
    }

    private boolean isNodeClear(BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        return this.collisionView.isClear(this.relativeBounds.move(center), Vec3.ZERO);
    }

    private boolean isEdgeClear(long fromKey, long toKey, Map<EdgeKey, Boolean> clearEdges) {
        return clearEdges.computeIfAbsent(new EdgeKey(fromKey, toKey),
                ignored -> isSegmentClear(nodePosition(fromKey), nodePosition(toKey)));
    }

    private boolean isSegmentClear(Vec3 from, Vec3 to) {
        Vec3 movement = to.subtract(from);
        int segments = Math.max(1, (int) Math.ceil(movement.length() / SWEEP_SEGMENT_LENGTH));
        Vec3 cursor = from;
        for (int segment = 1; segment <= segments; segment++) {
            if (this.cancelled.getAsBoolean()) return false;
            Vec3 end = segment == segments ? to : from.add(movement.scale((double) segment / segments));
            if (!this.collisionView.isClear(this.relativeBounds.move(cursor), end.subtract(cursor))) return false;
            cursor = end;
        }
        return true;
    }

    @Nullable
    private Parent repairParent(long key, Set<Long> closed, Map<Long, Double> gScore,
                                Map<EdgeKey, Boolean> clearEdges) {
        BlockPos position = BlockPos.of(key);
        Parent best = null;
        for (int[] offset : NEIGHBOR_OFFSETS) {
            if (this.cancelled.getAsBoolean()) return null;
            long neighbor = position.offset(offset[0], offset[1], offset[2]).asLong();
            if (!closed.contains(neighbor)) continue;
            double score = gScore.get(neighbor) + nodePosition(neighbor).distanceTo(nodePosition(key));
            if ((best == null || score < best.gScore()) && isEdgeClear(neighbor, key, clearEdges)) {
                best = new Parent(neighbor, score);
            }
        }
        return best;
    }

    private Vec3 nodePosition(long key) {
        return key == this.startNode.asLong() ? this.origin : nodeCenter(key);
    }

    private boolean withinBounds(int x, int y, int z) {
        return x >= this.minNode.getX() && x <= this.maxNode.getX()
                && y >= this.minNode.getY() && y <= this.maxNode.getY()
                && z >= this.minNode.getZ() && z <= this.maxNode.getZ();
    }

    private double heuristic(BlockPos pos) {
        return nodePosition(pos.asLong()).distanceTo(nodePosition(this.targetNode.asLong()));
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

    private record Parent(long key, double gScore) {
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
