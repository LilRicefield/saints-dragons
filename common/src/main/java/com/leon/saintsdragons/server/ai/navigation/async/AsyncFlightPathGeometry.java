package com.leon.saintsdragons.server.ai.navigation.async;

import java.util.List;
import net.minecraft.world.phys.Vec3;

final class AsyncFlightPathGeometry {
    private AsyncFlightPathGeometry() {
    }

    static LookAheadResult calculateLookAhead(List<Vec3> pathNodes,
                                               int currentPathIndex,
                                               Vec3 position,
                                               double lookAheadDistance,
                                               double sharpCornerDot) {
        if (pathNodes.isEmpty()) {
            return null;
        }
        if (pathNodes.size() == 1) {
            return new LookAheadResult(pathNodes.get(0), 0);
        }

        int lastSegmentIndex = pathNodes.size() - 2;
        int searchStart = Math.max(0, Math.min(currentPathIndex, lastSegmentIndex));
        int searchEnd = Math.min(lastSegmentIndex, searchStart + 6);
        double bestDistSq = Double.MAX_VALUE;
        int bestIndex = searchStart;

        for (int i = searchStart; i <= searchEnd; i++) {
            Vec3 closest = closestPointOnSegment(position, pathNodes.get(i), pathNodes.get(i + 1));
            double distanceSq = position.distanceToSqr(closest);
            if (distanceSq < bestDistSq) {
                bestDistSq = distanceSq;
                bestIndex = i;
            }
        }

        Vec3 cursor = closestPointOnSegment(
                position,
                pathNodes.get(bestIndex),
                pathNodes.get(bestIndex + 1)
        );
        double remainingDistance = Math.max(0.0D, lookAheadDistance);

        for (int index = bestIndex; index < pathNodes.size() - 1; index++) {
            Vec3 corner = pathNodes.get(index + 1);
            Vec3 segment = corner.subtract(cursor);
            double segmentLength = segment.length();
            if (segmentLength >= remainingDistance && segmentLength > 1.0E-4D) {
                return new LookAheadResult(
                        cursor.add(segment.scale(remainingDistance / segmentLength)),
                        bestIndex
                );
            }

            remainingDistance -= segmentLength;
            if (index + 2 < pathNodes.size()) {
                Vec3 outgoing = pathNodes.get(index + 2).subtract(corner);
                Vec3 incomingHorizontal = new Vec3(segment.x, 0.0D, segment.z);
                Vec3 outgoingHorizontal = new Vec3(outgoing.x, 0.0D, outgoing.z);
                if (incomingHorizontal.lengthSqr() > 0.001D
                        && outgoingHorizontal.lengthSqr() > 0.001D
                        && incomingHorizontal.normalize().dot(outgoingHorizontal.normalize()) < sharpCornerDot) {
                    return new LookAheadResult(corner, bestIndex);
                }
            }
            cursor = corner;
        }

        return new LookAheadResult(pathNodes.get(pathNodes.size() - 1), bestIndex);
    }

    static Vec3 closestPointOnSegment(Vec3 point, Vec3 segmentStart, Vec3 segmentEnd) {
        Vec3 segment = segmentEnd.subtract(segmentStart);
        double segmentLengthSq = segment.lengthSqr();
        if (segmentLengthSq < 1.0E-4D) {
            return segmentStart;
        }
        double progress = Math.max(
                0.0D,
                Math.min(1.0D, point.subtract(segmentStart).dot(segment) / segmentLengthSq)
        );
        return segmentStart.add(segment.scale(progress));
    }

    record LookAheadResult(Vec3 target, int pathIndex) {
    }
}
