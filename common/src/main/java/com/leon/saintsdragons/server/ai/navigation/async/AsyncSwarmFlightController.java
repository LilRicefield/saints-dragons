package com.leon.saintsdragons.server.ai.navigation.async;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class AsyncSwarmFlightController {
    private static final int PATH_RECALC_INTERVAL = 35;
    private static final int STUCK_THRESHOLD_TICKS = 30;
    private static final double STUCK_DISTANCE_SQ = 0.04D;
    private static final double LOOK_AHEAD_DISTANCE = 4.0D;
    private static final double NODE_REACHED_DISTANCE = 1.25D;
    private static final double COLLISION_SAMPLE_STEP = 0.4D;

    private final Mob mob;
    private final AsyncSwarmFlightMovementExecutor movementExecutor;
    private final List<Vec3> pathNodes = new ArrayList<>();

    private Vec3 waypoint;
    private Vec3 lastStuckCheckPos = Vec3.ZERO;
    private State state = State.IDLE;
    private double speed = 0.25D;
    private int pathIndex;
    private int recalcTicks;
    private int stuckTicks;
    private long pathGeneration;

    public AsyncSwarmFlightController(Mob mob) {
        this.mob = mob;
        this.movementExecutor = new AsyncSwarmFlightMovementExecutor(mob);
        this.lastStuckCheckPos = mob.position();
    }

    public void serverTick() {
        if (this.mob.level().isClientSide) {
            return;
        }

        if (this.waypoint == null || this.state == State.IDLE || this.state == State.FAILED) {
            this.movementExecutor.applyIdleFriction();
            return;
        }

        double arrivalDistance = calculateArrivalDistance();
        if (this.mob.position().distanceToSqr(this.waypoint) <= arrivalDistance * arrivalDistance) {
            clearWaypoint(false);
            return;
        }

        Vec3 lookAhead = this.state == State.FOLLOWING ? calculateLookAheadPoint() : this.waypoint;
        this.movementExecutor.executeMovement(lookAhead, this.waypoint, this.speed, arrivalDistance, this.pathNodes.isEmpty());

        if (isStuck()) {
            requestPath(this.waypoint);
            this.stuckTicks = 0;
        }

        if (this.state != State.DIRECT && ++this.recalcTicks >= PATH_RECALC_INTERVAL) {
            requestPath(this.waypoint);
        }
    }

    public void setWaypoint(Vec3 waypoint, double speed) {
        if (waypoint == null || this.mob.level().isClientSide) {
            return;
        }
        if (this.waypoint != null && this.waypoint.distanceToSqr(waypoint) < 1.0D && this.state != State.FAILED) {
            this.waypoint = waypoint;
            this.speed = speed;
            return;
        }

        this.waypoint = waypoint;
        this.speed = speed;
        requestPath(waypoint);
    }

    public void setDirectWaypoint(Vec3 waypoint, double speed) {
        if (waypoint == null || this.mob.level().isClientSide) {
            return;
        }

        this.waypoint = waypoint;
        this.speed = speed;
        this.pathNodes.clear();
        this.pathIndex = 0;
        this.state = State.DIRECT;
        this.recalcTicks = 0;
        this.stuckTicks = 0;
        this.pathGeneration++;
    }

    public void clearWaypoint() {
        clearWaypoint(true);
    }

    private void clearWaypoint(boolean zeroVelocity) {
        this.waypoint = null;
        this.pathNodes.clear();
        this.pathIndex = 0;
        this.state = State.IDLE;
        this.recalcTicks = 0;
        this.stuckTicks = 0;
        this.pathGeneration++;
        if (zeroVelocity) {
            this.movementExecutor.zeroVelocity();
        }
    }

    public boolean isIdle() {
        return this.state == State.IDLE || this.state == State.FAILED;
    }

    public Vec3 getWaypoint() {
        return this.waypoint;
    }

    private void requestPath(Vec3 target) {
        if (target == null || this.mob.level().isClientSide) {
            return;
        }

        this.state = State.CALCULATING;
        this.recalcTicks = 0;
        long generation = ++this.pathGeneration;
        AsyncDragonPathfinder.calculateSwarmFlyingPathAsync(this.mob, target, path -> {
            if (generation != this.pathGeneration || this.mob.isRemoved() || !this.mob.isAlive()) {
                return;
            }

            if (path == null || path.getNodeCount() == 0) {
                this.pathNodes.clear();
                this.pathIndex = 0;
                this.state = State.FOLLOWING;
                return;
            }

            cachePath(path, target, path.canReach());
            this.state = State.FOLLOWING;
        });
    }

    private void cachePath(Path path, Vec3 target, boolean canReach) {
        this.pathNodes.clear();
        this.pathIndex = 0;
        for (int i = 0; i < path.getNodeCount(); i++) {
            Node node = path.getNode(i);
            this.pathNodes.add(new Vec3(node.x + 0.5D, node.y + 0.5D, node.z + 0.5D));
        }
        if (canReach && (this.pathNodes.isEmpty()
                || this.pathNodes.get(this.pathNodes.size() - 1).distanceToSqr(target) > 1.0D)) {
            this.pathNodes.add(target);
        }
    }

    private Vec3 calculateLookAheadPoint() {
        if (this.pathNodes.isEmpty()) {
            return this.waypoint;
        }

        Vec3 pos = this.mob.position();
        while (this.pathIndex + 1 < this.pathNodes.size()
                && pos.distanceToSqr(this.pathNodes.get(this.pathIndex))
                < NODE_REACHED_DISTANCE * NODE_REACHED_DISTANCE) {
            this.pathIndex++;
        }

        int visibleIndex = this.pathIndex;
        for (int candidate = this.pathIndex + 1; candidate < this.pathNodes.size(); candidate++) {
            Vec3 candidateNode = this.pathNodes.get(candidate);
            if (pos.distanceToSqr(candidateNode) > LOOK_AHEAD_DISTANCE * LOOK_AHEAD_DISTANCE
                    || !isSegmentClear(pos, candidateNode)) {
                break;
            }
            visibleIndex = candidate;
        }
        this.pathIndex = visibleIndex;
        return this.pathNodes.get(visibleIndex);
    }

    private boolean isSegmentClear(Vec3 start, Vec3 end) {
        Vec3 offset = end.subtract(start);
        double distance = offset.length();
        if (distance < 1.0E-4D) {
            return true;
        }

        int samples = Math.max(1, (int) Math.ceil(distance / COLLISION_SAMPLE_STEP));
        for (int sample = 1; sample <= samples; sample++) {
            Vec3 step = offset.scale((double) sample / samples);
            if (!this.mob.level().noCollision(this.mob, this.mob.getBoundingBox().move(step))) {
                return false;
            }
        }
        return true;
    }

    private double calculateArrivalDistance() {
        return Math.max(1.0D, this.mob.getBbWidth() * 0.8D);
    }

    private boolean isStuck() {
        if (this.state == State.CALCULATING) {
            return false;
        }

        if (this.mob.tickCount % 10 != 0) {
            return false;
        }

        double movedSq = this.mob.position().distanceToSqr(this.lastStuckCheckPos);
        this.lastStuckCheckPos = this.mob.position();
        if (movedSq < STUCK_DISTANCE_SQ) {
            this.stuckTicks += 10;
        } else {
            this.stuckTicks = 0;
        }
        return this.stuckTicks >= STUCK_THRESHOLD_TICKS;
    }

    private enum State {
        IDLE,
        CALCULATING,
        FOLLOWING,
        DIRECT,
        FAILED
    }
}
