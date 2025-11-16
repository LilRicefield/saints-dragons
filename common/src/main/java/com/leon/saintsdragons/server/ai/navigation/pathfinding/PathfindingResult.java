package com.leon.saintsdragons.server.ai.navigation.pathfinding;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Result of a pathfinding operation.
 * Can be success (with path), failure, timeout, or cancelled.
 */
public class PathfindingResult {

    private final ResultType type;
    private final List<Vec3> path;
    private final String failureReason;
    private final long computeTimeMs;

    public enum ResultType {
        SUCCESS,        // Path found successfully
        FAILURE,        // No path exists
        TIMEOUT,        // Took too long, gave up
        CANCELLED       // Request was cancelled
    }

    // Private constructor - use static factory methods
    private PathfindingResult(ResultType type, @Nullable List<Vec3> path, @Nullable String failureReason, long computeTimeMs) {
        this.type = type;
        this.path = path;
        this.failureReason = failureReason;
        this.computeTimeMs = computeTimeMs;
    }

    // Factory methods for creating results
    public static PathfindingResult success(List<Vec3> path, long computeTimeMs) {
        return new PathfindingResult(ResultType.SUCCESS, path, null, computeTimeMs);
    }

    public static PathfindingResult failure(String reason, long computeTimeMs) {
        return new PathfindingResult(ResultType.FAILURE, null, reason, computeTimeMs);
    }

    public static PathfindingResult timeout(long computeTimeMs) {
        return new PathfindingResult(ResultType.TIMEOUT, null, "Pathfinding timeout", computeTimeMs);
    }

    public static PathfindingResult cancelled() {
        return new PathfindingResult(ResultType.CANCELLED, null, "Cancelled by user", 0);
    }

    // Getters
    public ResultType getType() {
        return type;
    }

    public boolean isSuccess() {
        return type == ResultType.SUCCESS;
    }

    @Nullable
    public List<Vec3> getPath() {
        return path;
    }

    @Nullable
    public String getFailureReason() {
        return failureReason;
    }

    public long getComputeTimeMs() {
        return computeTimeMs;
    }

    @Override
    public String toString() {
        switch (type) {
            case SUCCESS:
                return String.format("PathfindingResult[SUCCESS, %d waypoints, %dms]",
                    path != null ? path.size() : 0, computeTimeMs);
            case FAILURE:
                return String.format("PathfindingResult[FAILURE, reason=%s, %dms]",
                    failureReason, computeTimeMs);
            case TIMEOUT:
                return String.format("PathfindingResult[TIMEOUT, %dms]", computeTimeMs);
            case CANCELLED:
                return "PathfindingResult[CANCELLED]";
            default:
                return "PathfindingResult[UNKNOWN]";
        }
    }
}
