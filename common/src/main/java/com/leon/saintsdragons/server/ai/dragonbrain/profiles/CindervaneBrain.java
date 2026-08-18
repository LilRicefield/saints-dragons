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
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonPackFollowBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonRescueFallingOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonWaterEscapeBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.FirstApplicableDragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.GroundPursuitFlightTransitionBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.LookAtAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.MoveToGroundWalkTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.SetWalkTargetToAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane.CindervaneAirCombatMovementBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane.CindervaneAutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane.CindervaneGroundCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane.CindervaneTargetingBehaviour;
import com.leon.saintsdragons.server.ai.DragonAirCombatHelper;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public class CindervaneBrain implements DragonBrainOwner<Cindervane> {
    private static final DragonRescueFallingOwnerBehaviour.Config RESCUE_CONFIG =
            DragonRescueFallingOwnerBehaviour.Config.cindervane();

    @Override
    public List<SensorType<? extends Sensor<? super Cindervane>>> getDragonBrainSensors() {
        return List.of(ModSensorTypes.DRAGON_MOVEMENT_STATE.get());
    }

    @Override
    public void updateActivity(Brain<Cindervane> brain, Cindervane dragon) {
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
    public List<DragonBehaviourGroup<Cindervane>> getDragonBrainBehaviourGroups() {
        return List.of(
                DragonBehaviourGroup.<Cindervane>activity(Activity.CORE)
                        .behaviours(
                                new CindervaneTargetingBehaviour(),
                                new DragonIdleLookBehaviour<>(8.0D),
                                new DragonHuntAndEatBehaviour<>(),
                                new ApplyMovementIntentBehaviour<>(),
                                new MoveToGroundWalkTargetBehaviour<>(),
                                new LookAtAttackTargetBehaviour<>(30.0F, 30.0F)
                        )
                        .build(),
                DragonBehaviourGroup.<Cindervane>activity(Activity.FIGHT)
                        .behaviours(
                                new GroundPursuitFlightTransitionBehaviour<>(
                                        GroundPursuitFlightSettings.standard(),
                                        CindervaneGroundCombatBehaviour::isMovementCommitted
                                ),
                                new CindervaneAirCombatMovementBehaviour(),
                                new SetWalkTargetToAttackTargetBehaviour<>(
                                        CindervaneGroundCombatBehaviour.CHASE_SPEED,
                                        CindervaneGroundCombatBehaviour::groundStopRange,
                                        (dragon, target) -> CindervaneGroundCombatBehaviour.isMovementCommitted(dragon)
                                ),
                                new AsyncWaterChaseTargetBehaviour<>(0.12D, 8.0F),
                                new CindervaneGroundCombatBehaviour()
                        )
                        .clearWhenStopped(
                                DragonMemories.MOVEMENT_INTENT,
                                DragonMemories.WALK_TARGET,
                                DragonMemories.PATH,
                                DragonMemories.CANT_REACH_WALK_TARGET_SINCE
                        )
                        .build(),
                DragonBehaviourGroup.<Cindervane>activity(Activity.PANIC)
                        .behaviours(new DragonRescueFallingOwnerBehaviour<>(RESCUE_CONFIG))
                        .clearWhenStopped(
                                DragonMemories.RESCUE_TARGET,
                                DragonMemories.MOVEMENT_INTENT,
                                DragonMemories.WALK_TARGET,
                                DragonMemories.PATH,
                                DragonMemories.CANT_REACH_WALK_TARGET_SINCE
                        )
                        .build(),
                DragonBehaviourGroup.<Cindervane>activity(Activity.IDLE)
                        .behaviours(
                                new FirstApplicableDragonBehaviour<>(
                                        new DragonWaterEscapeBehaviour<>(8.0F, 0.12D),
                                        new DragonBreedBehaviour<>(
                                                1.0D,
                                                Cindervane.class,
                                                Cindervane.BREED_PARTNER_RANGE,
                                                Cindervane.BREED_DISTANCE_SQR
                                        ),
                                        new DragonPackFollowBehaviour<>(Cindervane.class, 1.0D, 20.0D, 10.0D),
                                        new DragonFollowParentBehaviour<>(Cindervane.class, 1.15D),
                                        new DragonFollowOwnerBehaviour<>(
                                                DragonFollowOwnerBehaviour.Config.cindervane(),
                                                dragon -> dragon.startTakeoffSequence(
                                                        0.12D,
                                                        Cindervane.TAKEOFF_ANIMATION_TICKS
                                                )
                                        ),
                                        new DragonDrinkBehaviour<>(DragonDrinkBehaviour.Config.standard()),
                                        new CindervaneAutonomousFlightBehaviour(),
                                        new DragonGroundWanderBehaviour<>(1.0D, 160)
                                )
                        )
                        .clearWhenStopped(DragonMemories.MOVEMENT_INTENT)
                        .build()
        );
    }

    private boolean canFight(Cindervane dragon, LivingEntity target) {
        if (!dragon.isTargetValid(target)
                || dragon.isBaby()
                || dragon.isVehicle()
                || dragon.isPassenger()
                || dragon.isOrderedToSit()
                || dragon.isInLava()) {
            return false;
        }
        if (DragonAirCombatHelper.isTargetAirborne(
                dragon,
                target,
                Cindervane.AI_AIR_COMBAT_SETTINGS.targetAirborneHeight()
        )) {
            return DragonAirCombatHelper.canEngageAirborneTarget(
                    dragon,
                    target,
                    Cindervane.AI_AIR_COMBAT_SETTINGS
            );
        }
        return dragon.isAerial()
                || !dragon.isFlying()
                && !dragon.isHovering()
                && !dragon.isTakeoff()
                && !dragon.isLanding();
    }
}
