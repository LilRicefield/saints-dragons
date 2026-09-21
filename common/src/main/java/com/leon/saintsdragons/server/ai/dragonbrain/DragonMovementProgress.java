package com.leon.saintsdragons.server.ai.dragonbrain;

import net.minecraft.world.phys.Vec3;

public final class DragonMovementProgress {
    private Vec3 destination;
    private long deadline;
    private long lastProgressAt;
    private double bestDistance;
    private double minimumProgress;
    private int stallTicks;
    private int lastRouteNode;
    private Status status = Status.IDLE;

    public void begin(Vec3 destination, Vec3 origin, long now, int timeoutTicks, int stallTicks, double minimumProgress) {
        if (!finite(destination) || !finite(origin) || timeoutTicks <= 0 || stallTicks <= 0
                || !Double.isFinite(minimumProgress) || minimumProgress <= 0) {
            throw new IllegalArgumentException("Invalid movement progress parameters");
        }
        this.destination = destination;
        deadline = now + timeoutTicks;
        lastProgressAt = now;
        bestDistance = origin.distanceTo(destination);
        this.minimumProgress = minimumProgress;
        this.stallTicks = stallTicks;
        lastRouteNode = -1;
        status = Status.IN_PROGRESS;
    }

    public Status observe(Vec3 position, long now, boolean valid, boolean arrived, boolean failed) {
        return observe(position, now, valid, arrived, failed, -1);
    }

    public Status observe(Vec3 position, long now, boolean valid, boolean arrived, boolean failed, int routeNode) {
        if (status != Status.IN_PROGRESS) return status;
        if (!valid || !finite(position)) return status = Status.INVALID;
        if (arrived) return status = Status.ARRIVED;
        if (failed) return status = Status.BLOCKED;
        if (now >= deadline) return status = Status.TIMED_OUT;
        double distance = position.distanceTo(destination);
        if (distance <= bestDistance - minimumProgress) {
            bestDistance = distance;
            lastProgressAt = now;
        }
        if (routeNode > lastRouteNode) {
            lastRouteNode = routeNode;
            lastProgressAt = now;
        }
        if (now - lastProgressAt >= stallTicks) return status = Status.BLOCKED;
        return status;
    }

    public void reset() {
        destination = null;
        status = Status.IDLE;
    }

    public Status status() {
        return status;
    }

    private static boolean finite(Vec3 position) {
        return position != null && Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z);
    }

    public enum Status { IDLE, IN_PROGRESS, ARRIVED, BLOCKED, INVALID, TIMED_OUT }
}
