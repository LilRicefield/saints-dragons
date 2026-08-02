package com.leon.saintsdragons.server.ai.dragonbrain;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.Optional;

public final class DragonMemoryMap {
    private final Brain<?> brain;

    DragonMemoryMap(Brain<?> brain) {
        this.brain = brain;
    }

    public <T> void set(MemoryModuleType<T> key, T value) {
        brain.setMemory(key, value);
    }

    public <T> void set(MemoryModuleType<T> key, T value, int ttlTicks) {
        if (ttlTicks > 0) {
            brain.setMemoryWithExpiry(key, value, ttlTicks);
        } else {
            set(key, value);
        }
    }

    public <T> Optional<T> get(MemoryModuleType<T> key) {
        return brain.getMemory(key);
    }

    /**
     * Returns the current value and erases it before the caller acts on it.
     */
    public <T> Optional<T> take(MemoryModuleType<T> key) {
        Optional<T> value = get(key);
        erase(key);
        return value;
    }

    public boolean has(MemoryModuleType<?> key) {
        return brain.hasMemoryValue(key);
    }

    public void erase(MemoryModuleType<?> key) {
        brain.eraseMemory(key);
    }

    public void eraseAll(Iterable<MemoryModuleType<?>> keys) {
        for (MemoryModuleType<?> key : keys) {
            erase(key);
        }
    }

    public void clear() {
        brain.clearMemories();
    }
}
