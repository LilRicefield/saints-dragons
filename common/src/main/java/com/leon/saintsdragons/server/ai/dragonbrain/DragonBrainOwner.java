package com.leon.saintsdragons.server.ai.dragonbrain;

import com.google.common.collect.ImmutableList;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.*;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.mojang.datafixers.util.Pair;
import com.leon.saintsdragons.common.registry.ModSensorTypes;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.ai.dragonbrain.debug.DragonBrainDiagnostics;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonPerception;
import com.leon.saintsdragons.server.ai.dragonbrain.learning.DragonCombatLearner;
import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonCombatFlightState;
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
    default boolean usesCombatDecisionSupport() { return false; }

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
            List<DragonBehaviour<T>> configuredBehaviours = new ArrayList<>();
            if (group.activity() == Activity.IDLE) {
                configuredBehaviours.addAll(idlePerceptionBehaviours(usesDragonScent()));
            }
            if (group.activity() == Activity.FIGHT) {
                configuredBehaviours.add(new AirToGroundTransitionBehaviour<>());
            }
            configuredBehaviours.addAll(group.behaviours());
            if (group.activity() == Activity.FIGHT) {
                configuredBehaviours.add(new DragonFlightMovementRecoveryBehaviour<>());
            }
            if (group.activity() == Activity.CORE) {
                configuredBehaviours.add(new DragonPerceptionBehaviour<>());
                configuredBehaviours.add(new DragonSleepBehaviour<>());
                configuredBehaviours.add(new DragonTacticalPlannerBehaviour<>());
            }
            for (DragonBehaviour<T> behaviour : configuredBehaviours) {
                behaviour.bindActivity(group.activity(), priority);
                behaviours.add(Pair.of(priority++, behaviour));
                registeredBehaviours.add(new DragonBrainDiagnostics.RegisteredBehaviour(
                        group.activity(), priority - 1, behaviour));
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

    default boolean usesDragonScent() {
        return getDragonBrainSensors().contains(ModSensorTypes.DRAGON_SCENT.get());
    }

    static <D extends DragonEntity> List<DragonBehaviour<D>> idlePerceptionBehaviours(boolean usesScent) {
        // Sight and hearing also create investigation targets, without a scent sensor.
        return usesScent
                ? List.of(new DragonScentAssessmentBehaviour<>(), new DragonInvestigateTargetBehaviour<>())
                : List.of(new DragonInvestigateTargetBehaviour<>());
    }

    default void tickBrain(ServerLevel level, T dragon) {
        level.getProfiler().push("dragonBrain");
        try {
            @SuppressWarnings("unchecked")
            Brain<T> brain = (Brain<T>)(Brain<?>)dragon.getBrain();
            dragon.refreshMountedCombatTarget();
            var perceivedTarget = DragonPerception.refreshTargetVisibility(brain, dragon, level.getGameTime());
            if (usesCombatDecisionSupport() && dragon instanceof RideableFlyingDragon flying) {
                flying.enableCombatDecisionSupport().observe(perceivedTarget,
                        brain.getMemory(DragonMemories.TARGET_VISIBLE).orElse(false));
            }
            var learning = DragonCombatLearner.get(dragon);
            if (learning != null) {
                learning.observe(perceivedTarget, brain.getMemory(DragonMemories.TARGET_VISIBLE).orElse(false));
            }
            DragonCombatFlightState combatFlight = DragonCombatFlightState.get(dragon);
            if (combatFlight != null) combatFlight.observe();
            updateActivity(brain, dragon);
            brain.tick(level, dragon);
            if (combatFlight != null) combatFlight.applyPlan();
            if (dragon instanceof RideableDragonBase rideable) {
                ApplyMovementIntentBehaviour.applyPending(rideable);
            }
        } finally {
            level.getProfiler().pop();
        }
    }

    default void updateActivity(Brain<T> brain, T dragon) {
        if (getCombatActivity(brain) == Activity.IDLE) {
            brain.setActiveActivityIfPossible(Activity.IDLE);
        } else {
            brain.setActiveActivityToFirstValid(getDragonBrainActivityPriority());
        }
    }

    default Activity getCombatActivity(Brain<T> brain) {
        return brain.hasMemoryValue(DragonMemories.ATTACK_TARGET)
                && !brain.hasMemoryValue(DragonMemories.RESCUE_TARGET)
                && !brain.getMemory(DragonMemories.TARGET_VISIBLE).orElse(true)
                && !(brain.getActiveActivities().contains(Activity.FIGHT) && DragonPerception.isSightInterruption(brain))
                && !brain.hasMemoryValue(DragonMemories.INTERCEPT_PROJECTILE)
                ? Activity.IDLE : Activity.FIGHT;
    }

}
