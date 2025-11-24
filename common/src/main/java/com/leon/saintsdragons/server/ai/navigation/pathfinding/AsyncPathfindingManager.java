package com.leon.saintsdragons.server.ai.navigation.pathfinding;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages asynchronous pathfinding requests for dragons.
 * Uses a thread pool to prevent main thread lag from pathfinding operations.
 *
 * Based on Baritone's async pathfinding approach but simplified for dragons.
 *
 * Key features:
 * - Thread pool for background pathfinding
 * - Automatic timeout handling
 * - Cancellation support
 * - Memory-safe result delivery
 */
public class AsyncPathfindingManager {

    private static AsyncPathfindingManager INSTANCE;

    // Thread pool for pathfinding
    private final ExecutorService executor;

    // Track active tasks for debugging/monitoring
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);

    // Configuration
    private static final int MAX_THREADS = 4; // Limit concurrent pathfinding operations
    private static final long DEFAULT_TIMEOUT_MS = 100; // Default timeout per pathfinding request

    private AsyncPathfindingManager() {
        // Create thread pool with daemon threads (won't prevent JVM shutdown)
        this.executor = new ThreadPoolExecutor(
            2,                          // Core threads
            MAX_THREADS,                // Max threads
            60L,                        // Keep-alive time
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "DragonPathfinder-" + threadNumber.getAndIncrement());
                    thread.setDaemon(true); // Daemon thread won't prevent shutdown
                    thread.setPriority(Thread.NORM_PRIORITY - 1); // Slightly lower priority
                    return thread;
                }
            }
        );
    }

    /**
     * Get singleton instance.
     */
    public static AsyncPathfindingManager getInstance() {
        if (INSTANCE == null) {
            synchronized (AsyncPathfindingManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AsyncPathfindingManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Request pathfinding from start to goal asynchronously.
     *
     * @param level          The world level
     * @param start          Starting position
     * @param goal           Goal position
     * @param gridResolution Grid resolution (1 = fine, 2-3 = coarse/faster)
     * @param timeoutMs      Maximum time to spend pathfinding (ms)
     * @param smoothPath     Whether to apply path smoothing
     * @param entityBounds   Entity bounding box (optional, uses default if null)
     * @param mode           Pathfinding mode (FLIGHT, WATER_ESCAPE, AMPHIBIOUS)
     * @return CompletableFuture that will contain the result
     */
    public CompletableFuture<PathfindingResult> requestPath(
        Level level,
        Vec3 start,
        Vec3 goal,
        int gridResolution,
        long timeoutMs,
        boolean smoothPath,
        net.minecraft.world.phys.AABB entityBounds,
        DragonPathfinder.PathMode mode
    ) {
        activeTasks.incrementAndGet();

        return CompletableFuture.supplyAsync(() -> {
            try {
                return computePath(level, start, goal, gridResolution, timeoutMs, smoothPath, entityBounds, mode);
            } catch (Exception e) {
                // Catch any unexpected errors
                System.err.println("Pathfinding error: " + e.getMessage());
                e.printStackTrace();
                return PathfindingResult.failure("Exception: " + e.getMessage(), 0);
            } finally {
                activeTasks.decrementAndGet();
                completedTasks.incrementAndGet();
            }
        }, executor);
    }

    /**
     * Convenience method with default timeout, smoothing, entity bounds, and FLIGHT mode.
     */
    public CompletableFuture<PathfindingResult> requestPath(
        Level level,
        Vec3 start,
        Vec3 goal,
        int gridResolution
    ) {
        return requestPath(level, start, goal, gridResolution, DEFAULT_TIMEOUT_MS, true, null, DragonPathfinder.PathMode.FLIGHT);
    }

    /**
     * Convenience method with default smoothing, entity bounds, and FLIGHT mode.
     */
    public CompletableFuture<PathfindingResult> requestPath(
        Level level,
        Vec3 start,
        Vec3 goal,
        int gridResolution,
        long timeoutMs
    ) {
        return requestPath(level, start, goal, gridResolution, timeoutMs, true, null, DragonPathfinder.PathMode.FLIGHT);
    }

    /**
     * Convenience method with entity bounds but default timeout, smoothing, and FLIGHT mode.
     */
    public CompletableFuture<PathfindingResult> requestPath(
        Level level,
        Vec3 start,
        Vec3 goal,
        int gridResolution,
        net.minecraft.world.phys.AABB entityBounds
    ) {
        return requestPath(level, start, goal, gridResolution, DEFAULT_TIMEOUT_MS, true, entityBounds, DragonPathfinder.PathMode.FLIGHT);
    }

    /**
     * Convenience method with pathfinding mode but default timeout, smoothing, and entity bounds.
     */
    public CompletableFuture<PathfindingResult> requestPath(
        Level level,
        Vec3 start,
        Vec3 goal,
        int gridResolution,
        DragonPathfinder.PathMode mode
    ) {
        return requestPath(level, start, goal, gridResolution, DEFAULT_TIMEOUT_MS, true, null, mode);
    }

    /**
     * Actually compute the path (runs on background thread).
     */
    private PathfindingResult computePath(
        Level level,
        Vec3 start,
        Vec3 goal,
        int gridResolution,
        long timeoutMs,
        boolean smoothPath,
        net.minecraft.world.phys.AABB entityBounds,
        DragonPathfinder.PathMode mode
    ) {
        long startTime = System.currentTimeMillis();

        // Create pathfinder instance with entity bounds and mode
        DragonPathfinder pathfinder = entityBounds != null
            ? new DragonPathfinder(level, gridResolution, 10000, timeoutMs, entityBounds, mode)
            : new DragonPathfinder(level, gridResolution, 10000, timeoutMs, mode);

        // Find path
        List<Vec3> path = pathfinder.findPath(start, goal);

        if (path == null) {
            long computeTime = System.currentTimeMillis() - startTime;
            // Check if it was a timeout or genuine failure
            if (computeTime >= timeoutMs) {
                return PathfindingResult.timeout(computeTime);
            } else {
                return PathfindingResult.failure("No path found", computeTime);
            }
        }

        // Apply smoothing if requested
        if (smoothPath && path.size() > 2) {
            PathSmoother smoother = new PathSmoother(level);
            path = smoother.smoothPath(path);
        }

        long computeTime = System.currentTimeMillis() - startTime;
        return PathfindingResult.success(path, computeTime);
    }

    /**
     * Get statistics about pathfinding performance.
     */
    public PathfindingStats getStats() {
        return new PathfindingStats(
            activeTasks.get(),
            completedTasks.get(),
            ((ThreadPoolExecutor) executor).getActiveCount(),
            ((ThreadPoolExecutor) executor).getQueue().size()
        );
    }

    /**
     * Shutdown the pathfinding manager (call on server stop).
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Statistics container.
     */
    public static class PathfindingStats {
        public final int activeRequests;
        public final int completedRequests;
        public final int activeThreads;
        public final int queuedRequests;

        public PathfindingStats(int activeRequests, int completedRequests, int activeThreads, int queuedRequests) {
            this.activeRequests = activeRequests;
            this.completedRequests = completedRequests;
            this.activeThreads = activeThreads;
            this.queuedRequests = queuedRequests;
        }

        @Override
        public String toString() {
            return String.format("PathfindingStats[active=%d, completed=%d, threads=%d, queued=%d]",
                activeRequests, completedRequests, activeThreads, queuedRequests);
        }
    }
}
