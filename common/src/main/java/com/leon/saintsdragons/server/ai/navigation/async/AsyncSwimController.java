package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.ai.goals.base.DragonSwimSteeringController;
import com.leon.saintsdragons.server.ai.pathfinding.AsyncDragonPathfinder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class AsyncSwimController {
    private static final int PATH_RECALC_COOLDOWN_TICKS = 20;
    private static final int STUCK_CHECK_INTERVAL_TICKS = 10;
    private static final int STUCK_THRESHOLD_TICKS = 30;
    private static final double STUCK_MOVEMENT_THRESHOLD_SQR = 0.18D * 0.18D;
    private static final int MAX_PATH_RETRIES = 4;
    private static final int REJECTED_TARGET_COOLDOWN_TICKS = 100;
    private static final double REJECTED_TARGET_DISTANCE_SQR = 4.0D * 4.0D;

    private final Mob host;
    private final DragonSwimSteeringController steering;
    private final List<Vec3> pathNodes = new ArrayList<>();
    private Vec3 target;
    private double speed;
    private float turnSpeed = 8.0F;
    private int currentPathIndex;
    private int recalcCooldown;
    private boolean calculating;
    private long pathRequestGeneration;
    private Vec3 lastStuckCheckPosition;
    private int stuckCheckTimer;
    private int stuckTicks;
    private int pathRetries;
    private int rejectedTargetCooldown;
    private Vec3 rejectedTarget;

    public AsyncSwimController(Mob host, DragonSwimSteeringController steering) {
        this.host = host;
        this.steering = steering;
        this.lastStuckCheckPosition = host.position();
    }

    public boolean trackTarget(Vec3 target, double speed, float turnSpeed) {
        if (isRejectedTarget(target)) {
            return false;
        }
        this.target = target;
        this.speed = speed;
        this.turnSpeed = turnSpeed;
        if (shouldRepath(target)) {
            requestPath(target);
        }
        return true;
    }

    public void serverTick() {
        tickRejectedTargetCooldown();
        if (target == null) {
            steering.slow(0.86D);
            return;
        }

        if (recalcCooldown > 0) {
            recalcCooldown--;
        }

        Vec3 waypoint = getCurrentLookAhead();
        if (waypoint == null) {
            if (!calculating && recalcCooldown <= 0) {
                requestPath(target);
            }
            steering.slow(0.90D);
            return;
        }

        tickStuckDetector(waypoint);
        if (host.position().distanceToSqr(waypoint) < arrivalDistanceSqr()) {
            currentPathIndex++;
            waypoint = getCurrentLookAhead();
            if (waypoint == null) {
                steering.slow(0.90D);
                return;
            }
        }

        steering.moveToward(waypoint, speed, turnSpeed);
    }

    public void stop() {
        this.target = null;
        this.pathNodes.clear();
        this.currentPathIndex = 0;
        this.calculating = false;
        this.pathRequestGeneration++;
        resetStuckDetector();
        this.steering.slow(0.8D);
    }

    public void clear() {
        this.target = null;
        this.pathNodes.clear();
        this.currentPathIndex = 0;
        this.calculating = false;
        this.pathRequestGeneration++;
        this.recalcCooldown = 0;
        this.pathRetries = 0;
        this.rejectedTargetCooldown = 0;
        this.rejectedTarget = null;
        resetStuckDetector();
        this.steering.clear();
    }

    public boolean isMoving() {
        return steering.isMoving();
    }

    private boolean shouldRepath(Vec3 newTarget) {
        if (calculating || recalcCooldown > 0) {
            return false;
        }
        if (pathNodes.isEmpty()) {
            return true;
        }
        Vec3 end = pathNodes.get(pathNodes.size() - 1);
        return end.distanceToSqr(newTarget) > 9.0D;
    }

    private void requestPath(Vec3 requestedTarget) {
        this.calculating = true;
        this.recalcCooldown = PATH_RECALC_COOLDOWN_TICKS;
        long requestGeneration = ++this.pathRequestGeneration;
        AsyncDragonPathfinder.calculateSwimPathAsync(this.host, requestedTarget, path -> {
            if (requestGeneration != this.pathRequestGeneration) {
                return;
            }
            this.calculating = false;
            if (path != null && !path.isEmpty()) {
                this.pathNodes.clear();
                this.currentPathIndex = 0;
                this.pathNodes.addAll(path);
                this.pathRetries = 0;
                resetStuckDetector();
            } else {
                this.pathRetries++;
                if (!this.pathNodes.isEmpty()) {
                    return;
                }
                if (this.pathRetries > MAX_PATH_RETRIES) {
                    rejectCurrentTarget(requestedTarget);
                }
            }
        });
    }

    private Vec3 getCurrentLookAhead() {
        if (pathNodes.isEmpty() || currentPathIndex >= pathNodes.size()) {
            return null;
        }

        Vec3 hostPos = host.position();
        int bestIndex = currentPathIndex;
        double bestDist = Double.MAX_VALUE;
        int searchEnd = Math.min(pathNodes.size(), currentPathIndex + 5);
        for (int i = currentPathIndex; i < searchEnd; i++) {
            double dist = hostPos.distanceToSqr(pathNodes.get(i));
            if (dist < bestDist) {
                bestDist = dist;
                bestIndex = i;
            }
        }
        currentPathIndex = bestIndex;
        int lookAheadIndex = Math.min(pathNodes.size() - 1, currentPathIndex + 2);
        return pathNodes.get(lookAheadIndex);
    }

    private double arrivalDistanceSqr() {
        double arrival = Math.max(1.2D, host.getBbWidth() * 0.6D);
        return arrival * arrival;
    }

    private void tickStuckDetector(Vec3 waypoint) {
        if (++stuckCheckTimer < STUCK_CHECK_INTERVAL_TICKS) {
            return;
        }
        stuckCheckTimer = 0;
        double movedSq = host.position().distanceToSqr(lastStuckCheckPosition);
        if (movedSq < STUCK_MOVEMENT_THRESHOLD_SQR) {
            stuckTicks += STUCK_CHECK_INTERVAL_TICKS;
        } else {
            stuckTicks = 0;
            lastStuckCheckPosition = host.position();
        }

        if (stuckTicks < STUCK_THRESHOLD_TICKS) {
            return;
        }

        stuckTicks = 0;
        pathNodes.clear();
        currentPathIndex = 0;
        pathRetries++;
        if (pathRetries > MAX_PATH_RETRIES) {
            rejectCurrentTarget(target);
            return;
        }
        if (!calculating && target != null) {
            recalcCooldown = 0;
            requestPath(target);
        }
    }

    private void resetStuckDetector() {
        this.stuckCheckTimer = 0;
        this.stuckTicks = 0;
        this.lastStuckCheckPosition = this.host.position();
    }

    private boolean isRejectedTarget(Vec3 candidate) {
        return candidate != null
                && rejectedTarget != null
                && rejectedTargetCooldown > 0
                && rejectedTarget.distanceToSqr(candidate) <= REJECTED_TARGET_DISTANCE_SQR;
    }

    private void tickRejectedTargetCooldown() {
        if (rejectedTargetCooldown > 0) {
            rejectedTargetCooldown--;
            if (rejectedTargetCooldown == 0) {
                rejectedTarget = null;
            }
        }
    }

    private void rejectCurrentTarget(Vec3 rejected) {
        this.rejectedTarget = rejected;
        this.rejectedTargetCooldown = REJECTED_TARGET_COOLDOWN_TICKS;
        this.target = null;
        this.pathRetries = 0;
        this.pathNodes.clear();
        this.currentPathIndex = 0;
        this.calculating = false;
        this.steering.slow(0.6D);
    }
}
