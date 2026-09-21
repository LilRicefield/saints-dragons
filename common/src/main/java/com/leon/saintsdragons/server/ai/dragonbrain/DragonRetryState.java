package com.leon.saintsdragons.server.ai.dragonbrain;

public final class DragonRetryState {
    private final int maxAttempts;
    private final int initialDelay;
    private final int maxDelay;
    private int attempts;
    private long nextAttemptAt;

    public DragonRetryState(int maxAttempts, int initialDelay, int maxDelay) {
        if (maxAttempts <= 0 || initialDelay <= 0 || maxDelay < initialDelay) {
            throw new IllegalArgumentException("Invalid retry parameters");
        }
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
    }

    public boolean beginAttempt(long now) {
        if (exhausted() || now < nextAttemptAt) return false;
        attempts++;
        return true;
    }

    public void failed(long now) {
        long delay = (long) initialDelay << Math.min(30, Math.max(0, attempts - 1));
        nextAttemptAt = now + Math.min(maxDelay, delay);
    }

    public boolean exhausted() {
        return attempts >= maxAttempts;
    }

    public int attempts() {
        return attempts;
    }

    public void reset() {
        attempts = 0;
        nextAttemptAt = 0L;
    }
}
