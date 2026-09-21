package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.config.dragon.profile.IgnivorusStatProfile;

import com.leon.saintsdragons.common.registry.ModSensorTypes;
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
    @Override
    public boolean usesCombatDecisionSupport() { return true; }

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
            brain.setActiveActivityIfPossible(getCombatActivity(brain));
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
                                new IgnivorusAirCombatBehaviour(),
                                new SetWalkTargetToAttackTargetBehaviour<>(
                                        IgnivorusGroundCombatBehaviour.CHASE_SPEED,
                                        groundCombat::getPreferredStopDistance,
                                        (dragon, targetEntity) -> groundCombat.isGroundMovementLocked()
                                ),
                                new AsyncWaterChaseTargetBehaviour<>(IgnivorusStatProfile.Brain.WATER_CHASE_SPEED, IgnivorusStatProfile.Brain.WATER_CHASE_TURN_DEGREES),
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
                                        new DragonWaterEscapeBehaviour<>(IgnivorusStatProfile.Brain.WATER_ESCAPE_TURN_DEGREES, IgnivorusStatProfile.Brain.WATER_ESCAPE_SPEED),
                                        new DragonBreedBehaviour<>(
                                                IgnivorusStatProfile.Brain.BREED_SPEED,
                                                Ignivorus.class,
                                                Ignivorus.BREED_PARTNER_RANGE,
                                                Ignivorus.BREED_DISTANCE_SQR
                                        ),
                                        new ReturnToRoostBehaviour<>(
                                                Ignivorus.ROOST_SLEEP_RADIUS,
                                                Ignivorus.ROOST_TERRITORY_RADIUS,
                                                Ignivorus.ROOST_TERRITORY_RETURN_RADIUS,
                                                IgnivorusStatProfile.Brain.ROOST_RETURN_GROUND_SPEED,
                                                IgnivorusStatProfile.Brain.ROOST_RETURN_SWIM_SPEED,
                                                IgnivorusStatProfile.Brain.ROOST_RETURN_SWIM_TURN_DEGREES,
                                                1.5D
                                        ),
                                        new DragonFollowOwnerBehaviour<>(
                                                DragonFollowOwnerBehaviour.Config.ignivorus(),
                                                dragon -> dragon.startTakeoffSequence(
                                                        0.12D,
                                                        Ignivorus.TAKEOFF_ANIMATION_TICKS
                                                )
                                        ),
                                        new DragonFollowParentBehaviour<>(Ignivorus.class, IgnivorusStatProfile.Brain.FOLLOW_PARENT_SPEED),
                                        new DragonDrinkBehaviour<>(
                                                DragonDrinkBehaviour.Config.standard().withSearchRadius(20)
                                        ),
                                        new IgnivorusAutonomousFlightBehaviour(),
                                        new DragonGroundWanderBehaviour<>(
                                                IgnivorusStatProfile.Brain.GROUND_WANDER_SPEED,
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
        if (followRange <= 0.0D) followRange = Ignivorus.BASE_FOLLOW_RANGE;
        return dragon.distanceToSqr(target) <= followRange * followRange;
    }
}
