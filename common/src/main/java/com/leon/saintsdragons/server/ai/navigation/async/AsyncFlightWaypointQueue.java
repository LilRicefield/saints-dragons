package com.leon.saintsdragons.server.ai.navigation.async;

import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

class AsyncFlightWaypointQueue {
    private final Queue<QueuedWaypoint> waypointQueue = new LinkedList<>();

    public void add(QueuedWaypoint waypoint) {
        this.waypointQueue.add(waypoint);
    }

    public QueuedWaypoint poll() {
        return this.waypointQueue.poll();
    }

    public void clear() {
        this.waypointQueue.clear();
    }

    public boolean isEmpty() {
        return this.waypointQueue.isEmpty();
    }

    public Stream<QueuedWaypoint> stream() {
        return this.waypointQueue.stream();
    }

    record QueuedWaypoint(Vec3 position,
                          double speed,
                          @Nullable AsyncFlightController.WaypointArrivalCallback onArrival,
                          boolean groundTransition) {
    }
}
