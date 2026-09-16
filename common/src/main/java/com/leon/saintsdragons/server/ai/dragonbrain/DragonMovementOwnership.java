package com.leon.saintsdragons.server.ai.dragonbrain;

import net.minecraft.world.entity.schedule.Activity;

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
    private Supplier<?> actionSupplier;
    private Object action;
    private Object actionOwner;
    private Object pendingAction;

    public void setActionSupplier(Supplier<?> actionSupplier) {
        this.actionSupplier = actionSupplier;
    }

    public void coordinateAction(Object action) {
        if (this.action != action) {
            this.action = action;
            actionOwner = null;
        }
    }

    private void refreshAction() {
        if (actionSupplier != null) coordinateAction(actionSupplier.get());
    }

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
        refreshAction();
        if (!cleanup && action != null && actor instanceof DragonBehaviour<?> behaviour) {
            if (actionOwner != null && actionOwner != actor && behaviour.activity() != Activity.PANIC) return false;
        }
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
        pendingAction = action;
        if (action != null && actor instanceof DragonBehaviour<?> behaviour && behaviour.claimsControl()
                && (behaviour.activity() == Activity.FIGHT || behaviour.activity() == Activity.PANIC)) {
            actionOwner = actor;
        }
        return true;
    }

    public boolean canErasePending(Object intent) {
        refreshAction();
        if (!cleanup && action != null && actionOwner != null && actionOwner != actor
                && actor instanceof DragonBehaviour<?> behaviour && behaviour.activity() != Activity.PANIC) return false;
        return !cleanup || pending == intent && pendingOwner == actor;
    }

    public boolean apply(Object intent, long currentGeneration, Runnable action) {
        refreshAction();
        if (pending == intent && (pendingGeneration != currentGeneration || pendingAction != this.action)) {
            discardPending();
            return false;
        }
        Object source = pending == intent ? pendingOwner : null;
        pending = null;
        pendingOwner = null;
        try {
            runAs(source, false, () -> { action.run(); return null; });
        } finally {
            // Reserve each command through its application, not locomotion for the entire ability.
            // Ground walk-target execution must remain available to attacks that allow advancing.
            discardPending();
        }
        return true;
    }

    public void discardPending() {
        pending = null;
        pendingOwner = null;
        pendingAction = null;
        actionOwner = null;
    }

    public boolean hasRecentHold(long currentGeneration, long tick) {
        return hold && generation == currentGeneration && tick - issuedAt <= 10;
    }

    public String summary(long currentGeneration, long tick) {
        return "owner=" + (owner == null ? "external" : owner.getClass().getSimpleName())
                + ",action_owner=" + (actionOwner == null ? "none" : actionOwner.getClass().getSimpleName())
                + ",current=" + (generation == currentGeneration) + ",reason=" + reason
                + ",age=" + Math.max(0, tick - issuedAt) + "t";
    }
}
