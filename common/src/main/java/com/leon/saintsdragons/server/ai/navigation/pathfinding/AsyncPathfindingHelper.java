package com.leon.saintsdragons.server.ai.navigation.pathfinding;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Helper class demonstrating how to use async pathfinding safely.
 * Provides convenience methods and handles thread-safety.
 *
 * IMPORTANT: Callbacks run on the pathfinding thread, NOT the main thread!
 * If you need to modify the entity, use scheduleOnMainThread().
 */
public class AsyncPathfindingHelper {

    /**
     * Request a path and execute callback when done.
     *
     * Example usage:
     * <pre>
     * AsyncPathfindingHelper.requestPath(
     *     dragon.level(),
     *     dragon.position(),
     *     targetPos,
     *     2,  // Grid resolution
     *     dragon.getBoundingBox(), // Entity bounding box
     *     result -> {
     *         if (result.isSuccess()) {
     *             // This runs on background thread!
     *             // Schedule any entity modification on main thread
     *             scheduleOnMainThread(dragon.level(), () -> {
     *                 dragon.startFollowingPath(result.getPath());
     *             });
     *         }
     *     }
     * );
     * </pre>
     */
    public static CompletableFuture<PathfindingResult> requestPath(
        ServerLevel level,
        Vec3 start,
        Vec3 goal,
        int gridResolution,
        net.minecraft.world.phys.AABB entityBounds,
        Consumer<PathfindingResult> callback
    ) {
        AsyncPathfindingManager manager = AsyncPathfindingManager.getInstance();

        return manager.requestPath(level, start, goal, gridResolution, entityBounds)
            .thenApply(result -> {
                // Execute callback when done
                if (callback != null) {
                    callback.accept(result);
                }
                return result;
            });
    }

    /**
     * Convenience method without entity bounds (uses default).
     */
    public static CompletableFuture<PathfindingResult> requestPath(
        ServerLevel level,
        Vec3 start,
        Vec3 goal,
        int gridResolution,
        Consumer<PathfindingResult> callback
    ) {
        return requestPath(level, start, goal, gridResolution, null, callback);
    }

    /**
     * Request a path synchronously (blocks until complete).
     * USE SPARINGLY - defeats the purpose of async!
     *
     * Only use this for:
     * - Testing/debugging
     * - One-time setup paths
     * - Commands/tools
     */
    public static PathfindingResult requestPathSync(
        ServerLevel level,
        Vec3 start,
        Vec3 goal,
        int gridResolution,
        long timeoutMs
    ) {
        try {
            return AsyncPathfindingManager.getInstance()
                .requestPath(level, start, goal, gridResolution, timeoutMs)
                .get(); // Block until complete
        } catch (Exception e) {
            return PathfindingResult.failure("Sync request failed: " + e.getMessage(), 0);
        }
    }

    /**
     * Schedule a task to run on the main server thread.
     * Use this when you need to modify entities from async callbacks.
     *
     * @param level The server level
     * @param task  The task to run on main thread
     */
    public static void scheduleOnMainThread(ServerLevel level, Runnable task) {
        level.getServer().execute(task);
    }

    /**
     * Example: Complete pathfinding workflow for a mob.
     * Shows how to request path, handle result, and update mob safely.
     */
    public static void exampleUsage(Mob mob, Vec3 targetPos) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return; // Client side or not a server level
        }

        // Request path asynchronously
        requestPath(
            serverLevel,
            mob.position(),
            targetPos,
            2, // Grid resolution
            result -> {
                // This callback runs on background thread!

                if (result.isSuccess()) {
                    List<Vec3> path = result.getPath();

                    // Schedule entity modification on main thread
                    scheduleOnMainThread(serverLevel, () -> {
                        // Now it's safe to modify the mob
                        System.out.println("Path found with " + path.size() + " waypoints in " +
                            result.getComputeTimeMs() + "ms");

                        // TODO: Make mob follow the path
                        // mob.getNavigation().moveTo(path.get(0).x, path.get(0).y, path.get(0).z, 1.0);
                    });

                } else {
                    // Path failed
                    System.out.println("Pathfinding failed: " + result.getFailureReason());
                }
            }
        );
    }

    /**
     * Get current pathfinding statistics.
     */
    public static AsyncPathfindingManager.PathfindingStats getStats() {
        return AsyncPathfindingManager.getInstance().getStats();
    }
}
