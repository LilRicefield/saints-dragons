package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.registry.ModSensorTypes;
import com.leon.saintsdragons.server.ai.GroundPursuitFlightSettings;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainOwner;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ApplyMovementIntentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonHuntAndEatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AsyncWaterChaseTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonBreedBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonDrinkBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFollowOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFollowParentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonIdleLookBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonRescueFallingOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonWaterEscapeBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.FirstApplicableDragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.GroundPursuitFlightTransitionBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.LookAtAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.MoveToGroundWalkTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.SetWalkTargetToAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx.RaevyxAirCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx.RaevyxAutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx.RaevyxGroundCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx.RaevyxTargetingBehaviour;
import com.leon.saintsdragons.server.ai.DragonAirCombatHelper;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public class RaevyxBrain implements DragonBrainOwner<Raevyx> {
    private static final DragonRescueFallingOwnerBehaviour.Config RESCUE_CONFIG =
            DragonRescueFallingOwnerBehaviour.Config.raevyx();

    @Override
    public List<SensorType<? extends Sensor<? super Raevyx>>> getDragonBrainSensors() {
        return List.of(ModSensorTypes.DRAGON_MOVEMENT_STATE.get(), ModSensorTypes.DRAGON_SCENT.get());
    }

    @Override
    public void updateActivity(Brain<Raevyx> brain, Raevyx dragon) {
        if (DragonRescueFallingOwnerBehaviour.updateRescueTarget(brain, dragon, RESCUE_CONFIG)) {
            brain.setActiveActivityIfPossible(Activity.PANIC);
            return;
        }
        LivingEntity target = brain.getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        if (canFight(dragon, target)) {
            brain.setActiveActivityIfPossible(Activity.FIGHT);
        } else {
            brain.useDefaultActivity();
        }
    }

    @Override
    public List<DragonBehaviourGroup<Raevyx>> getDragonBrainBehaviourGroups() {
        return List.of(
                DragonBehaviourGroup.<Raevyx>activity(Activity.CORE)
                        .behaviours(
                                new RaevyxTargetingBehaviour(),
                                new DragonIdleLookBehaviour<>(8.0D),
                                new DragonHuntAndEatBehaviour<>(),
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
                                new RaevyxAirCombatBehaviour(),
                                new SetWalkTargetToAttackTargetBehaviour<Raevyx>(
                                        0.8F,
                                        (dragon, target) ->
                                                RaevyxGroundCombatBehaviour.meleeStopRange(dragon, target)
                                                        + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D,
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
                DragonBehaviourGroup.<Raevyx>activity(Activity.PANIC)
                        .behaviours(new DragonRescueFallingOwnerBehaviour<>(RESCUE_CONFIG))
                        .clearWhenStopped(
                                DragonMemories.RESCUE_TARGET,
                                DragonMemories.MOVEMENT_INTENT,
                                DragonMemories.WALK_TARGET,
                                DragonMemories.PATH,
                                DragonMemories.CANT_REACH_WALK_TARGET_SINCE
                        )
                        .build(),
                DragonBehaviourGroup.<Raevyx>activity(Activity.IDLE)
                        .behaviours(
                                new FirstApplicableDragonBehaviour<>(
                                        new DragonWaterEscapeBehaviour<>(8.0F, 0.12D),
                                        new DragonFollowParentBehaviour<>(Raevyx.class, 1.15D),
                                        new DragonBreedBehaviour<>(
                                                1.0D,
                                                Raevyx.class,
                                                Raevyx.BREED_PARTNER_RANGE,
                                                Raevyx.BREED_DISTANCE_SQR
                                        ),
                                        new DragonFollowOwnerBehaviour<Raevyx>(
                                                DragonFollowOwnerBehaviour.Config.raevyx(),
                                                dragon -> dragon.startTakeoffSequence(
                                                        0.12D,
                                                        Raevyx.TAKEOFF_ANIMATION_TICKS
                                                )
                                        ),
                                        new DragonDrinkBehaviour<>(DragonDrinkBehaviour.Config.standard()),
                                        new RaevyxAutonomousFlightBehaviour(),
                                        new DragonGroundWanderBehaviour<>(0.6D, 60)
                                )
                        )
                        .clearWhenStopped(DragonMemories.MOVEMENT_INTENT)
                        .build()
        );
    }

    private boolean canFight(Raevyx dragon, LivingEntity target) {
        if (!dragon.isTargetValid(target)
                || dragon.isBaby()
                || dragon.isVehicle()
                || dragon.isPassenger()
                || dragon.isOrderedToSit()) {
            return false;
        }
        if (target.isInWaterOrBubble()) {
            return dragon.isAerial()
                    || dragon.distanceToSqr(target) <= DragonAirCombatHelper.maxAggroDistanceSqr(dragon, 32.0D);
        }
        if (DragonAirCombatHelper.isTargetAirborne(
                dragon,
                target,
                Raevyx.AI_AIR_COMBAT_SETTINGS.targetAirborneHeight()
        )) {
            return DragonAirCombatHelper.canEngageAirborneTarget(
                    dragon,
                    target,
                    Raevyx.AI_AIR_COMBAT_SETTINGS
            );
        }
        return dragon.isAerial()
                || dragon.distanceToSqr(target) <= DragonAirCombatHelper.maxAggroDistanceSqr(dragon, 32.0D);
    }
}
