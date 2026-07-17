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
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus.IgnivorusAirCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus.IgnivorusAutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus.IgnivorusGroundCombatBehaviour;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public class IgnivorusCombatBrain implements DragonBrainOwner<Ignivorus> {
    @Override
    public List<SensorType<? extends Sensor<? super Ignivorus>>> getDragonBrainSensors() {
        return List.of(
                ModSensorTypes.DRAGON_TARGET.get(),
                ModSensorTypes.DRAGON_MOVEMENT_STATE.get()
        );
    }

    @Override
    public void updateActivity(Brain<Ignivorus> brain, Ignivorus dragon) {
        if (dragon.shouldUseCombatBrain()) {
            brain.setActiveActivityIfPossible(Activity.FIGHT);
        } else {
            brain.useDefaultActivity();
        }
    }

    @Override
    public boolean shouldTakeControl(Ignivorus dragon) {
        if (dragon.shouldUseCombatBrain()) {
            return true;
        }
        if (dragon.getTarget() != null && dragon.getTarget().isAlive()) {
            return false;
        }
        return DragonBrainOwner.super.shouldTakeControl(dragon);
    }

    @Override
    public List<DragonBehaviourGroup<Ignivorus>> getDragonBrainBehaviourGroups() {
        IgnivorusGroundCombatBehaviour groundCombat = new IgnivorusGroundCombatBehaviour();
        return List.of(
                DragonBehaviourGroup.<Ignivorus>activity(Activity.CORE)
                        .behaviours(
                                new ApplyMovementIntentBehaviour<>(),
                                new MoveToGroundWalkTargetBehaviour<>(),
                                new LookAtAttackTargetBehaviour<>(30.0F, 30.0F)
                        )
                        .build(),
                DragonBehaviourGroup.<Ignivorus>activity(Activity.FIGHT)
                        .behaviours(
                                new GroundPursuitFlightTransitionBehaviour<>(
                                        GroundPursuitFlightSettings.standard()
                                ),
                                new LandForGroundTargetBehaviour<>(Ignivorus.AI_AIR_COMBAT_SETTINGS.landingSpeed()),
                                new IgnivorusAirCombatBehaviour(),
                                new SetWalkTargetToAttackTargetBehaviour<Ignivorus>(
                                        IgnivorusGroundCombatBehaviour.CHASE_SPEED,
                                        (dragon, target) ->
                                                IgnivorusGroundCombatBehaviour.MELEE_ENGAGE_RANGE
                                                        + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D,
                                        (dragon, target) -> groundCombat.isGroundMovementLocked()
                                ),
                                new AsyncWaterChaseTargetBehaviour<>(0.12D, 8.0F),
                                groundCombat
                        )
                        .clearWhenStopped(
                                DragonMemories.MOVEMENT_INTENT,
                                DragonMemories.WALK_TARGET,
                                DragonMemories.PATH,
                                DragonMemories.CANT_REACH_WALK_TARGET_SINCE
                        )
                        .build(),
                DragonBehaviourGroup.<Ignivorus>activity(Activity.IDLE)
                        .behaviours(new IgnivorusAutonomousFlightBehaviour())
                        .clearWhenStopped(DragonMemories.MOVEMENT_INTENT)
                        .build()
        );
    }
}
