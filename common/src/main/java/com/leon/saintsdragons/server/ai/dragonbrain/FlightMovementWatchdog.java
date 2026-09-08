package com.leon.saintsdragons.server.ai.dragonbrain;

public final class FlightMovementWatchdog {
    private static final int GRACE_TICKS = 10;
    private static final int RETRY_TICKS = 20;
    private int missingTicks;
    private long nextRecoveryTick;

    public boolean tick(long tick, boolean missingMovement) {
        if (!missingMovement) {
            missingTicks = 0;
            return false;
        }
        missingTicks = Math.min(GRACE_TICKS, missingTicks + 1);
        if (missingTicks < GRACE_TICKS || tick < nextRecoveryTick) return false;
        missingTicks = 0;
        nextRecoveryTick = tick + RETRY_TICKS;
        return true;
    }

    public int missingTicks() {
        return missingTicks;
    }
}
