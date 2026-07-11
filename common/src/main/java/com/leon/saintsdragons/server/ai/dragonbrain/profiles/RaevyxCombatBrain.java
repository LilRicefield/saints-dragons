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
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx.RaevyxAirCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx.RaevyxAutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx.RaevyxGroundCombatBehaviour;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public class RaevyxCombatBrain implements DragonBrainOwner<Raevyx> {
    @Override
    public List<SensorType<? extends Sensor<? super Raevyx>>> getDragonBrainSensors() {
        return List.of(
                ModSensorTypes.DRAGON_TARGET.get(),
                ModSensorTypes.DRAGON_MOVEMENT_STATE.get()
        );
    }

    @Override
    public void updateActivity(Brain<Raevyx> brain, Raevyx dragon) {
        if (dragon.shouldUseCombatBrain()) {
            brain.setActiveActivityIfPossible(Activity.FIGHT);
        } else {
            brain.useDefaultActivity();
        }
    }

    @Override
    public boolean shouldTakeControl(Raevyx dragon) {
        if (dragon.shouldUseCombatBrain()) {
            return true;
        }
        if (dragon.getTarget() != null && dragon.getTarget().isAlive()) {
            return false;
        }
        return DragonBrainOwner.super.shouldTakeControl(dragon);
    }

    @Override
    public List<DragonBehaviourGroup<Raevyx>> getDragonBrainBehaviourGroups() {
        return List.of(
                DragonBehaviourGroup.<Raevyx>activity(Activity.CORE)
                        .behaviours(
                                new ApplyMovementIntentBehaviour<>(),
                                new MoveToGroundWalkTargetBehaviour<>(),
                                new LookAtAttackTargetBehaviour<>(30.0F, 30.0F)
                        )
                        .build(),
                DragonBehaviourGroup.<Raevyx>activity(Activity.FIGHT)
                        .behaviours(
                                new GroundPursuitFlightTransitionBehaviour<>(
                                        GroundPursuitFlightSettings.standard()
                                ),
                                new LandForGroundTargetBehaviour<>(Raevyx.AI_AIR_COMBAT_SETTINGS.landingSpeed()),
                                new RaevyxAirCombatBehaviour(),
                                new SetWalkTargetToAttackTargetBehaviour<Raevyx>(
                                        0.8F,
                                        (dragon, target) -> (int)Math.floor(
                                                RaevyxGroundCombatBehaviour.meleeStopRange(target)
                                                        + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D
                                        ),
                                        (dragon, target) -> dragon.getActiveAbility() != null
                                                || dragon.isDashing()
                                                || dragon.isDodging()
                                                || dragon.isGroundRending()
                                ),
                                new RaevyxGroundCombatBehaviour(),
                                new AsyncWaterChaseTargetBehaviour<>(0.12D, 8.0F)
                        )
                        .clearWhenStopped(
                                DragonMemories.MOVEMENT_INTENT,
                                DragonMemories.WALK_TARGET,
                                DragonMemories.PATH,
                                DragonMemories.CANT_REACH_WALK_TARGET_SINCE
                        )
                        .build(),
                DragonBehaviourGroup.<Raevyx>activity(Activity.IDLE)
                        .behaviours(new RaevyxAutonomousFlightBehaviour())
                        .clearWhenStopped(DragonMemories.MOVEMENT_INTENT)
                        .build()
        );
    }
}
