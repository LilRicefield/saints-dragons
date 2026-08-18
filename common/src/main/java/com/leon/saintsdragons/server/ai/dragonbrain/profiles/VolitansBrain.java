package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.registry.ModSensorTypes;
import com.leon.saintsdragons.server.ai.GroundPursuitFlightSettings;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainOwner;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ApplyMovementIntentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonHuntAndEatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AsyncWaterChaseTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFindWaterBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFollowOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonIdleLookBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonRescueFallingOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonSwimFollowBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonSwimWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonWaterEscapeBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.FirstApplicableDragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.GroundPursuitFlightTransitionBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.LookAtAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.MoveToGroundWalkTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.SetWalkTargetToAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans.VolitansAirCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans.VolitansAutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans.VolitansFindSleepDepthBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans.VolitansGroundCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans.VolitansTargetingBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans.VolitansUnderwaterBreedBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans.VolitansWaterCombatBehaviour;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public final class VolitansBrain implements DragonBrainOwner<Volitans> {
    private static final DragonRescueFallingOwnerBehaviour.Config RESCUE_CONFIG =
            DragonRescueFallingOwnerBehaviour.Config.volitans();

    @Override
    public List<SensorType<? extends Sensor<? super Volitans>>> getDragonBrainSensors() {
        return List.of(ModSensorTypes.DRAGON_MOVEMENT_STATE.get(), ModSensorTypes.DRAGON_SCENT.get());
    }

    @Override
    public void updateActivity(Brain<Volitans> brain, Volitans dragon) {
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
    public List<DragonBehaviourGroup<Volitans>> getDragonBrainBehaviourGroups() {
        VolitansGroundCombatBehaviour groundCombat = new VolitansGroundCombatBehaviour();
        return List.of(
                DragonBehaviourGroup.<Volitans>activity(Activity.CORE)
                        .behaviours(
                                new VolitansTargetingBehaviour(),
                                new DragonIdleLookBehaviour<>(8.0D),
                                new DragonHuntAndEatBehaviour<>(),
                                new ApplyMovementIntentBehaviour<>(),
                                new MoveToGroundWalkTargetBehaviour<>(),
                                new LookAtAttackTargetBehaviour<>(35.0F, 35.0F)
                        )
                        .build(),
                DragonBehaviourGroup.<Volitans>activity(Activity.FIGHT)
                        .behaviours(
                                new GroundPursuitFlightTransitionBehaviour<>(
                                        GroundPursuitFlightSettings.standard(),
                                        dragon -> groundCombat.isGroundMovementLocked(),
                                        dragon -> dragon.getTarget() == null
                                                || !dragon.getTarget().isInWaterOrBubble()
                                ),
                                new VolitansAirCombatBehaviour(),
                                new SetWalkTargetToAttackTargetBehaviour<>(
                                        VolitansGroundCombatBehaviour.CHASE_SPEED,
                                        (dragon, target) ->
                                                VolitansGroundCombatBehaviour.CHASE_STOP_RANGE
                                                        + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D,
                                        (dragon, target) -> groundCombat.isGroundMovementLocked()
                                ),
                                new AsyncWaterChaseTargetBehaviour<>(
                                        (dragon, target) -> 0.28D,
                                        8.0F,
                                        (dragon, target) -> groundCombat.isGroundMovementLocked()
                                ),
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
                DragonBehaviourGroup.<Volitans>activity(Activity.PANIC)
                        .behaviours(new DragonRescueFallingOwnerBehaviour<>(RESCUE_CONFIG))
                        .clearWhenStopped(
                                DragonMemories.RESCUE_TARGET,
                                DragonMemories.MOVEMENT_INTENT,
                                DragonMemories.WALK_TARGET,
                                DragonMemories.PATH,
                                DragonMemories.CANT_REACH_WALK_TARGET_SINCE
                        )
                        .build(),
                DragonBehaviourGroup.<Volitans>activity(Activity.IDLE)
                        .behaviours(
                                new FirstApplicableDragonBehaviour<>(
                                        new VolitansUnderwaterBreedBehaviour(
                                                1.0D,
                                                Volitans.BREED_PARTNER_RANGE,
                                                Volitans.BREED_DISTANCE_SQR
                                        ),
                                        new VolitansFindSleepDepthBehaviour(6.0F, 0.16D),
                                        new DragonWaterEscapeBehaviour<>(
                                                8.0F,
                                                0.28D,
                                                Volitans::shouldLeaveWater,
                                                VolitansBrain::canContinueLeavingWater
                                        ),
                                        new DragonFindWaterBehaviour<>(1.0D),
                                        new DragonFollowOwnerBehaviour<>(
                                                DragonFollowOwnerBehaviour.Config.volitans(),
                                                dragon -> dragon.startTakeoffSequence(
                                                        0.12D,
                                                        Volitans.TAKEOFF_ANIMATION_TICKS
                                                )
                                        ),
                                        new DragonSwimFollowBehaviour<>(
                                                Volitans.class,
                                                8.0F,
                                                0.24D,
                                                20.0D,
                                                8.0D,
                                                dragon -> !dragon.isSleepLocked()
                                        ),
                                        new VolitansAutonomousFlightBehaviour(),
                                        new DragonGroundWanderBehaviour<>(
                                                0.9D,
                                                70,
                                                10,
                                                dragon -> !dragon.isInWaterOrBubble(),
                                                (dragon, position) -> true
                                        ),
                                        new DragonSwimWanderBehaviour<>(
                                                6.0F,
                                                0.20D,
                                                30,
                                                dragon -> !dragon.isSleepLocked(),
                                                (dragon, position) -> true
                                        )
                                )
                        )
                        .clearWhenStopped(DragonMemories.MOVEMENT_INTENT)
                        .build()
        );
    }

    private boolean canFight(Volitans dragon, LivingEntity target) {
        if (!dragon.isTargetValid(target)
                || dragon.isBaby()
                || dragon.isVehicle()
                || dragon.isPassenger()
                || dragon.isOrderedToSit()
                || dragon.isAiSpecialCombatActive()
                || dragon.isAiSpecialCombatReserved()) {
            return false;
        }
        double followRange = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 32.0D;
        }
        return dragon.distanceToSqr(target) <= followRange * followRange;
    }

    private static boolean canContinueLeavingWater(Volitans dragon) {
        if (!dragon.canSwim() || dragon.isOrderedToSit()) {
            return false;
        }
        LivingEntity owner = dragon.getOwner();
        return !dragon.isTame()
                || dragon.getCommand() != 0
                || owner == null
                || !owner.isAlive()
                || owner.level() != dragon.level()
                || !owner.isInWaterOrBubble();
    }
}
