package com.leon.saintsdragons.server.ai.navigation.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplified 3D A* pathfinder for dragon flight.
 * This is a demonstration/test implementation showing how the BinaryHeapOpenSet works.
 *
 * Key features:
 * - 3D pathfinding (not limited to ground plane)
 * - Coarse grid (2-block spacing to reduce search space)
 * - Timeout-based to prevent lag
 * - Uses binary heap for efficiency
 *
 * NOT YET INTEGRATED - This is for testing and demonstration only!
 */
public class DragonPathfinder {

    private final Level level;
    private final int gridResolution; // How many blocks per pathfinding node
    private final int maxSearchNodes; // Maximum nodes to explore before giving up
    private final long timeoutMs; // Maximum time to spend pathfinding

    /**
     * Movement offsets for 3D pathfinding.
     * 26 directions: all combinations of {-1, 0, 1} for x, y, z except {0, 0, 0}
     */
    private static final int[][] MOVEMENT_OFFSETS_3D = generateMovementOffsets();

    public DragonPathfinder(Level level, int gridResolution, int maxSearchNodes, long timeoutMs) {
        this.level = level;
        this.gridResolution = gridResolution;
        this.maxSearchNodes = maxSearchNodes;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Find a path from start to goal using A*.
     *
     * @param start Starting position
     * @param goal  Goal position
     * @return List of waypoints, or null if no path found
     */
    @Nullable
    public List<Vec3> findPath(Vec3 start, Vec3 goal) {
        long startTime = System.currentTimeMillis();

        // Snap to grid
        BlockPos startGrid = snapToGrid(start);
        BlockPos goalGrid = snapToGrid(goal);

        if (startGrid.equals(goalGrid)) {
            // Already at goal
            List<Vec3> path = new ArrayList<>();
            path.add(goal);
            return path;
        }

        // Initialize data structures
        DragonBinaryHeapOpenSet openSet = new DragonBinaryHeapOpenSet();
        Map<BlockPos, DragonPathNode> nodeMap = new HashMap<>();

        // Create start node
        DragonPathNode startNode = getOrCreateNode(nodeMap, startGrid);
        startNode.updateCombinedCost(0, heuristic(startGrid, goalGrid));
        openSet.insert(startNode);

        int nodesExplored = 0;

        // A* main loop
        while (!openSet.isEmpty() && nodesExplored < maxSearchNodes) {
            // Check timeout
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                // System.out.println("Pathfinding timeout after exploring " + nodesExplored + " nodes");
                return null; // Timeout
            }

            DragonPathNode current = openSet.removeLowest();
            nodesExplored++;

            // Check if we reached the goal
            if (current.asBlockPos().equals(goalGrid)) {
                // Reconstruct path
                return reconstructPath(current, goal);
            }

            // Explore neighbors
            for (int[] offset : MOVEMENT_OFFSETS_3D) {
                int newX = current.x + offset[0] * gridResolution;
                int newY = current.y + offset[1] * gridResolution;
                int newZ = current.z + offset[2] * gridResolution;

                // Basic bounds check
                if (newY < level.getMinBuildHeight() || newY > level.getMaxBuildHeight()) {
                    continue;
                }

                BlockPos neighborPos = new BlockPos(newX, newY, newZ);

                // Check if passable (simplified - just check if solid blocks are in the way)
                if (!isPassable(neighborPos)) {
                    continue;
                }

                DragonPathNode neighbor = getOrCreateNode(nodeMap, neighborPos);

                // Calculate costs
                float movementCost = getMovementCost(current.asBlockPos(), neighborPos);
                float tentativeG = current.g + movementCost;

                // If this path to neighbor is better than any previous one
                if (tentativeG < neighbor.g) {
                    // Update neighbor
                    neighbor.parent = current;
                    neighbor.updateCombinedCost(tentativeG, heuristic(neighborPos, goalGrid));

                    // Add to or update in open set
                    if (neighbor.isOpen()) {
                        openSet.update(neighbor);
                    } else {
                        openSet.insert(neighbor);
                    }
                }
            }
        }

        // System.out.println("No path found after exploring " + nodesExplored + " nodes");
        return null; // No path found
    }

    /**
     * Heuristic function for A* (Euclidean distance).
     */
    private float heuristic(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Get movement cost between two positions.
     * Higher cost = less desirable path.
     */
    private float getMovementCost(BlockPos from, BlockPos to) {
        // Base cost is Euclidean distance
        float baseCost = heuristic(from, to);

        // Add penalties for undesirable movements
        int dy = to.getY() - from.getY();

        // Slight penalty for vertical movement (encourage horizontal flight)
        if (dy != 0) {
            baseCost *= 1.1f;
        }

        // TODO: Add more sophisticated costs:
        // - Penalty for flying near ground
        // - Penalty for tight spaces
        // - Bonus for open air

        return baseCost;
    }

    /**
     * Check if a position is passable for flight.
     * Simplified version - just checks if blocks are solid.
     */
    private boolean isPassable(BlockPos pos) {
        // Check a 3x3x3 box around the position for obstacles
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(checkPos);
                    if (!state.isAir() && state.blocksMotion()) {
                        return false; // Obstacle detected
                    }
                }
            }
        }
        return true;
    }

    /**
     * Snap a position to the pathfinding grid.
     */
    private BlockPos snapToGrid(Vec3 pos) {
        int x = ((int) Math.floor(pos.x / gridResolution)) * gridResolution;
        int y = ((int) Math.floor(pos.y / gridResolution)) * gridResolution;
        int z = ((int) Math.floor(pos.z / gridResolution)) * gridResolution;
        return new BlockPos(x, y, z);
    }

    /**
     * Get or create a node from the map.
     */
    private DragonPathNode getOrCreateNode(Map<BlockPos, DragonPathNode> nodeMap, BlockPos pos) {
        return nodeMap.computeIfAbsent(pos, p -> new DragonPathNode(p));
    }

    /**
     * Reconstruct the path from start to goal by following parent pointers.
     */
    private List<Vec3> reconstructPath(DragonPathNode goalNode, Vec3 actualGoal) {
        List<Vec3> path = new ArrayList<>();

        DragonPathNode current = goalNode;
        while (current != null) {
            path.add(0, new Vec3(current.x, current.y, current.z));
            current = (DragonPathNode) current.parent;
        }

        // Replace last waypoint with actual goal (not snapped to grid)
        if (!path.isEmpty()) {
            path.set(path.size() - 1, actualGoal);
        }

        return path;
    }

    /**
     * Generate 26 movement directions for 3D pathfinding.
     */
    private static int[][] generateMovementOffsets() {
        List<int[]> offsets = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue; // Skip no movement
                    }
                    offsets.add(new int[]{dx, dy, dz});
                }
            }
        }
        return offsets.toArray(new int[0][]);
    }

    /**
     * Get statistics about the last pathfinding operation.
     */
    public static class PathfindingStats {
        public int nodesExplored;
        public long timeMs;
        public boolean success;
        public int pathLength;

        @Override
        public String toString() {
            return String.format("Pathfinding[success=%s, nodes=%d, time=%dms, pathLength=%d]",
                    success, nodesExplored, timeMs, pathLength);
        }
    }
}
