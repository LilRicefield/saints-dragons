package com.leon.saintsdragons.server.ai.navigation.pathfinding;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Smooths coarse pathfinding results into natural, flowing paths.
 * Based on techniques from Baritone and game AI research.
 *
 * Techniques:
 * 1. Line-of-sight shortcuts - Remove unnecessary waypoints
 * 2. Catmull-Rom spline interpolation - Smooth curves between waypoints
 * 3. Corner cutting - Round sharp corners
 *
 * @author Saint's Dragons Team
 */
public class PathSmoother {

    private final Level level;
    private final SmoothingConfig config;

    public PathSmoother(Level level) {
        this(level, SmoothingConfig.DEFAULT);
    }

    public PathSmoother(Level level, SmoothingConfig config) {
        this.level = level;
        this.config = config;
    }

    /**
     * Apply full smoothing pipeline to a path.
     *
     * @param rawPath The coarse path from pathfinding
     * @return Smoothed path with more waypoints but smoother curves
     */
    public List<Vec3> smoothPath(List<Vec3> rawPath) {
        if (rawPath == null || rawPath.size() < 2) {
            return rawPath; // Nothing to smooth
        }

        List<Vec3> path = new ArrayList<>(rawPath);

        // Step 1: Remove redundant waypoints via line-of-sight
        if (config.enableShortcuts) {
            path = applyLineOfSightShortcuts(path);
        }

        // Step 2: Interpolate smooth curves between waypoints
        if (config.enableSplineSmoothing) {
            path = applySplineSmoothing(path);
        }

        return path;
    }

    /**
     * Remove waypoints that can be skipped via direct line-of-sight.
     * Based on Baritone's pathfinding optimization.
     *
     * Example:
     * Before: A -> B -> C -> D
     * After:  A -> D (if A can see D directly)
     */
    private List<Vec3> applyLineOfSightShortcuts(List<Vec3> path) {
        if (path.size() < 3) {
            return path; // Need at least 3 points to shortcut
        }

        List<Vec3> shortcutPath = new ArrayList<>();
        shortcutPath.add(path.get(0)); // Always keep start

        int currentIndex = 0;

        while (currentIndex < path.size() - 1) {
            Vec3 current = path.get(currentIndex);

            // Try to skip as many waypoints as possible
            int farthestVisible = currentIndex + 1;

            for (int i = currentIndex + 2; i < path.size(); i++) {
                Vec3 candidate = path.get(i);

                if (hasLineOfSight(current, candidate)) {
                    farthestVisible = i;
                } else {
                    break; // Can't see further, stop
                }
            }

            // Jump to farthest visible waypoint
            currentIndex = farthestVisible;
            shortcutPath.add(path.get(currentIndex));
        }

        return shortcutPath;
    }

    /**
     * Check if there's clear line-of-sight between two points.
     * Uses raycasting to detect obstacles.
     */
    private boolean hasLineOfSight(Vec3 from, Vec3 to) {
        // Raycast through the world
        ClipContext clipContext = new ClipContext(
            from,
            to,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            null // No entity context needed for basic checks
        );

        HitResult result = level.clip(clipContext);

        // If we hit nothing or only hit at the target, line of sight is clear
        return result.getType() == HitResult.Type.MISS ||
               result.getLocation().distanceToSqr(to) < 0.1;
    }

    /**
     * Apply Catmull-Rom spline interpolation for smooth curves.
     * Creates flowing paths instead of sharp corners.
     *
     * This adds intermediate points between waypoints to create smooth arcs.
     */
    private List<Vec3> applySplineSmoothing(List<Vec3> path) {
        if (path.size() < 2) {
            return path;
        }

        List<Vec3> smoothed = new ArrayList<>();

        // For each segment between waypoints
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 p0 = (i > 0) ? path.get(i - 1) : path.get(i); // Previous (or current if first)
            Vec3 p1 = path.get(i);     // Current waypoint
            Vec3 p2 = path.get(i + 1); // Next waypoint
            Vec3 p3 = (i < path.size() - 2) ? path.get(i + 2) : path.get(i + 1); // After next (or next if last)

            // Add current waypoint
            smoothed.add(p1);

            // Interpolate points between p1 and p2
            int steps = config.interpolationSteps;
            for (int step = 1; step < steps; step++) {
                float t = (float) step / steps;
                Vec3 interpolated = catmullRomInterpolate(p0, p1, p2, p3, t);
                smoothed.add(interpolated);
            }
        }

        // Add final waypoint
        smoothed.add(path.get(path.size() - 1));

        return smoothed;
    }

    /**
     * Catmull-Rom spline interpolation.
     * Creates smooth curves that pass through control points.
     *
     * @param p0 Point before segment
     * @param p1 Segment start
     * @param p2 Segment end
     * @param p3 Point after segment
     * @param t  Interpolation factor (0 to 1)
     * @return Interpolated point on the curve
     */
    private Vec3 catmullRomInterpolate(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;

        // Catmull-Rom matrix coefficients
        float v0 = -0.5f * t3 + t2 - 0.5f * t;
        float v1 = 1.5f * t3 - 2.5f * t2 + 1.0f;
        float v2 = -1.5f * t3 + 2.0f * t2 + 0.5f * t;
        float v3 = 0.5f * t3 - 0.5f * t2;

        double x = v0 * p0.x + v1 * p1.x + v2 * p2.x + v3 * p3.x;
        double y = v0 * p0.y + v1 * p1.y + v2 * p2.y + v3 * p3.y;
        double z = v0 * p0.z + v1 * p1.z + v2 * p2.z + v3 * p3.z;

        return new Vec3(x, y, z);
    }

    /**
     * Configuration for path smoothing.
     */
    public static class SmoothingConfig {
        public final boolean enableShortcuts;
        public final boolean enableSplineSmoothing;
        public final int interpolationSteps; // Points to add between waypoints

        public SmoothingConfig(boolean shortcuts, boolean spline, int interpolationSteps) {
            this.enableShortcuts = shortcuts;
            this.enableSplineSmoothing = spline;
            this.interpolationSteps = interpolationSteps;
        }

        // Presets
        public static final SmoothingConfig DEFAULT = new SmoothingConfig(true, true, 4);
        public static final SmoothingConfig PERFORMANCE = new SmoothingConfig(true, false, 2);
        public static final SmoothingConfig QUALITY = new SmoothingConfig(true, true, 8);
        public static final SmoothingConfig SHORTCUTS_ONLY = new SmoothingConfig(true, false, 0);
        public static final SmoothingConfig NONE = new SmoothingConfig(false, false, 0);
    }

    /**
     * Get statistics about smoothing results.
     */
    public static class SmoothingStats {
        public final int originalWaypoints;
        public final int smoothedWaypoints;
        public final int waypointsRemoved;
        public final int waypointsAdded;

        public SmoothingStats(int original, int smoothed) {
            this.originalWaypoints = original;
            this.smoothedWaypoints = smoothed;
            this.waypointsRemoved = Math.max(0, original - smoothed);
            this.waypointsAdded = Math.max(0, smoothed - original);
        }

        @Override
        public String toString() {
            return String.format("SmoothingStats[%d -> %d waypoints, removed=%d, added=%d]",
                originalWaypoints, smoothedWaypoints, waypointsRemoved, waypointsAdded);
        }
    }

    /**
     * Smooth a path and return statistics.
     */
    public SmoothingResult smoothPathWithStats(List<Vec3> rawPath) {
        int originalSize = rawPath != null ? rawPath.size() : 0;
        List<Vec3> smoothed = smoothPath(rawPath);
        int smoothedSize = smoothed != null ? smoothed.size() : 0;

        return new SmoothingResult(smoothed, new SmoothingStats(originalSize, smoothedSize));
    }

    /**
     * Container for smoothed path and statistics.
     */
    public static class SmoothingResult {
        public final List<Vec3> path;
        public final SmoothingStats stats;

        public SmoothingResult(List<Vec3> path, SmoothingStats stats) {
            this.path = path;
            this.stats = stats;
        }
    }
}
