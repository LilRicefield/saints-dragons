package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.ai.navigation.DragonAIMovementController;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.Optional;

public final class DragonMemoryMap {
    private final Brain<?> brain;
    private final DragonAIMovementController movement;

    DragonMemoryMap(DragonEntity dragon) {
        this.brain = dragon.getBrain();
        this.movement = dragon instanceof RideableDragonBase rideable
                ? rideable.getAIMovement() : null;
    }

    public <T> void set(MemoryModuleType<T> key, T value) {
        if (key == DragonMemories.MOVEMENT_INTENT && movement != null
                && !movement.brainMovement().submit(value, movement.getMovementCommandGeneration())) return;
        brain.setMemory(key, value);
    }

    public <T> void set(MemoryModuleType<T> key, T value, int ttlTicks) {
        if (ttlTicks > 0) {
            if (key == DragonMemories.MOVEMENT_INTENT && movement != null
                    && !movement.brainMovement().submit(value, movement.getMovementCommandGeneration())) return;
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
        if (key == DragonMemories.MOVEMENT_INTENT && movement != null
                && !movement.brainMovement().canErasePending(brain.getMemory(DragonMemories.MOVEMENT_INTENT).orElse(null))) return;
        brain.eraseMemory(key);
        if (key == DragonMemories.MOVEMENT_INTENT && movement != null) movement.brainMovement().discardPending();
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
