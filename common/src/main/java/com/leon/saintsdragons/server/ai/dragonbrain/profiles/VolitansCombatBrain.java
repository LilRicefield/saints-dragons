package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.registry.ModSensorTypes;
import com.leon.saintsdragons.server.ai.GroundPursuitFlightSettings;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainOwner;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ApplyMovementIntentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AsyncWaterChaseTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.GroundPursuitFlightTransitionBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.LandForGroundTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.LookAtAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.MoveToGroundWalkTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.SetWalkTargetToAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans.VolitansAirCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans.VolitansAutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans.VolitansGroundCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans.VolitansWaterCombatBehaviour;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public class VolitansCombatBrain implements DragonBrainOwner<Volitans> {
    @Override
    public List<SensorType<? extends Sensor<? super Volitans>>> getDragonBrainSensors() {
        return List.of(
                ModSensorTypes.DRAGON_TARGET.get(),
                ModSensorTypes.DRAGON_MOVEMENT_STATE.get()
        );
    }

    @Override
    public void updateActivity(Brain<Volitans> brain, Volitans dragon) {
        if (dragon.shouldUseCombatBrain()) {
            brain.setActiveActivityIfPossible(Activity.FIGHT);
        } else {
            brain.useDefaultActivity();
        }
    }

    @Override
    public boolean shouldTakeControl(Volitans dragon) {
        if (dragon.shouldUseCombatBrain()) {
            return true;
        }
        if (dragon.getTarget() != null && dragon.getTarget().isAlive()) {
            return false;
        }
        return DragonBrainOwner.super.shouldTakeControl(dragon);
    }

    @Override
    public List<DragonBehaviourGroup<Volitans>> getDragonBrainBehaviourGroups() {
        VolitansGroundCombatBehaviour groundCombat = new VolitansGroundCombatBehaviour();
        return List.of(
                DragonBehaviourGroup.<Volitans>activity(Activity.CORE)
                        .behaviours(
                                new ApplyMovementIntentBehaviour<>(),
                                new MoveToGroundWalkTargetBehaviour<>(),
                                new LookAtAttackTargetBehaviour<>(35.0F, 35.0F)
                        )
                        .build(),
                DragonBehaviourGroup.<Volitans>activity(Activity.FIGHT)
                        .behaviours(
                                new GroundPursuitFlightTransitionBehaviour<>(
                                        GroundPursuitFlightSettings.standard()
                                ),
                                new LandForGroundTargetBehaviour<>(Volitans.AI_AIR_COMBAT_SETTINGS.landingSpeed()),
                                new VolitansAirCombatBehaviour(),
                                new SetWalkTargetToAttackTargetBehaviour<Volitans>(
                                        VolitansGroundCombatBehaviour.CHASE_SPEED,
                                        (dragon, target) ->
                                                VolitansGroundCombatBehaviour.BITE_RANGE
                                                        + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D,
                                        (dragon, target) -> groundCombat.isGroundMovementLocked()
                                ),
                                new AsyncWaterChaseTargetBehaviour<>(0.28D, 8.0F),
                                groundCombat,
                                new VolitansWaterCombatBehaviour()
                        )
                        .clearWhenStopped(
                                DragonMemories.MOVEMENT_INTENT,
                                DragonMemories.WALK_TARGET,
                                DragonMemories.PATH,
                                DragonMemories.CANT_REACH_WALK_TARGET_SINCE
                        )
                        .build(),
                DragonBehaviourGroup.<Volitans>activity(Activity.IDLE)
                        .behaviours(new VolitansAutonomousFlightBehaviour())
                        .clearWhenStopped(DragonMemories.MOVEMENT_INTENT)
                        .build()
        );
    }
}
