package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.ai.navigation.DragonAIMovementController;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Optional;

public final class DragonMemoryMap {
    private final Brain<?> brain;
    private final DragonAIMovementController movement;
    private final DragonBehaviourResources resources;

    DragonMemoryMap(DragonEntity dragon) {
        this.brain = dragon.getBrain();
        this.resources = dragon.getBrainUtilities().resources();
        this.movement = dragon instanceof RideableDragonBase rideable
                ? rideable.getAIMovement() : null;
    }

    public <T> void set(MemoryModuleType<T> key, T value) {
        if (key == DragonMemories.MOVEMENT_INTENT && movement != null
                && !movement.brainMovement().submit(value, movement.getMovementCommandGeneration())) return;
        brain.setMemory(key, value);
        resources.written(key, value, false);
    }

    public <T> void set(MemoryModuleType<T> key, T value, int ttlTicks) {
        if (ttlTicks > 0) {
            if (key == DragonMemories.MOVEMENT_INTENT && movement != null
                    && !movement.brainMovement().submit(value, movement.getMovementCommandGeneration())) return;
            brain.setMemoryWithExpiry(key, value, ttlTicks);
            resources.written(key, value, false);
        } else {
            set(key, value);
        }
    }

    public <T> Optional<T> get(MemoryModuleType<T> key) {
        return brain.getMemory(key);
    }

    public <T> void setOwned(MemoryModuleType<T> key, T value) {
        if (!brain.checkMemory(key, MemoryStatus.REGISTERED)) {
            throw new IllegalArgumentException("Owned memory must be registered in the brain");
        }
        if (key == DragonMemories.MOVEMENT_INTENT) {
            throw new IllegalArgumentException("Movement intents already have movement ownership");
        }
        resources.written(key, value, true);
        brain.setMemory(key, value);
    }

    public void eraseOwned(MemoryModuleType<?> key) {
        if (!brain.checkMemory(key, MemoryStatus.REGISTERED)) {
            resources.erased(key);
            return;
        }
        if (resources.canErase(key, brain.getMemory(key).orElse(null), true)) erase(key);
    }

    /**
     * Returns the current value and erases it before the caller acts on it.
     */
    public <T> Optional<T> take(MemoryModuleType<T> key) {
        Optional<T> value = get(key);
        return tryErase(key) ? value : Optional.empty();
    }

    public boolean has(MemoryModuleType<?> key) {
        return brain.hasMemoryValue(key);
    }

    public void erase(MemoryModuleType<?> key) {
        tryErase(key);
    }

    private boolean tryErase(MemoryModuleType<?> key) {
        if (!brain.checkMemory(key, MemoryStatus.REGISTERED)) {
            resources.erased(key);
            return false;
        }
        if (!resources.canErase(key, brain.getMemory(key).orElse(null), false)) return false;
        if (key == DragonMemories.MOVEMENT_INTENT && movement != null
                && !movement.brainMovement().canErasePending(brain.getMemory(DragonMemories.MOVEMENT_INTENT).orElse(null))) return false;
        brain.eraseMemory(key);
        resources.erased(key);
        if (key == DragonMemories.MOVEMENT_INTENT && movement != null) movement.brainMovement().discardPending();
        return true;
    }

    public void eraseAll(Iterable<MemoryModuleType<?>> keys) {
        for (MemoryModuleType<?> key : keys) {
            erase(key);
        }
    }

    public void clear() {
        brain.clearMemories();
        resources.clear();
    }
}
