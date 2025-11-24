package com.leon.saintsdragons.server.ai.navigation.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;

/**
 * Enhanced path node for dragon pathfinding with binary heap support.
 * Based on Baritone's PathNode implementation.
 *
 * This extends vanilla Node but adds heap position tracking for efficient
 * decrease-key operations in the binary heap priority queue.
 */
public class DragonPathNode extends Node {

    /**
     * Position in the binary heap array. -1 means not in the open set.
     * Used for efficient decrease-key operations.
     */
    public int heapPosition = -1;

    /**
     * Combined cost (g + h) for A* algorithm.
     * g = actual cost from start
     * h = heuristic estimate to goal
     */
    public float combinedCost;

    /**
     * Parent node in the path (for path reconstruction).
     * Separate from vanilla's cameFrom to avoid conflicts.
     */
    public DragonPathNode parent;

    public DragonPathNode(int x, int y, int z) {
        super(x, y, z);
        this.heapPosition = -1;
        this.combinedCost = Float.MAX_VALUE;
        this.g = Float.MAX_VALUE; // CRITICAL: Initialize g to infinity so A* can update it
        this.parent = null;
    }

    public DragonPathNode(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Check if this node is currently in the open set (heap).
     */
    public boolean isOpen() {
        return heapPosition != -1;
    }

    /**
     * Update the combined cost (f = g + h).
     */
    public void updateCombinedCost(float costFromStart, float estimatedCostToGoal) {
        this.g = costFromStart;
        this.combinedCost = costFromStart + estimatedCostToGoal;
    }

    /**
     * Convert this node to a BlockPos for convenience.
     */
    public BlockPos asBlockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }

    @Override
    public int hashCode() {
        // Use vanilla's hash based on position
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Node)) {
            return false;
        }
        Node other = (Node) obj;
        return this.x == other.x && this.y == other.y && this.z == other.z;
    }
}
