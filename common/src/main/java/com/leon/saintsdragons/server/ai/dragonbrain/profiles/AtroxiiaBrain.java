package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.registry.ModSensorTypes;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainOwner;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ApplyMovementIntentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundFollowOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonIdleLookBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonWaterEscapeBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.FirstApplicableDragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.MoveToGroundWalkTargetBehaviour;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public final class AtroxiiaBrain implements DragonBrainOwner<Atroxiia> {
    @Override
    public List<SensorType<? extends Sensor<? super Atroxiia>>> getDragonBrainSensors() {
        return List.of(ModSensorTypes.DRAGON_MOVEMENT_STATE.get());
    }

    @Override
    public void updateActivity(Brain<Atroxiia> brain, Atroxiia dragon) {
        brain.useDefaultActivity();
    }

    @Override
    public List<DragonBehaviourGroup<Atroxiia>> getDragonBrainBehaviourGroups() {
        return List.of(
                DragonBehaviourGroup.<Atroxiia>activity(Activity.CORE)
                        .behaviours(
                                new DragonIdleLookBehaviour<>(8.0D),
                                new ApplyMovementIntentBehaviour<>(),
                                new MoveToGroundWalkTargetBehaviour<>()
                        )
                        .build(),
                DragonBehaviourGroup.<Atroxiia>activity(Activity.IDLE)
                        .behaviours(
                                new FirstApplicableDragonBehaviour<>(
                                        new DragonWaterEscapeBehaviour<>(8.0F, 0.12D),
                                        new DragonGroundFollowOwnerBehaviour<>(
                                                DragonGroundFollowOwnerBehaviour.Config.atroxiia()),
                                        new DragonGroundWanderBehaviour<>(0.55D, 100)
                                )
                        )
                        .clearWhenStopped(DragonMemories.MOVEMENT_INTENT)
                        .build()
        );
    }
}
