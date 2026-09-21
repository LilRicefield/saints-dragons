package com.leon.saintsdragons.server.ai.dragonbrain;

import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public final class DragonBehaviourResources {
    private final Map<MemoryModuleType<?>, MemoryClaim> memories = new HashMap<>();
    private final Map<TaskKey, TaskToken> tasks = new HashMap<>();
    private final Map<TaskKey, Runnable> cleanups = new HashMap<>();
    private Object actor;
    private boolean cleanup;

    public <R> R runAs(Object owner, boolean cleanup, Supplier<R> action) {
        Object previousActor = actor;
        boolean previousCleanup = this.cleanup;
        actor = owner;
        this.cleanup = cleanup;
        try {
            return action.get();
        } finally {
            actor = previousActor;
            this.cleanup = previousCleanup;
        }
    }

    public void written(MemoryModuleType<?> key, Object value, boolean owned) {
        if (owned && actor == null) throw new IllegalStateException("Owned memory requires a behaviour scope");
        if (actor != null && (owned || key == DragonMemories.LOOK_TARGET)) {
            memories.put(key, new MemoryClaim(actor, value));
        } else {
            memories.remove(key);
        }
    }

    public boolean canErase(MemoryModuleType<?> key, Object current, boolean ownedOnly) {
        MemoryClaim claim = memories.get(key);
        if (!ownedOnly && (!cleanup || claim == null && key != DragonMemories.LOOK_TARGET)) return true;
        return claim != null && claim.owner == actor && claim.value == current;
    }

    public void erased(MemoryModuleType<?> key) {
        memories.remove(key);
    }

    public TaskToken beginTask(Object owner, String slot) {
        TaskKey key = new TaskKey(Objects.requireNonNull(owner), Objects.requireNonNull(slot));
        TaskToken previous = tasks.remove(key);
        if (previous != null) previous.cancel();
        TaskToken token = new TaskToken(key);
        tasks.put(key, token);
        return token;
    }

    public void onStop(Object owner, String slot, Runnable action) {
        TaskKey key = new TaskKey(Objects.requireNonNull(owner), Objects.requireNonNull(slot));
        Runnable previous = cleanups.remove(key);
        if (previous != null) previous.run();
        cleanups.put(key, Objects.requireNonNull(action));
    }

    public boolean complete(TaskToken token, Runnable action) {
        if (!token.isCurrent() || tasks.get(token.key) != token) return false;
        tasks.remove(token.key);
        token.active = false;
        token.future = null;
        runAs(token.key.owner, false, () -> { action.run(); return null; });
        return true;
    }

    public void release(Object owner, DragonMemoryMap memoryMap) {
        runAs(owner, true, () -> { releaseOwned(owner, memoryMap); return null; });
    }

    private void releaseOwned(Object owner, DragonMemoryMap memoryMap) {
        for (TaskToken token : new ArrayList<>(tasks.values())) {
            if (token.key.owner == owner) token.cancel();
        }
        for (var entry : new ArrayList<>(memories.entrySet())) {
            if (entry.getValue().owner == owner) {
                memoryMap.eraseOwned(entry.getKey());
                memories.remove(entry.getKey(), entry.getValue());
            }
        }
        for (var entry : new ArrayList<>(cleanups.entrySet())) {
            if (entry.getKey().owner == owner && cleanups.remove(entry.getKey(), entry.getValue())) {
                entry.getValue().run();
            }
        }
    }

    public void clear() {
        for (TaskToken token : new ArrayList<>(tasks.values())) token.cancel();
        memories.clear();
        var pendingCleanups = new ArrayList<>(cleanups.values());
        cleanups.clear();
        pendingCleanups.forEach(Runnable::run);
    }

    public final class TaskToken {
        private final TaskKey key;
        private boolean active = true;
        private Future<?> future;

        private TaskToken(TaskKey key) {
            this.key = key;
        }

        public boolean isCurrent() {
            return active && tasks.get(key) == this;
        }

        public void bind(Future<?> future) {
            if (isCurrent()) this.future = future;
            else if (future != null) future.cancel(true);
        }

        public void cancel() {
            active = false;
            tasks.remove(key, this);
            Future<?> pending = future;
            future = null;
            if (pending != null) pending.cancel(true);
        }
    }

    private record TaskKey(Object owner, String slot) {
        @Override
        public boolean equals(Object other) {
            return other instanceof TaskKey key && owner == key.owner && slot.equals(key.slot);
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(owner) + slot.hashCode();
        }
    }

    private record MemoryClaim(Object owner, Object value) {
    }
}
