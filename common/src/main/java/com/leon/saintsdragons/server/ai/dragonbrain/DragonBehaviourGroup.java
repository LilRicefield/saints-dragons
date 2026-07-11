package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DragonBehaviourGroup<T extends DragonEntity> {
    private final Activity activity;
    private final List<DragonBehaviour<T>> behaviours;
    private final Map<MemoryModuleType<?>, MemoryStatus> requirements;
    private final List<MemoryModuleType<?>> clearWhenStopped;

    private DragonBehaviourGroup(Activity activity,
                                 List<DragonBehaviour<T>> behaviours,
                                 Map<MemoryModuleType<?>, MemoryStatus> requirements,
                                 List<MemoryModuleType<?>> clearWhenStopped) {
        this.activity = activity;
        this.behaviours = List.copyOf(behaviours);
        this.requirements = Map.copyOf(requirements);
        this.clearWhenStopped = List.copyOf(clearWhenStopped);
    }

    public static <T extends DragonEntity> Builder<T> activity(Activity activity) {
        return new Builder<>(activity);
    }

    public Activity activity() {
        return activity;
    }

    public List<DragonBehaviour<T>> behaviours() {
        return behaviours;
    }

    public Map<MemoryModuleType<?>, MemoryStatus> requirements() {
        return requirements;
    }

    public List<MemoryModuleType<?>> clearWhenStopped() {
        return clearWhenStopped;
    }

    public static final class Builder<T extends DragonEntity> {
        private final Activity activity;
        private final List<DragonBehaviour<T>> behaviours = new ArrayList<>();
        private final Map<MemoryModuleType<?>, MemoryStatus> requirements = new LinkedHashMap<>();
        private final List<MemoryModuleType<?>> clearWhenStopped = new ArrayList<>();

        private Builder(Activity activity) {
            this.activity = activity;
        }

        public Builder<T> behaviours(List<DragonBehaviour<T>> behaviours) {
            this.behaviours.addAll(behaviours);
            return this;
        }

        @SafeVarargs
        public final Builder<T> behaviours(DragonBehaviour<T>... behaviours) {
            Collections.addAll(this.behaviours, behaviours);
            return this;
        }

        public Builder<T> requires(MemoryModuleType<?> key, MemoryStatus status) {
            requirements.put(key, status);
            return this;
        }

        public Builder<T> clearWhenStopped(MemoryModuleType<?>... keys) {
            Collections.addAll(this.clearWhenStopped, keys);
            return this;
        }

        public DragonBehaviourGroup<T> build() {
            return new DragonBehaviourGroup<>(activity, behaviours, requirements, clearWhenStopped);
        }
    }
}
