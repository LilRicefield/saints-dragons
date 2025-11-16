package com.leon.saintsdragons.server.ai.navigation.pathfinding;

import java.util.Arrays;

/**
 * A binary heap implementation for efficient pathfinding node management.
 * Based on Baritone's BinaryHeapOpenSet - provides O(log n) insert/remove operations.
 *
 * This is significantly faster than vanilla Minecraft's pathfinding open set
 * for large search spaces, which is critical for dragon flight pathfinding.
 *
 * @author Baritone contributors (original implementation)
 * @author Adapted for Saint's Dragons
 */
public class DragonBinaryHeapOpenSet {

    /**
     * Initial capacity of the heap (2^10 = 1024 nodes).
     * Will automatically grow as needed.
     */
    private static final int INITIAL_CAPACITY = 1024;

    /**
     * The array backing the binary heap.
     * Index 0 is unused - heap starts at index 1 for easier parent/child math.
     */
    private DragonPathNode[] array;

    /**
     * Current number of nodes in the heap.
     */
    private int size;

    public DragonBinaryHeapOpenSet() {
        this(INITIAL_CAPACITY);
    }

    public DragonBinaryHeapOpenSet(int initialCapacity) {
        this.size = 0;
        this.array = new DragonPathNode[initialCapacity];
    }

    /**
     * Get the current number of nodes in the heap.
     */
    public int size() {
        return size;
    }

    /**
     * Check if the heap is empty.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Insert a new node into the heap.
     * Time complexity: O(log n)
     */
    public void insert(DragonPathNode node) {
        // Grow array if needed (double the size)
        if (size >= array.length - 1) {
            array = Arrays.copyOf(array, array.length << 1);
        }

        size++;
        node.heapPosition = size;
        array[size] = node;

        // Bubble up to maintain heap property
        update(node);
    }

    /**
     * Update a node's position in the heap after its cost has changed.
     * This is the "decrease-key" operation critical for A*.
     * Time complexity: O(log n)
     */
    public void update(DragonPathNode node) {
        int index = node.heapPosition;
        float cost = node.combinedCost;

        // Bubble up while parent has higher cost
        while (index > 1) {
            int parentIndex = index >>> 1; // Equivalent to index / 2
            DragonPathNode parentNode = array[parentIndex];

            if (parentNode.combinedCost <= cost) {
                break; // Heap property satisfied
            }

            // Swap with parent
            array[index] = parentNode;
            array[parentIndex] = node;
            node.heapPosition = parentIndex;
            parentNode.heapPosition = index;

            index = parentIndex;
        }
    }

    /**
     * Remove and return the node with the lowest combined cost.
     * Time complexity: O(log n)
     */
    public DragonPathNode removeLowest() {
        if (size == 0) {
            throw new IllegalStateException("Cannot remove from empty heap");
        }

        // Root has the lowest cost
        DragonPathNode result = array[1];
        result.heapPosition = -1; // Mark as not in heap

        if (size == 1) {
            array[1] = null;
            size = 0;
            return result;
        }

        // Move last element to root
        DragonPathNode lastNode = array[size];
        array[1] = lastNode;
        lastNode.heapPosition = 1;
        array[size] = null;
        size--;

        if (size < 2) {
            return result;
        }

        // Bubble down to maintain heap property
        int index = 1;
        float cost = lastNode.combinedCost;

        while (true) {
            int leftChild = index << 1; // index * 2

            if (leftChild > size) {
                break; // No children
            }

            // Find smaller child
            int smallerChild = leftChild;
            DragonPathNode smallerChildNode = array[leftChild];
            float smallerChildCost = smallerChildNode.combinedCost;

            int rightChild = leftChild + 1;
            if (rightChild <= size) {
                DragonPathNode rightChildNode = array[rightChild];
                float rightChildCost = rightChildNode.combinedCost;

                if (rightChildCost < smallerChildCost) {
                    smallerChild = rightChild;
                    smallerChildNode = rightChildNode;
                    smallerChildCost = rightChildCost;
                }
            }

            // If current node is smaller than both children, we're done
            if (cost <= smallerChildCost) {
                break;
            }

            // Swap with smaller child
            array[index] = smallerChildNode;
            array[smallerChild] = lastNode;
            lastNode.heapPosition = smallerChild;
            smallerChildNode.heapPosition = index;

            index = smallerChild;
        }

        return result;
    }

    /**
     * Clear the heap without deallocating the array.
     * Useful for reusing the same heap across multiple pathfinding operations.
     */
    public void clear() {
        // Clear references to help GC
        for (int i = 1; i <= size; i++) {
            array[i].heapPosition = -1;
            array[i] = null;
        }
        size = 0;
    }

    /**
     * Get statistics about the heap for debugging/profiling.
     */
    public String getStats() {
        return String.format("BinaryHeap[size=%d, capacity=%d, load=%.2f%%]",
                size, array.length, (size * 100.0f) / array.length);
    }
}
