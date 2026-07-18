package com.leon.saintsdragons.server.ai.dragonbrain;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.ai.dragonbrain.debug.DragonBrainDiagnostics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface DragonBrainOwner<T extends DragonEntity> {
    default List<MemoryModuleType<?>> getDragonBrainMemories() {
        return DragonMemories.all();
    }

    default List<SensorType<? extends Sensor<? super T>>> getDragonBrainSensors() {
        return List.of();
    }

    default List<Activity> getDragonBrainActivityPriority() {
        return List.of(Activity.FIGHT, Activity.IDLE);
    }

    default List<DragonBehaviourGroup<T>> getDragonBrainBehaviourGroups() {
        return List.of();
    }

    default Activity getDragonBrainFallbackActivity() {
        return Activity.IDLE;
    }

    default Brain.Provider<T> brainProvider() {
        return Brain.provider(getDragonBrainMemories(), getDragonBrainSensors());
    }

    default Brain<T> makeBrain(Brain<T> brain) {
        List<DragonBrainDiagnostics.RegisteredBehaviour> registeredBehaviours = new ArrayList<>();
        for (DragonBehaviourGroup<T> group : getDragonBrainBehaviourGroups()) {
            ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super T>>> behaviours = ImmutableList.builder();
            int priority = group.activity() == Activity.CORE ? 0 : 10;
            for (DragonBehaviour<T> behaviour : group.behaviours()) {
                behaviour.bindActivity(group.activity(), priority);
                behaviours.add(Pair.of(priority++, behaviour));
                @SuppressWarnings("unchecked")
                BehaviorControl<? super net.minecraft.world.entity.LivingEntity> debugBehaviour =
                        (BehaviorControl<? super net.minecraft.world.entity.LivingEntity>)(BehaviorControl<?>)behaviour;
                registeredBehaviours.add(new DragonBrainDiagnostics.RegisteredBehaviour(
                        group.activity(), priority - 1, debugBehaviour));
            }

            Set<Pair<MemoryModuleType<?>, MemoryStatus>> requirements = new HashSet<>();
            group.requirements().forEach((memory, status) -> requirements.add(Pair.of(memory, status)));
            brain.addActivityAndRemoveMemoriesWhenStopped(
                    group.activity(),
                    behaviours.build(),
                    requirements,
                    new HashSet<>(group.clearWhenStopped())
            );
        }

        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(getDragonBrainFallbackActivity());
        brain.useDefaultActivity();
        DragonBrainDiagnostics.attach(brain, registeredBehaviours);
        return brain;
    }

    default void tickBrain(ServerLevel level, T dragon) {
        @SuppressWarnings("unchecked")
        Brain<T> brain = (Brain<T>)(Brain<?>)dragon.getBrain();
        updateActivity(brain, dragon);
        brain.tick(level, dragon);
    }

    default void updateActivity(Brain<T> brain, T dragon) {
        brain.setActiveActivityToFirstValid(getDragonBrainActivityPriority());
    }

    default boolean shouldTakeControl(T dragon) {
        return dragon.getBrain().getRunningBehaviors().stream()
                .anyMatch(behaviour -> behaviour instanceof DragonBehaviour<?> dragonBehaviour
                        && dragonBehaviour.claimsControl());
    }
}
