package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.common.registry.ModSensorTypes;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ApplyMovementIntentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonHuntAndEatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonIdleLookBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.FirstApplicableDragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.LookAtAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.MoveToGroundWalkTargetBehaviour;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Common Brain wiring*/
public abstract class DragonBrainProfile<T extends RideableDragonBase> implements DragonBrainOwner<T> {
    @Override
    public final List<DragonBehaviourGroup<T>> getDragonBrainBehaviourGroups() {
        List<DragonBehaviourGroup<T>> groups = new ArrayList<>(createBehaviourGroups());
        addFeatureGroups(groups);
        return List.copyOf(groups);
    }

    protected abstract List<DragonBehaviourGroup<T>> createBehaviourGroups();

    protected void addFeatureGroups(List<DragonBehaviourGroup<T>> groups) {
    }

    @Override
    public List<SensorType<? extends Sensor<? super T>>> getDragonBrainSensors() {
        return usesDragonScent()
                ? List.of(ModSensorTypes.DRAGON_MOVEMENT_STATE.get(), ModSensorTypes.DRAGON_SCENT.get())
                : List.of(ModSensorTypes.DRAGON_MOVEMENT_STATE.get());
    }

    @Override
    public boolean usesDragonScent() {
        return false;
    }

    @Override
    public final void updateActivity(Brain<T> brain, T dragon) {
        if (selectPriorityActivity(brain, dragon)) {
            return;
        }
        LivingEntity target = brain.getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        if (canFight(dragon, target)) {
            brain.setActiveActivityIfPossible(getCombatActivity(brain));
        } else {
            onCombatUnavailable(brain, dragon, target);
            brain.useDefaultActivity();
        }
    }
    protected boolean selectPriorityActivity(Brain<T> brain, T dragon) {
        return false;
    }

    protected boolean canFight(T dragon, @Nullable LivingEntity target) {
        return DragonTargetLifecycle.isValidTarget(dragon, target)
                && !dragon.isVehicle() && !dragon.isOrderedToSit();
    }

    protected void onCombatUnavailable(Brain<T> brain, T dragon, @Nullable LivingEntity target) {
    }

    @SafeVarargs
    protected final DragonBehaviourGroup<T> coreGroup(float lookTurnSpeed, DragonBehaviour<T>... leadingBehaviours) {
        return coreGroup(lookTurnSpeed, false, leadingBehaviours);
    }

    @SafeVarargs
    protected final DragonBehaviourGroup<T> huntingCoreGroup(float lookTurnSpeed, DragonBehaviour<T>... leadingBehaviours) {
        return coreGroup(lookTurnSpeed, true, leadingBehaviours);
    }

    @SafeVarargs
    private final DragonBehaviourGroup<T> coreGroup(float lookTurnSpeed, boolean hunts,
                                                   DragonBehaviour<T>... leadingBehaviours) {
        var group = DragonBehaviourGroup.<T>activity(Activity.CORE)
                .behaviours(leadingBehaviours)
                .behaviours(new DragonIdleLookBehaviour<>(8.0D));
        if (hunts) {
            group.behaviours(new DragonHuntAndEatBehaviour<>());
        }
        return group.behaviours(new ApplyMovementIntentBehaviour<>(), new MoveToGroundWalkTargetBehaviour<>(),
                        new LookAtAttackTargetBehaviour<>(lookTurnSpeed, lookTurnSpeed))
                .build();
    }

    @SafeVarargs
    protected final DragonBehaviourGroup<T> fightGroup(DragonBehaviour<T>... behaviours) {
        return DragonBehaviourGroup.<T>activity(Activity.FIGHT).behaviours(behaviours)
                .clearMovementWhenStopped()
                .build();
    }

    @SafeVarargs
    protected final DragonBehaviourGroup<T> idleGroup(DragonBehaviour<T>... behaviours) {
        return DragonBehaviourGroup.<T>activity(Activity.IDLE)
                .behaviours(new FirstApplicableDragonBehaviour<>(behaviours))
                .clearWhenStopped(DragonMemories.MOVEMENT_INTENT)
                .build();
    }
}
