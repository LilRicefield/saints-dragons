package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.registry.ModSensorTypes;
import com.leon.saintsdragons.server.ai.GroundPursuitFlightSettings;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainOwner;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.*;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus.IgnivorusAirCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus.IgnivorusAutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus.IgnivorusGroundCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus.IgnivorusTargetingBehaviour;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public class IgnivorusBrain implements DragonBrainOwner<Ignivorus> {
    private static final DragonRescueFallingOwnerBehaviour.Config RESCUE_CONFIG =
            DragonRescueFallingOwnerBehaviour.Config.ignivorus();

    @Override
    public List<SensorType<? extends Sensor<? super Ignivorus>>> getDragonBrainSensors() {
        return List.of(ModSensorTypes.DRAGON_MOVEMENT_STATE.get(), ModSensorTypes.DRAGON_SCENT.get());
    }

    @Override
    public void updateActivity(Brain<Ignivorus> brain, Ignivorus dragon) {
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
    public List<DragonBehaviourGroup<Ignivorus>> getDragonBrainBehaviourGroups() {
        IgnivorusGroundCombatBehaviour groundCombat = new IgnivorusGroundCombatBehaviour();
        return List.of(
                DragonBehaviourGroup.<Ignivorus>activity(Activity.CORE)
                        .behaviours(
                                new IgnivorusTargetingBehaviour(),
                                new DragonIdleLookBehaviour<>(8.0D),
                                new DragonHuntAndEatBehaviour<>(),
                                new ApplyMovementIntentBehaviour<>(),
                                new MoveToGroundWalkTargetBehaviour<>(),
                                new LookAtAttackTargetBehaviour<>(30.0F, 30.0F)
                        )
                        .build(),
                DragonBehaviourGroup.<Ignivorus>activity(Activity.FIGHT)
                        .behaviours(
                                new GroundPursuitFlightTransitionBehaviour<>(
                                        GroundPursuitFlightSettings.standard(),
                                        dragon -> groundCombat.isGroundMovementLocked(),
                                        dragon -> false
                                ),
                                new IgnivorusAirCombatBehaviour(),
                                new SetWalkTargetToAttackTargetBehaviour<Ignivorus>(
                                        IgnivorusGroundCombatBehaviour.CHASE_SPEED,
                                        (dragon, targetEntity) ->
                                                IgnivorusGroundCombatBehaviour.MELEE_ENGAGE_RANGE
                                                        + (dragon.getBbWidth() + targetEntity.getBbWidth()) * 0.5D,
                                        (dragon, targetEntity) -> groundCombat.isGroundMovementLocked()
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
                DragonBehaviourGroup.<Ignivorus>activity(Activity.PANIC)
                        .behaviours(new DragonRescueFallingOwnerBehaviour<>(RESCUE_CONFIG))
                        .clearWhenStopped(
                                DragonMemories.RESCUE_TARGET,
                                DragonMemories.MOVEMENT_INTENT,
                                DragonMemories.WALK_TARGET,
                                DragonMemories.PATH,
                                DragonMemories.CANT_REACH_WALK_TARGET_SINCE
                        )
                        .build(),
                DragonBehaviourGroup.<Ignivorus>activity(Activity.IDLE)
                        .behaviours(
                                new FirstApplicableDragonBehaviour<>(
                                        new DragonWaterEscapeBehaviour<>(8.0F, 0.12D),
                                        new DragonBreedBehaviour<>(
                                                1.0D,
                                                Ignivorus.class,
                                                Ignivorus.BREED_PARTNER_RANGE,
                                                Ignivorus.BREED_DISTANCE_SQR
                                        ),
                                        new ReturnToRoostBehaviour<>(
                                                Ignivorus.ROOST_SLEEP_RADIUS,
                                                Ignivorus.ROOST_TERRITORY_RADIUS,
                                                Ignivorus.ROOST_TERRITORY_RETURN_RADIUS,
                                                1.0F,
                                                0.25D,
                                                8.0F,
                                                1.5D
                                        ),
                                        new DragonFollowOwnerBehaviour<Ignivorus>(
                                                DragonFollowOwnerBehaviour.Config.ignivorus(),
                                                dragon -> dragon.startTakeoffSequence(
                                                        0.12D,
                                                        Ignivorus.TAKEOFF_ANIMATION_TICKS
                                                )
                                        ),
                                        new DragonFollowParentBehaviour<>(Ignivorus.class, 1.1D),
                                        new DragonDrinkBehaviour<>(
                                                DragonDrinkBehaviour.Config.standard().withSearchRadius(20)
                                        ),
                                        new IgnivorusAutonomousFlightBehaviour(),
                                        new DragonGroundWanderBehaviour<>(
                                                1.0D,
                                                120,
                                                10,
                                                dragon -> !dragon.isInWaterOrBubble()
                                                        && !dragon.shouldSuspendRoostWandering(),
                                                Ignivorus::isWithinRoostWanderArea
                                        )
                                )
                        )
                        .clearWhenStopped(DragonMemories.MOVEMENT_INTENT)
                        .build()
        );
    }

    private boolean canFight(Ignivorus dragon, LivingEntity target) {
        if (!dragon.isTargetValid(target)
                || dragon.isBaby()
                || dragon.isVehicle()
                || dragon.isPassenger()
                || dragon.isOrderedToSit()
                || dragon.isAiSpecialCombatActive()
                || dragon.areRiderControlsLocked()
                || dragon.isLeaping()
                || dragon.isLeapImpactRecovering()) {
            return false;
        }
        double followRange = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) followRange = 32.0D;
        return dragon.distanceToSqr(target) <= followRange * followRange;
    }
}
