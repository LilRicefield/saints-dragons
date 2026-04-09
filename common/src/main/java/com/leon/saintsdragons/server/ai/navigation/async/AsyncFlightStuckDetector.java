package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

class AsyncFlightStuckDetector {
    private static final int RETRY_BACKOFF_TICKS = 30;

    private final Mob dragon;
    private int stuckTicks = 0;
    private Vec3 lastStuckCheckPosition;
    private int retryCount = 0;
    private int ticksUntilRetry = 0;

    AsyncFlightStuckDetector(Mob dragon) {
        this.dragon = dragon;
        this.lastStuckCheckPosition = dragon.position();
    }

    public boolean check(AsyncFlightController.PathState state, double stuckMovementThreshold, int stuckThresholdTicks) {
        if (state == AsyncFlightController.PathState.CALCULATING) {
            return false;
        }

        Vec3 currentPos = this.dragon.position();
        double movedSq = currentPos.distanceToSqr(this.lastStuckCheckPosition);
        if (movedSq < stuckMovementThreshold * stuckMovementThreshold) {
            this.stuckTicks++;
        } else {
            this.stuckTicks = 0;
            this.lastStuckCheckPosition = currentPos;
        }
        return this.stuckTicks >= stuckThresholdTicks;
    }

    public StuckAction handleStuck(int maxRetries) {
        this.stuckTicks = 0;
        this.retryCount++;
        this.ticksUntilRetry = RETRY_BACKOFF_TICKS;
        if (this.retryCount > maxRetries) {
            return StuckAction.FAILED;
        }
        return StuckAction.RETRY_SAME;
    }

    public void reset() {
        this.retryCount = 0;
        this.stuckTicks = 0;
        this.ticksUntilRetry = 0;
        this.lastStuckCheckPosition = this.dragon.position();
    }

    public void tickBackoff() {
        if (this.ticksUntilRetry > 0) {
            this.ticksUntilRetry--;
        }
    }

    public boolean isInBackoff() {
        return this.ticksUntilRetry > 0;
    }

    enum StuckAction {
        RETRY_SAME,
        FAILED
    }
}
