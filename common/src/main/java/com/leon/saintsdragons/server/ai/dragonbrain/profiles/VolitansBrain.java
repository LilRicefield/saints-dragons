package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.config.dragon.profile.VolitansStatProfile;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.FlyingDragonBrain;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AsyncWaterChaseTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFindWaterBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFollowOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonRescueFallingOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonSwimFollowBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonSwimWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonWaterEscapeBehaviour;
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
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public final class VolitansBrain extends FlyingDragonBrain<Volitans> {
    @Override
    public boolean usesCombatDecisionSupport() { return true; }

    private static final DragonRescueFallingOwnerBehaviour.Config RESCUE_CONFIG =
            DragonRescueFallingOwnerBehaviour.Config.volitans();

    @Override
    protected DragonRescueFallingOwnerBehaviour.Config getRescueConfig() {
        return RESCUE_CONFIG;
    }

    @Override
    protected List<DragonBehaviourGroup<Volitans>> createBehaviourGroups() {
        VolitansGroundCombatBehaviour groundCombat = new VolitansGroundCombatBehaviour();
        return List.of(
                huntingCoreGroup(35.0F,
                    new VolitansTargetingBehaviour()
                ),
                fightGroup(
                    new VolitansAirCombatBehaviour(),
                    new SetWalkTargetToAttackTargetBehaviour<>(
                            VolitansGroundCombatBehaviour.CHASE_SPEED,
                            (dragon, target) -> dragon.getBreathCombat().groundStopDistance(target),
                            (dragon, target) -> groundCombat.isGroundMovementLocked()
                    ),
                    new AsyncWaterChaseTargetBehaviour<>(
                            (dragon, target) -> dragon.isBreathing() ? VolitansStatProfile.Brain.WATER_CHASE_BREATH_SPEED : VolitansStatProfile.Brain.WATER_CHASE_SPEED,
                            VolitansStatProfile.Brain.WATER_CHASE_TURN_DEGREES,
                            (dragon, target) -> dragon.shouldAiHoldPositionForAbility() || dragon.isGroundMobilityActive()
                                    || dragon.getWaterCombatMovement().holdForMelee(),
                            (dragon, target) -> dragon.getWaterCombatMovement().destination(target)
                    ),
                    groundCombat,
                    new VolitansWaterCombatBehaviour()
                ),
                idleGroup(
                    new VolitansUnderwaterBreedBehaviour(
                            1.0D,
                            Volitans.BREED_PARTNER_RANGE,
                            Volitans.BREED_DISTANCE_SQR
                    ),
                    new VolitansFindSleepDepthBehaviour(6.0F, 0.16D),
                    new DragonWaterEscapeBehaviour<>(
                            VolitansStatProfile.Brain.WATER_ESCAPE_TURN_DEGREES,
                            VolitansStatProfile.Brain.WATER_ESCAPE_SPEED,
                            Volitans::shouldLeaveWater,
                            Volitans::canContinueLeavingWater
                    ),
                    new DragonFindWaterBehaviour<>(VolitansStatProfile.Brain.FIND_WATER_SPEED),
                    new DragonFollowOwnerBehaviour<>(
                            DragonFollowOwnerBehaviour.Config.volitans(),
                            dragon -> dragon.startTakeoffSequence(
                                    0.12D,
                                    Volitans.TAKEOFF_ANIMATION_TICKS
                            )
                    ),
                    new DragonSwimFollowBehaviour<>(
                            Volitans.class,
                            VolitansStatProfile.Brain.SWIM_FOLLOW_TURN_DEGREES,
                            VolitansStatProfile.Brain.SWIM_FOLLOW_SPEED,
                            20.0D,
                            8.0D,
                            dragon -> !dragon.isSleepLocked()
                    ),
                    new VolitansAutonomousFlightBehaviour(),
                    new DragonGroundWanderBehaviour<>(
                            VolitansStatProfile.Brain.GROUND_WANDER_SPEED,
                            70,
                            10,
                            dragon -> !dragon.isInWaterOrBubble(),
                            (dragon, position) -> true
                    ),
                    new DragonSwimWanderBehaviour<>(
                            VolitansStatProfile.Brain.SWIM_WANDER_TURN_DEGREES,
                            VolitansStatProfile.Brain.SWIM_WANDER_SPEED,
                            30,
                            dragon -> !dragon.isSleepLocked(),
                            (dragon, position) -> true
                    )
                )
        );
    }

    @Override
    protected boolean canFight(Volitans dragon, LivingEntity target) {
        if (!super.canFight(dragon, target)
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

}
