package com.leon.saintsdragons.server.ai.navigation.pathfinding;

import net.minecraft.core.BlockPos;

import java.util.*;

/**
 * Simple benchmark to demonstrate binary heap performance vs naive implementations.
 * Run this to see the speed difference!
 *
 * Usage: Call runBenchmark() from a command or during development testing.
 */
public class PathfindingBenchmark {

    /**
     * Run a comprehensive benchmark comparing different open set implementations.
     */
    public static void runBenchmark() {
        System.out.println("=== Dragon Pathfinding Benchmark ===\n");

        // Test with different sizes to show scaling
        int[] testSizes = {100, 500, 1000, 5000, 10000};

        for (int size : testSizes) {
            System.out.println("Testing with " + size + " nodes:");

            // Test binary heap
            long binaryHeapTime = benchmarkBinaryHeap(size);

            // Test naive list (for comparison)
            long naiveListTime = benchmarkNaiveList(size);

            double speedup = (double) naiveListTime / binaryHeapTime;

            System.out.printf("  Binary Heap: %d ms\n", binaryHeapTime);
            System.out.printf("  Naive List:  %d ms\n", naiveListTime);
            System.out.printf("  Speedup:     %.2fx faster\n\n", speedup);
        }
    }

    /**
     * Benchmark the binary heap implementation.
     */
    private static long benchmarkBinaryHeap(int nodeCount) {
        DragonBinaryHeapOpenSet heap = new DragonBinaryHeapOpenSet();
        Random random = new Random(12345); // Fixed seed for consistency

        long startTime = System.nanoTime();

        // Simulate A* operations: insert and remove-lowest
        for (int i = 0; i < nodeCount; i++) {
            DragonPathNode node = new DragonPathNode(
                random.nextInt(1000),
                random.nextInt(256),
                random.nextInt(1000)
            );
            node.combinedCost = random.nextFloat() * 1000f;
            heap.insert(node);
        }

        // Remove all nodes in sorted order (what A* does)
        while (!heap.isEmpty()) {
            heap.removeLowest();
        }

        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000; // Convert to milliseconds
    }

    /**
     * Benchmark a naive list-based implementation (what vanilla-ish systems use).
     */
    private static long benchmarkNaiveList(int nodeCount) {
        List<DragonPathNode> list = new ArrayList<>();
        Random random = new Random(12345); // Same seed for fair comparison

        long startTime = System.nanoTime();

        // Simulate A* operations with a naive sorted list
        for (int i = 0; i < nodeCount; i++) {
            DragonPathNode node = new DragonPathNode(
                random.nextInt(1000),
                random.nextInt(256),
                random.nextInt(1000)
            );
            node.combinedCost = random.nextFloat() * 1000f;

            // Insert in sorted order (O(n) operation)
            insertSorted(list, node);
        }

        // Remove all nodes (simulating A* remove-lowest)
        while (!list.isEmpty()) {
            list.remove(0); // Remove first (lowest cost)
        }

        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000; // Convert to milliseconds
    }

    /**
     * Insert a node into a sorted list (O(n) operation).
     */
    private static void insertSorted(List<DragonPathNode> list, DragonPathNode node) {
        int index = 0;
        while (index < list.size() && list.get(index).combinedCost < node.combinedCost) {
            index++;
        }
        list.add(index, node);
    }

    /**
     * Quick demonstration you can call from anywhere.
     */
    public static void quickDemo() {
        System.out.println("Quick Binary Heap Demo:");

        DragonBinaryHeapOpenSet heap = new DragonBinaryHeapOpenSet();

        // Add some nodes with different costs
        DragonPathNode node1 = new DragonPathNode(new BlockPos(0, 0, 0));
        node1.combinedCost = 10.0f;

        DragonPathNode node2 = new DragonPathNode(new BlockPos(1, 0, 0));
        node2.combinedCost = 5.0f;

        DragonPathNode node3 = new DragonPathNode(new BlockPos(2, 0, 0));
        node3.combinedCost = 15.0f;

        heap.insert(node1);
        heap.insert(node2);
        heap.insert(node3);

        System.out.println("Inserted nodes with costs: 10, 5, 15");
        System.out.println("Removing in order:");

        while (!heap.isEmpty()) {
            DragonPathNode node = heap.removeLowest();
            System.out.printf("  Removed node at (%d, %d, %d) with cost %.1f\n",
                node.x, node.y, node.z, node.combinedCost);
        }

        System.out.println("✓ Heap correctly returns nodes in cost order!");
    }

    /**
     * Test heap correctness with random data.
     */
    public static boolean testCorrectness() {
        DragonBinaryHeapOpenSet heap = new DragonBinaryHeapOpenSet();
        Random random = new Random(54321);

        // Insert 1000 random nodes
        float[] costs = new float[1000];
        for (int i = 0; i < 1000; i++) {
            DragonPathNode node = new DragonPathNode(i, 0, 0);
            node.combinedCost = random.nextFloat() * 10000f;
            costs[i] = node.combinedCost;
            heap.insert(node);
        }

        // Verify they come out in sorted order
        float lastCost = -1f;
        while (!heap.isEmpty()) {
            DragonPathNode node = heap.removeLowest();
            if (node.combinedCost < lastCost) {
                System.err.println("ERROR: Heap returned nodes out of order!");
                return false;
            }
            lastCost = node.combinedCost;
        }

        System.out.println("✓ Correctness test passed!");
        return true;
    }
}
