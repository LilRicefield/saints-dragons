package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.registry.ModAbilities;
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
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane.CindervaneAutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane.CindervaneAirCombatMovementBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane.CindervaneMeleeAttackBehaviour;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public class CindervaneCombatBrain implements DragonBrainOwner<Cindervane> {
    private static final double GROUND_CHASE_SPEED = 1.0D;
    private static final double WATER_CHASE_SPEED = 0.12D;
    private static final float WATER_TURN_SPEED = 8.0F;

    @Override
    public List<SensorType<? extends Sensor<? super Cindervane>>> getDragonBrainSensors() {
        return List.of(
                ModSensorTypes.DRAGON_TARGET.get(),
                ModSensorTypes.DRAGON_MOVEMENT_STATE.get()
        );
    }

    @Override
    public void updateActivity(Brain<Cindervane> brain, Cindervane dragon) {
        if (dragon.shouldUseCombatBrain()) {
            brain.setActiveActivityIfPossible(Activity.FIGHT);
        } else {
            brain.useDefaultActivity();
        }
    }

    @Override
    public boolean shouldTakeControl(Cindervane dragon) {
        if (dragon.shouldUseCombatBrain()) {
            return true;
        }
        if (dragon.getTarget() != null && dragon.getTarget().isAlive()) {
            return false;
        }
        return DragonBrainOwner.super.shouldTakeControl(dragon);
    }

    @Override
    public List<DragonBehaviourGroup<Cindervane>> getDragonBrainBehaviourGroups() {
        return List.of(
                DragonBehaviourGroup.<Cindervane>activity(Activity.CORE)
                        .behaviours(
                                new ApplyMovementIntentBehaviour<>(),
                                new MoveToGroundWalkTargetBehaviour<>(),
                                new LookAtAttackTargetBehaviour<>(30.0F, 30.0F)
                        )
                        .build(),
                DragonBehaviourGroup.<Cindervane>activity(Activity.FIGHT)
                        .behaviours(
                                new GroundPursuitFlightTransitionBehaviour<>(
                                        GroundPursuitFlightSettings.standard(),
                                        dragon -> dragon.isAbilityActive(ModAbilities.CINDERVANE_BITE)
                                ),
                                new LandForGroundTargetBehaviour<>(Cindervane.AI_AIR_COMBAT_SETTINGS.landingSpeed()),
                                new CindervaneAirCombatMovementBehaviour(),
                                new SetWalkTargetToAttackTargetBehaviour<Cindervane>(
                                        (float)GROUND_CHASE_SPEED,
                                        (dragon, target) ->
                                                CindervaneMeleeAttackBehaviour.groundStopRange(target)
                                                        + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D,
                                        (dragon, target) -> dragon.isAbilityActive(ModAbilities.CINDERVANE_BITE)
                                ),
                                new AsyncWaterChaseTargetBehaviour<>(WATER_CHASE_SPEED, WATER_TURN_SPEED),
                                new CindervaneMeleeAttackBehaviour()
                        )
                        .clearWhenStopped(
                                DragonMemories.MOVEMENT_INTENT,
                                DragonMemories.WALK_TARGET,
                                DragonMemories.PATH,
                                DragonMemories.CANT_REACH_WALK_TARGET_SINCE
                        )
                        .build(),
                DragonBehaviourGroup.<Cindervane>activity(Activity.IDLE)
                        .behaviours(new CindervaneAutonomousFlightBehaviour())
                        .clearWhenStopped(DragonMemories.MOVEMENT_INTENT)
                        .build()
        );
    }
}
