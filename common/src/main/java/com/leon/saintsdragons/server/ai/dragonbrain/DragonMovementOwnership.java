package com.leon.saintsdragons.server.ai.dragonbrain;

import java.util.function.Supplier;

public final class DragonMovementOwnership {
    private Object actor;
    private boolean cleanup;
    private Object owner;
    private long generation = -1;
    private Object pending;
    private Object pendingOwner;
    private long pendingGeneration;
    private long issuedAt;
    private String reason = "none";
    private boolean hold;

    public <R> R runAs(Object actor, boolean cleanup, Supplier<R> action) {
        Object previousActor = this.actor;
        boolean previousCleanup = this.cleanup;
        this.actor = actor;
        this.cleanup = cleanup;
        try {
            return action.get();
        } finally {
            this.actor = previousActor;
            this.cleanup = previousCleanup;
        }
    }

    public boolean canMutate(long currentGeneration) {
        return !cleanup || (owner == actor && generation == currentGeneration
                && (pending == null || pendingOwner == actor));
    }

    public void commanded(long generation, long tick, String reason, boolean hold) {
        owner = actor;
        this.generation = generation;
        issuedAt = tick;
        this.reason = reason;
        this.hold = hold;
    }

    public boolean submit(Object intent, long currentGeneration) {
        if (!canMutate(currentGeneration)) return false;
        pending = intent;
        pendingOwner = actor;
        pendingGeneration = currentGeneration;
        return true;
    }

    public boolean canErasePending(Object intent) {
        return !cleanup || pending == intent && pendingOwner == actor;
    }

    public boolean apply(Object intent, long currentGeneration, Runnable action) {
        if (pending == intent && pendingGeneration != currentGeneration) {
            discardPending();
            return false;
        }
        Object source = pending == intent ? pendingOwner : null;
        pending = null;
        pendingOwner = null;
        runAs(source, false, () -> { action.run(); return null; });
        return true;
    }

    public void discardPending() {
        pending = null;
        pendingOwner = null;
    }

    public boolean hasRecentHold(long currentGeneration, long tick) {
        return hold && generation == currentGeneration && tick - issuedAt <= 10;
    }

    public String summary(long currentGeneration, long tick) {
        return "owner=" + (owner == null ? "external" : owner.getClass().getSimpleName())
                + ",current=" + (generation == currentGeneration) + ",reason=" + reason
                + ",age=" + Math.max(0, tick - issuedAt) + "t";
    }
}
