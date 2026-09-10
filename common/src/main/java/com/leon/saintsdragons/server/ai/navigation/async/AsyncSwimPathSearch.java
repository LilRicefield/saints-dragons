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
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class AsyncSwimPathSearch {
    private static final int MAX_VISITED_NODES = 50000;
    private static final double MAX_SHORTCUT_DISTANCE = 24.0D;
    private static final double SWEEP_SEGMENT_LENGTH = 4.0D;
    private static final double FOLLOW_SEGMENT_LENGTH = 2.0D;
    private static final double HEURISTIC_WEIGHT = 1.25D;
    private static final int GOAL_SWEEP_INTERVAL = 16;
    private static final double EPSILON = 1.0E-8D;
    private static final int[][] NEIGHBORS = createNeighborOffsets();

    private final boolean[] water;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final BlockPos minimum;
    private final int start;
    private final int goal;
    private final AABB relativeBounds;
    private final CollisionView collisions;
    private final BooleanSupplier cancelled;

    AsyncSwimPathSearch(boolean[] water, int sizeX, int sizeY, int sizeZ, BlockPos minimum,
                        int start, int goal, AABB relativeBounds, CollisionView collisions,
                        BooleanSupplier cancelled) {
        this.water = water;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.minimum = minimum;
        this.start = start;
        this.goal = goal;
        this.relativeBounds = relativeBounds;
        this.collisions = collisions;
        this.cancelled = cancelled;
    }

    @Nullable
    List<Vec3> findPath(@Nullable DragonPathSearchDebug.SearchSession debugSession) {
        return findPath(debugSession, System.nanoTime());
    }

    @Nullable
    List<Vec3> findPath(@Nullable DragonPathSearchDebug.SearchSession debugSession, long startedNanos) {
        if (cancelled.getAsBoolean()) return null;
        PriorityQueue<OpenNode> open = new PriorityQueue<>(Comparator.comparingDouble(OpenNode::fScore)
                .thenComparingDouble(OpenNode::hScore));
        Map<Integer, Integer> parents = new HashMap<>();
        Map<Integer, Double> scores = new HashMap<>();
        Map<EdgeKey, Double> edges = new HashMap<>();
        Set<Integer> closed = debugSession == null ? new HashSet<>() : new LinkedHashSet<>();
        List<Vec3> path = null;

        if (isWater(start) && isWater(goal)) {
            double directCost = edgeCost(start, goal, edges);
            // A floor-hugging shortcut must still compete with routes through deeper water.
            if (Double.isFinite(directCost) && directCost <= lowerBound(start, goal) + EPSILON) {
                if (start != goal) parents.put(goal, start);
                closed.add(start);
                closed.add(goal);
                path = reconstructPath(parents);
            } else {
                scores.put(start, 0.0D);
                double h = distance(start, goal);
                open.add(new OpenNode(start, 0.0D, h, HEURISTIC_WEIGHT * h));
            }
        }

        int visited = 0;
        while (path == null && !open.isEmpty() && visited < MAX_VISITED_NODES) {
            if (cancelled.getAsBoolean()) return null;
            OpenNode current = open.poll();
            int node = current.index();
            double score = scores.getOrDefault(node, Double.POSITIVE_INFINITY);
            if (Double.compare(current.gScore(), score) != 0 || closed.contains(node)) continue;

            Integer parent = parents.get(node);
            if (parent != null) {
                double verifiedScore = scores.get(parent) + edgeCost(parent, node, edges);
                if (!Double.isFinite(verifiedScore)) {
                    Parent repair = repairParent(node, closed, scores, edges);
                    if (repair == null) {
                        parents.remove(node);
                        scores.remove(node);
                        continue;
                    }
                    parents.put(node, repair.index());
                    verifiedScore = repair.score();
                }
                scores.put(node, verifiedScore);
                if (verifiedScore > score + EPSILON) {
                    open.add(new OpenNode(node, verifiedScore, current.hScore(),
                            verifiedScore + HEURISTIC_WEIGHT * current.hScore()));
                    continue;
                }
                score = verifiedScore;
            } else if (!collisions.isClear(relativeBounds.move(toWorld(node)), Vec3.ZERO)) {
                break;
            }
            if (cancelled.getAsBoolean()) return null;
            closed.add(node);
            visited++;
            if (node == goal) {
                path = reconstructPath(parents);
                break;
            }

            if (visited % GOAL_SWEEP_INTERVAL == 0) {
                double finishCost = edgeCost(node, goal, edges);
                if (Double.isFinite(finishCost) && finishCost <= lowerBound(node, goal) + EPSILON) {
                    parents.put(goal, node);
                    closed.add(goal);
                    path = reconstructPath(parents);
                    break;
                }
            }

            for (int[] offset : NEIGHBORS) {
                if (cancelled.getAsBoolean()) return null;
                int next = neighbor(node, offset);
                if (next < 0 || closed.contains(next)) continue;
                int nextParent = node;
                double nextScore = score + estimatedCost(node, next, edges);
                Integer ancestor = parents.get(node);
                if (ancestor != null && distance(ancestor, next) <= MAX_SHORTCUT_DISTANCE) {
                    double shortcutScore = scores.get(ancestor) + estimatedCost(ancestor, next, edges);
                    if (shortcutScore <= nextScore) {
                        nextParent = ancestor;
                        nextScore = shortcutScore;
                    }
                }
                if (nextScore >= scores.getOrDefault(next, Double.POSITIVE_INFINITY)) continue;
                parents.put(next, nextParent);
                scores.put(next, nextScore);
                double h = distance(next, goal);
                open.add(new OpenNode(next, nextScore, h, nextScore + HEURISTIC_WEIGHT * h));
            }
        }

        if (cancelled.getAsBoolean()) return null;
        if (debugSession != null) {
            DragonPathSearchDebug.publishGridSearch(debugSession, DragonPathSearchDebug.SearchType.SWIM,
                    toWorld(start), toWorld(goal), closed.stream().map(this::toWorld).toList(),
                    scores.keySet().stream().filter(node -> !closed.contains(node)).map(this::toWorld).toList(),
                    List.of(), path != null, startedNanos);
        }
        return path;
    }

    @Nullable
    private Parent repairParent(int node, Set<Integer> closed, Map<Integer, Double> scores,
                                Map<EdgeKey, Double> edges) {
        Parent best = null;
        for (int[] offset : NEIGHBORS) {
            if (cancelled.getAsBoolean()) return null;
            int neighbor = neighbor(node, offset);
            if (neighbor < 0 || !closed.contains(neighbor)) continue;
            double score = scores.get(neighbor) + edgeCost(neighbor, node, edges);
            if (Double.isFinite(score) && (best == null || score < best.score())) {
                best = new Parent(neighbor, score);
            }
        }
        return best;
    }

    private double estimatedCost(int from, int to, Map<EdgeKey, Double> edges) {
        return edges.getOrDefault(new EdgeKey(from, to), lowerBound(from, to));
    }

    private double edgeCost(int from, int to, Map<EdgeKey, Double> edges) {
        return edges.computeIfAbsent(new EdgeKey(from, to), ignored -> {
            double cost = waterSegmentCost(from, to);
            if (!Double.isFinite(cost)) return Double.POSITIVE_INFINITY;
            Vec3 origin = toWorld(from);
            Vec3 movement = toWorld(to).subtract(origin);
            int segments = Math.max(1, (int) Math.ceil(movement.length() / SWEEP_SEGMENT_LENGTH));
            Vec3 cursor = origin;
            for (int segment = 1; segment <= segments; segment++) {
                if (cancelled.getAsBoolean()) return Double.POSITIVE_INFINITY;
                Vec3 end = segment == segments ? toWorld(to) : origin.add(movement.scale((double) segment / segments));
                if (!collisions.isClear(relativeBounds.move(cursor), end.subtract(cursor))) {
                    return Double.POSITIVE_INFINITY;
                }
                cursor = end;
            }
            return cost;
        });
    }

    double waterSegmentCost(int from, int to) {
        if (!isWater(from) || !isWater(to)) return Double.POSITIVE_INFINITY;
        int x = x(from), y = y(from), z = z(from);
        int dx = x(to) - x, dy = y(to) - y, dz = z(to) - z;
        int sx = Integer.signum(dx), sy = Integer.signum(dy), sz = Integer.signum(dz);
        double stepX = dx == 0 ? Double.POSITIVE_INFINITY : 1.0D / Math.abs(dx);
        double stepY = dy == 0 ? Double.POSITIVE_INFINITY : 1.0D / Math.abs(dy);
        double stepZ = dz == 0 ? Double.POSITIVE_INFINITY : 1.0D / Math.abs(dz);
        double nextX = stepX * 0.5D, nextY = stepY * 0.5D, nextZ = stepZ * 0.5D;
        double length = distance(from, to);
        double cost = lowerBound(from, to);
        double progress = 0.0D;
        while (true) {
            if (cancelled.getAsBoolean()) return Double.POSITIVE_INFINITY;
            double next = Math.min(1.0D, Math.min(nextX, Math.min(nextY, nextZ)));
            if (y == 0 || !isWater(index(x, y - 1, z))) cost += 3.0D * length * (next - progress);
            if (next >= 1.0D) return cost;

            int axes = (nextX <= next + EPSILON ? 1 : 0)
                    | (nextY <= next + EPSILON ? 2 : 0)
                    | (nextZ <= next + EPSILON ? 4 : 0);
            // Check every cell touched at an edge/corner, including the side cells of diagonals.
            for (int subset = axes; subset > 0; subset = (subset - 1) & axes) {
                int nx = x + ((subset & 1) != 0 ? sx : 0);
                int ny = y + ((subset & 2) != 0 ? sy : 0);
                int nz = z + ((subset & 4) != 0 ? sz : 0);
                if (!withinBounds(nx, ny, nz) || !isWater(index(nx, ny, nz))) {
                    return Double.POSITIVE_INFINITY;
                }
            }
            if ((axes & 1) != 0) { x += sx; nextX += stepX; }
            if ((axes & 2) != 0) { y += sy; nextY += stepY; }
            if ((axes & 4) != 0) { z += sz; nextZ += stepZ; }
            progress = next;
        }
    }

    private int neighbor(int from, int[] offset) {
        int x = x(from), y = y(from), z = z(from);
        int nx = x + offset[0], ny = y + offset[1], nz = z + offset[2];
        if (!withinBounds(nx, ny, nz)) return -1;
        int next = index(nx, ny, nz);
        if (!isWater(next)
                || (offset[0] != 0 && !isWater(index(nx, y, z)))
                || (offset[1] != 0 && !isWater(index(x, ny, z)))
                || (offset[2] != 0 && !isWater(index(x, y, nz)))) return -1;
        return next;
    }

    private double lowerBound(int from, int to) {
        return distance(from, to) + Math.abs(y(to) - y(from)) * 1.25D;
    }

    private double distance(int from, int to) {
        int dx = x(to) - x(from), dy = y(to) - y(from), dz = z(to) - z(from);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private List<Vec3> reconstructPath(Map<Integer, Integer> parents) {
        List<Vec3> corners = new ArrayList<>();
        int node = goal;
        while (true) {
            corners.add(toWorld(node));
            Integer parent = parents.get(node);
            if (parent == null) break;
            node = parent;
        }
        Collections.reverse(corners);
        // Keep the existing swim follower's local look-ahead checks short along straight segments.
        List<Vec3> path = new ArrayList<>();
        path.add(corners.get(0));
        for (int i = 1; i < corners.size(); i++) {
            Vec3 from = corners.get(i - 1), to = corners.get(i);
            int steps = Math.max(1, (int) Math.ceil(from.distanceTo(to) / FOLLOW_SEGMENT_LENGTH));
            for (int step = 1; step <= steps; step++) {
                path.add(step == steps ? to : from.lerp(to, (double) step / steps));
            }
        }
        return path;
    }

    private Vec3 toWorld(int node) {
        return new Vec3(minimum.getX() + x(node) + 0.5D, minimum.getY() + y(node) + 0.5D,
                minimum.getZ() + z(node) + 0.5D);
    }

    private int x(int node) { return node % sizeX; }
    private int y(int node) { return (node / sizeX) % sizeY; }
    private int z(int node) { return node / (sizeX * sizeY); }
    private int index(int x, int y, int z) { return x + y * sizeX + z * sizeX * sizeY; }
    private boolean isWater(int node) { return node >= 0 && node < water.length && water[node]; }
    private boolean withinBounds(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
    }

    private static int[][] createNeighborOffsets() {
        List<int[]> offsets = new ArrayList<>(18);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    int distance = Math.abs(x) + Math.abs(y) + Math.abs(z);
                    if (distance > 0 && distance <= 2) offsets.add(new int[]{x, y, z});
                }
            }
        }
        return offsets.toArray(int[][]::new);
    }

    private record OpenNode(int index, double gScore, double hScore, double fScore) { }
    private record Parent(int index, double score) { }
    private record EdgeKey(int from, int to) {
        private EdgeKey {
            if (from > to) { int swap = from; from = to; to = swap; }
        }
    }

    @FunctionalInterface
    interface CollisionView {
        boolean isClear(AABB box, Vec3 movement);
    }
}
