package com.leon.saintsdragons.server.ai.navigation.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
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
 * - Entity bounding box awareness for dragon size
 * - Multi-mode support: FLIGHT, WATER_ESCAPE, AMPHIBIOUS
 */
public class DragonPathfinder {

    /**
     * Pathfinding mode determines how the pathfinder treats different block types.
     */
    public enum PathMode {
        /** Air-only pathfinding for flying dragons. Water and solid blocks are impassable. */
        FLIGHT,

        /** Water escape mode for flying dragons stuck in water. Strongly prefers land over water. */
        WATER_ESCAPE,

        /** Amphibious mode for aquatic dragons. Can path through water, air, and along ground. */
        AMPHIBIOUS
    }

    private final Level level;
    private final int gridResolution; // How many blocks per pathfinding node
    private final int maxSearchNodes; // Maximum nodes to explore before giving up
    private final long timeoutMs; // Maximum time to spend pathfinding
    private final AABB entityBounds; // Bounding box of the entity (for collision checking)
    private final PathMode mode; // Pathfinding mode

    /**
     * Movement offsets for 3D pathfinding.
     * 26 directions: all combinations of {-1, 0, 1} for x, y, z except {0, 0, 0}
     */
    private static final int[][] MOVEMENT_OFFSETS_3D = generateMovementOffsets();

    public DragonPathfinder(Level level, int gridResolution, int maxSearchNodes, long timeoutMs, AABB entityBounds, PathMode mode) {
        this.level = level;
        this.gridResolution = gridResolution;
        this.maxSearchNodes = maxSearchNodes;
        this.timeoutMs = timeoutMs;
        this.entityBounds = entityBounds;
        this.mode = mode;
    }

    public DragonPathfinder(Level level, int gridResolution, int maxSearchNodes, long timeoutMs, AABB entityBounds) {
        this(level, gridResolution, maxSearchNodes, timeoutMs, entityBounds, PathMode.FLIGHT);
    }

    public DragonPathfinder(Level level, int gridResolution, int maxSearchNodes, long timeoutMs) {
        this(level, gridResolution, maxSearchNodes, timeoutMs, new AABB(-0.5, 0, -0.5, 0.5, 1, 0.5), PathMode.FLIGHT);
    }

    public DragonPathfinder(Level level, int gridResolution, int maxSearchNodes, long timeoutMs, PathMode mode) {
        this(level, gridResolution, maxSearchNodes, timeoutMs, new AABB(-0.5, 0, -0.5, 0.5, 1, 0.5), mode);
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

        // Check if start is passable
        if (!isPassable(startGrid)) {
            return null;
        }

        // Check if goal is passable
        // Special case for WATER_ESCAPE: If goal is solid ground with air above, it's a valid shore
        if (!isPassable(goalGrid)) {
            if (mode == PathMode.WATER_ESCAPE) {
                BlockState goalState = level.getBlockState(goalGrid);
                BlockPos aboveGoal = goalGrid.above();
                BlockState aboveState = level.getBlockState(aboveGoal);

                // If goal is solid ground with air above (and not water), it's a shore - accept it!
                if (!goalState.isAir() && goalState.blocksMotion() &&
                    aboveState.isAir() && level.getFluidState(aboveGoal).isEmpty()) {
                    // Adjust goal to be the AIR position above the ground (where dragon will stand)
                    // CRITICAL: Must snap adjusted goal back to grid!
                    goalGrid = snapToGrid(Vec3.atCenterOf(aboveGoal));
                } else {
                    return null;
                }
            } else {
                return null;
            }
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
     * Cost adjustments based on PathMode and terrain.
     */
    private float getMovementCost(BlockPos from, BlockPos to) {
        // Base cost is Euclidean distance
        float baseCost = heuristic(from, to);

        // Add penalties/bonuses based on mode and terrain
        int dy = to.getY() - from.getY();

        switch (mode) {
            case FLIGHT:
                // Flying dragons prefer horizontal movement and avoid ground
                if (dy != 0) {
                    baseCost *= 1.1f; // Slight penalty for vertical movement
                }
                // Check if near ground and penalize
                if (to.getY() < level.getSeaLevel() + 5) {
                    baseCost *= 1.2f; // Penalty for flying low
                }
                break;

            case WATER_ESCAPE:
                // Heavily prefer land over water (based on TDE's SemiWaterNodeEvaluator)
                boolean toIsWater = isPositionInWater(to);
                boolean toIsLand = !toIsWater && isPositionOnLand(to);

                if (toIsLand) {
                    // Strongly prefer land - subtract significant cost
                    baseCost = Math.max(0.1f, baseCost - 6.0f);
                } else if (toIsWater) {
                    boolean isWaterBorder = isWaterBorder(to);
                    if (isWaterBorder) {
                        // Water edges are less desirable than open water
                        baseCost += 4.0f;
                    }
                    // Water is passable but not preferred (cost remains base)
                }
                break;

            case AMPHIBIOUS:
                // Neutral costs for all terrain types
                boolean inWater = isPositionInWater(to);
                if (inWater) {
                    // Check if on underwater ground (more stable)
                    BlockPos below = to.below();
                    BlockState belowState = level.getBlockState(below);
                    if (!belowState.isAir() && belowState.blocksMotion()) {
                        // Slight preference for swimming along bottom
                        baseCost *= 0.8f;
                    }
                }
                break;
        }

        return baseCost;
    }

    /**
     * Check if a position is in water.
     */
    private boolean isPositionInWater(BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        return !fluidState.isEmpty();
    }

    /**
     * Check if a position is on land (air above solid ground).
     */
    private boolean isPositionOnLand(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            return false;
        }
        // Check if there's solid ground below within reasonable distance
        for (int i = 1; i <= 3; i++) {
            BlockPos below = pos.below(i);
            BlockState belowState = level.getBlockState(below);
            if (!belowState.isAir() && belowState.blocksMotion()) {
                return true;
            }
            // Stop if we hit water
            if (!level.getFluidState(below).isEmpty()) {
                return false;
            }
        }
        return false;
    }

    /**
     * Check if a water position is at the border (has non-water neighbors).
     */
    private boolean isWaterBorder(BlockPos pos) {
        if (!isPositionInWater(pos)) {
            return false;
        }
        // Check horizontal neighbors
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos neighbor = pos.offset(dx, 0, dz);
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.blocksMotion()) {
                    return true; // Adjacent to solid block
                }
            }
        }
        return false;
    }

    /**
     * Check if a position is passable based on the pathfinding mode.
     * Uses entity bounding box to check if dragon fits.
     */
    private boolean isPassable(BlockPos pos) {
        // Calculate bounding box at this position based on entity size
        double halfWidth = (entityBounds.maxX - entityBounds.minX) / 2.0;
        double height = entityBounds.maxY - entityBounds.minY;

        // Check blocks that the entity's bounding box would occupy
        int minX = (int) Math.floor(pos.getX() - halfWidth);
        int maxX = (int) Math.ceil(pos.getX() + halfWidth);
        int minY = pos.getY();
        int maxY = (int) Math.ceil(pos.getY() + height);
        int minZ = (int) Math.floor(pos.getZ() - halfWidth);
        int maxZ = (int) Math.ceil(pos.getZ() + halfWidth);

        // Check all blocks in the bounding box
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    FluidState fluidState = level.getFluidState(checkPos);

                    boolean isAir = state.isAir();
                    boolean isWater = !fluidState.isEmpty();
                    boolean isSolid = !isAir && state.blocksMotion();

                    switch (mode) {
                        case FLIGHT:
                            // Only air is passable for flying dragons (but allow standing on ground at start)
                            if (isSolid) {
                                // Allow solid ground at the bottom (dragon can take off from ground)
                                if (y != minY) {
                                    return false; // Solid blocks above ground level block movement
                                }
                                // Ground level solid is OK (standing/taking off from ground)
                            }
                            // Water blocks movement in flight mode
                            if (isWater) {
                                return false;
                            }
                            break;

                        case WATER_ESCAPE:
                            // Air and water are passable, solid blocks allowed ONLY at bottom (standing on shore)
                            if (isSolid) {
                                // Allow solid ground at the bottom (dragon will stand on shore)
                                if (y != minY) {
                                    return false; // Solid blocks above ground level block movement
                                }
                                // Ground level solid is OK (standing on shore)
                            }
                            break;

                        case AMPHIBIOUS:
                            // Air, water, and some ground positions are passable
                            if (isSolid) {
                                // Allow movement along solid ground if it's the bottom of the bounding box
                                if (y != minY) {
                                    return false; // Solid blocks above ground level block movement
                                }
                                // Ground level solid is OK (walking/swimming along bottom)
                            }
                            break;
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
     * Find the nearest shore position from a water position.
     * Uses breadth-first search to find the closest air block above solid ground.
     *
     * @param level      The world level
     * @param startPos   Starting position (usually the dragon's current position in water)
     * @param maxRadius  Maximum search radius in blocks
     * @return The nearest shore position, or null if none found within radius
     */
    @Nullable
    public static Vec3 findNearestShore(Level level, Vec3 startPos, int maxRadius) {
        BlockPos start = BlockPos.containing(startPos);

        // Quick check: if already on land, return current position
        if (!level.getFluidState(start).isEmpty() == false) {
            BlockPos landPos = findLandPositionBelow(level, start);
            if (landPos != null) {
                return Vec3.atBottomCenterOf(landPos);
            }
        }

        // BFS to find nearest shore
        List<BlockPos> queue = new ArrayList<>();
        Map<BlockPos, Boolean> visited = new HashMap<>();

        queue.add(start);
        visited.put(start, true);

        int[] dx = {-1, 1, 0, 0, 0, 0};
        int[] dy = {0, 0, -1, 1, 0, 0};
        int[] dz = {0, 0, 0, 0, -1, 1};

        int queueIndex = 0;
        while (queueIndex < queue.size()) {
            BlockPos current = queue.get(queueIndex++);

            // Check distance from start
            if (current.distManhattan(start) > maxRadius) {
                continue;
            }

            // Check if this position is land (air above solid ground)
            BlockState state = level.getBlockState(current);
            if (state.isAir()) {
                BlockPos landPos = findLandPositionBelow(level, current);
                if (landPos != null) {
                    // Found land! Return this position
                    return Vec3.atBottomCenterOf(landPos);
                }
            }

            // Add neighbors to queue
            for (int i = 0; i < 6; i++) {
                BlockPos neighbor = current.offset(dx[i], dy[i], dz[i]);

                // Bounds check
                if (neighbor.getY() < level.getMinBuildHeight() || neighbor.getY() > level.getMaxBuildHeight()) {
                    continue;
                }

                if (visited.containsKey(neighbor)) {
                    continue;
                }

                visited.put(neighbor, true);
                queue.add(neighbor);
            }
        }

        return null; // No shore found within radius
    }

    /**
     * Check if a position is above solid ground (land).
     * Returns the ground position if found, null otherwise.
     */
    @Nullable
    private static BlockPos findLandPositionBelow(Level level, BlockPos pos) {
        // Check up to 5 blocks below for solid ground
        for (int i = 0; i <= 5; i++) {
            BlockPos below = pos.below(i);
            BlockState belowState = level.getBlockState(below);

            // If we hit water, this isn't land
            if (!level.getFluidState(below).isEmpty()) {
                return null;
            }

            // If we hit solid ground, this is land!
            if (!belowState.isAir() && belowState.blocksMotion()) {
                return pos; // Return the air position above the ground
            }
        }
        return null; // No ground found within range
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
