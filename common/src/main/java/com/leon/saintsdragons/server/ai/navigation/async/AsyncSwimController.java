package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.ai.goals.base.GenericSwimSteeringController;
import com.leon.saintsdragons.server.ai.pathfinding.AsyncDragonPathfinder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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
    private static final double RETRY_TARGET_DISTANCE_SQR = 8.0D * 8.0D;
    private static final double RETRY_PROGRESS_DISTANCE_SQR = 8.0D * 8.0D;

    private final Mob host;
    private final GenericSwimSteeringController steering;
    private final List<Vec3> pathNodes = new ArrayList<>();
    private Vec3 target;
    private Vec3 pathTarget;
    private double speed;
    private float turnSpeed = 8.0F;
    private boolean liveTracking;
    private double liveArrivalDistance = 1.5D;
    private int currentPathIndex;
    private int recalcCooldown;
    private boolean calculating;
    private long pathRequestGeneration;
    private Vec3 lastStuckCheckPosition;
    private int stuckCheckTimer;
    private int stuckTicks;
    private int pathRetries;
    private Vec3 retryTarget;
    private Vec3 retryProgressOrigin;
    private int rejectedTargetCooldown;
    private Vec3 rejectedTarget;

    public AsyncSwimController(Mob host, GenericSwimSteeringController steering) {
        this.host = host;
        this.steering = steering;
        this.lastStuckCheckPosition = host.position();
        this.retryProgressOrigin = host.position();
    }

    public boolean trackTarget(Vec3 target, double speed, float turnSpeed) {
        if (isRejectedTarget(target)) {
            return false;
        }
        prepareRetryBudget(target);
        this.liveTracking = false;
        this.target = target;
        this.speed = speed;
        this.turnSpeed = turnSpeed;
        if (shouldRepath(target)) {
            requestPath(target);
        }
        return true;
    }

    public boolean trackMovingTarget(Vec3 target, double speed, float turnSpeed, double arrivalDistance) {
        if (target == null) {
            return false;
        }
        this.liveTracking = true;
        this.target = target;
        this.speed = speed;
        this.turnSpeed = turnSpeed;
        this.liveArrivalDistance = Math.max(0.5D, arrivalDistance);
        this.pathNodes.clear();
        this.pathTarget = null;
        this.currentPathIndex = 0;
        this.calculating = false;
        this.pathRequestGeneration++;
        return true;
    }

    public void serverTick() {
        tickRejectedTargetCooldown();
        if (target == null) {
            steering.slow(0.86D);
            return;
        }
        resetRetryBudgetAfterProgress();
        if (liveTracking) {
            tickLiveTracking();
            return;
        }

        if (recalcCooldown > 0) {
            recalcCooldown--;
        }

        Vec3 waypoint = getCurrentLookAhead();
        if (waypoint == null) {
            if (hasReachedPathEnd()) {
                resetRetryBudget(target);
                steering.slow(0.90D);
                return;
            }
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
                resetRetryBudget(target);
                steering.slow(0.90D);
                return;
            }
        }

        steering.moveToward(waypoint, speed, turnSpeed);
    }

    public void stop() {
        this.target = null;
        this.liveTracking = false;
        this.pathNodes.clear();
        this.pathTarget = null;
        this.currentPathIndex = 0;
        this.calculating = false;
        this.pathRequestGeneration++;
        resetStuckDetector();
        this.steering.slow(0.8D);
    }

    public void clear() {
        this.target = null;
        this.liveTracking = false;
        this.pathNodes.clear();
        this.pathTarget = null;
        this.currentPathIndex = 0;
        this.calculating = false;
        this.pathRequestGeneration++;
        this.recalcCooldown = 0;
        this.pathRetries = 0;
        this.retryTarget = null;
        this.retryProgressOrigin = this.host.position();
        this.rejectedTargetCooldown = 0;
        this.rejectedTarget = null;
        resetStuckDetector();
        this.steering.clear();
    }

    public boolean isMoving() {
        return steering.isMoving();
    }

    public boolean hasReachedPathEnd() {
        return !calculating && !pathNodes.isEmpty() && currentPathIndex >= pathNodes.size();
    }

    public boolean isNearPathEnd(double distance) {
        Vec3 endpoint = getPathEndpoint();
        return endpoint != null && host.position().distanceToSqr(endpoint) <= distance * distance;
    }

    @Nullable
    public Vec3 getPathEndpoint() {
        return pathNodes.isEmpty() ? null : pathNodes.get(pathNodes.size() - 1);
    }

    public DebugSnapshot getDebugSnapshot() {
        return new DebugSnapshot(
                target,
                getPathEndpoint(),
                List.copyOf(pathNodes),
                currentPathIndex,
                pathNodes.size(),
                calculating,
                steering.isMoving(),
                liveTracking,
                recalcCooldown,
                stuckTicks,
                pathRetries,
                rejectedTarget,
                rejectedTargetCooldown
        );
    }

    private boolean shouldRepath(Vec3 newTarget) {
        if (calculating || recalcCooldown > 0) {
            return false;
        }
        if (pathNodes.isEmpty()) {
            return true;
        }
        return pathTarget == null || pathTarget.distanceToSqr(newTarget) > 9.0D;
    }

    private void tickLiveTracking() {
        double distSq = host.position().distanceToSqr(target);
        double arrivalSq = liveArrivalDistance * liveArrivalDistance;
        if (distSq <= arrivalSq) {
            steering.slow(0.82D);
            return;
        }

        double distance = Math.sqrt(distSq);
        double easingRange = Math.max(2.0D, liveArrivalDistance * 2.5D);
        double easedSpeed = speed * Math.min(1.0D, Math.max(0.25D, (distance - liveArrivalDistance) / easingRange));
        steering.moveToward(target, easedSpeed, turnSpeed);
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
                this.pathTarget = requestedTarget;
                resetStuckDetector();
            } else {
                if (!this.pathNodes.isEmpty()) {
                    return;
                }
                recordPathFailure(requestedTarget);
            }
        });
    }

    private Vec3 getCurrentLookAhead() {
        if (pathNodes.isEmpty() || currentPathIndex >= pathNodes.size()) {
            return null;
        }
        return pathNodes.get(currentPathIndex);
    }

    private double arrivalDistanceSqr() {
        double arrival = Math.max(0.9D, Math.min(1.5D, host.getBbWidth() * 0.25D));
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
        pathTarget = null;
        currentPathIndex = 0;
        if (recordPathFailure(target)) {
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

    private void prepareRetryBudget(Vec3 candidate) {
        if (this.retryTarget != null
                && this.retryTarget.distanceToSqr(candidate) <= RETRY_TARGET_DISTANCE_SQR) {
            return;
        }
        resetRetryBudget(candidate);
    }

    private void resetRetryBudget(@Nullable Vec3 currentTarget) {
        this.pathRetries = 0;
        this.retryTarget = currentTarget;
        this.retryProgressOrigin = this.host.position();
    }

    private void resetRetryBudgetAfterProgress() {
        if (this.retryProgressOrigin == null
                || this.host.position().distanceToSqr(this.retryProgressOrigin) < RETRY_PROGRESS_DISTANCE_SQR) {
            return;
        }
        resetRetryBudget(this.target);
    }

    private boolean recordPathFailure(@Nullable Vec3 failedTarget) {
        if (failedTarget == null) {
            return false;
        }
        prepareRetryBudget(failedTarget);
        this.pathRetries++;
        if (this.pathRetries <= MAX_PATH_RETRIES) {
            return false;
        }
        rejectCurrentTarget(failedTarget);
        return true;
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
        this.retryTarget = null;
        this.retryProgressOrigin = this.host.position();
        this.pathNodes.clear();
        this.pathTarget = null;
        this.currentPathIndex = 0;
        this.calculating = false;
        this.steering.slow(0.6D);
    }

    public record DebugSnapshot(@Nullable Vec3 target,
                                @Nullable Vec3 endpoint,
                                List<Vec3> pathNodes,
                                int pathIndex,
                                int pathSize,
                                boolean calculating,
                                boolean moving,
                                boolean liveTracking,
                                int recalcCooldown,
                                int stuckTicks,
                                int retries,
                                @Nullable Vec3 rejectedTarget,
                                int rejectedCooldown) {
    }
}
