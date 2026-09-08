package com.leon.saintsdragons.server.ai.navigation;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

final class GroundRouteProgress {
    private static final int MAX_DETOUR_SEGMENTS = 4;
    private static final double MAX_DETOUR_DISTANCE = 128.0D;
    private static final int MAX_RECENT_ENDPOINTS = 12;
    private final Deque<Vec3> recentEndpoints = new ArrayDeque<>();
    private Vec3 target;
    private Vec3 segmentStart;
    private Vec3 segmentEnd;
    private double segmentLength;
    private double minimumProgress;
    private double bestDistance;
    private double detourDistance;
    private int detourSegments;

    boolean canFollowPartial(Vec3 start, Vec3 end, Vec3 destination, boolean frontier,
                             double minimum, double routeLength) {
        prepare(start, destination);
        if (start.distanceTo(end) < minimum
                || recentEndpoints.stream().anyMatch(point -> point.distanceTo(end) < minimum)) {
            return false;
        }
        if (start.distanceTo(destination) - end.distanceTo(destination) >= minimum) {
            return true;
        }
        return frontier && detourSegments < MAX_DETOUR_SEGMENTS
                && detourDistance + routeLength <= MAX_DETOUR_DISTANCE;
    }

    void beginSegment(Vec3 start, Vec3 end, Vec3 destination, double length, double minimum) {
        prepare(start, destination);
        segmentStart = start;
        segmentEnd = end;
        segmentLength = length;
        minimumProgress = minimum;
    }

    boolean completeSegment(Vec3 position, double arrivalTolerance) {
        if (segmentStart == null || position.distanceTo(segmentEnd) > arrivalTolerance
                || position.distanceTo(segmentStart) < minimumProgress) {
            return false;
        }
        double distance = position.distanceTo(target);
        if (distance <= bestDistance - minimumProgress) {
            bestDistance = distance;
            detourSegments = 0;
            detourDistance = 0.0D;
        } else {
            detourSegments++;
            detourDistance += segmentLength;
        }
        remember(segmentEnd);
        segmentStart = null;
        segmentEnd = null;
        return detourSegments <= MAX_DETOUR_SEGMENTS && detourDistance <= MAX_DETOUR_DISTANCE;
    }

    private void prepare(Vec3 start, Vec3 destination) {
        if (target == null || target.distanceToSqr(destination) > 16.0D) {
            reset();
            target = destination;
            bestDistance = start.distanceTo(destination);
            remember(start);
        }
    }

    private void remember(Vec3 point) {
        recentEndpoints.addLast(point);
        while (recentEndpoints.size() > MAX_RECENT_ENDPOINTS) {
            recentEndpoints.removeFirst();
        }
    }

    void reset() {
        target = null;
        segmentStart = null;
        segmentEnd = null;
        detourSegments = 0;
        detourDistance = 0.0D;
        recentEndpoints.clear();
    }
}
