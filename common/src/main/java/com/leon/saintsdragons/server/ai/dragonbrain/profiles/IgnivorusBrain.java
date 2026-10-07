package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.config.dragon.profile.IgnivorusStatProfile;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.FlyingDragonBrain;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.*;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus.IgnivorusAirCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus.IgnivorusAutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus.IgnivorusGroundCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus.IgnivorusTargetingBehaviour;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public class IgnivorusBrain extends FlyingDragonBrain<Ignivorus> {
    @Override
    public boolean usesCombatDecisionSupport() { return true; }

    private static final DragonRescueFallingOwnerBehaviour.Config RESCUE_CONFIG =
            DragonRescueFallingOwnerBehaviour.Config.ignivorus();

    @Override
    protected DragonRescueFallingOwnerBehaviour.Config getRescueConfig() {
        return RESCUE_CONFIG;
    }

    @Override
    protected List<DragonBehaviourGroup<Ignivorus>> createBehaviourGroups() {
        IgnivorusGroundCombatBehaviour groundCombat = new IgnivorusGroundCombatBehaviour();
        return List.of(
                huntingCoreGroup(30.0F,
                    new IgnivorusTargetingBehaviour(),
                    new DragonWaterEscapeBehaviour<>(IgnivorusStatProfile.Brain.WATER_ESCAPE_TURN_DEGREES, IgnivorusStatProfile.Brain.WATER_ESCAPE_SPEED)
                ),
                fightGroup(
                    new IgnivorusAirCombatBehaviour(),
                    new SetWalkTargetToAttackTargetBehaviour<>(
                            IgnivorusGroundCombatBehaviour.CHASE_SPEED,
                            groundCombat::getPreferredStopDistance,
                            (dragon, targetEntity) -> groundCombat.isGroundMovementLocked()
                    ),
                    new AsyncWaterChaseTargetBehaviour<>(IgnivorusStatProfile.Brain.WATER_CHASE_SPEED, IgnivorusStatProfile.Brain.WATER_CHASE_TURN_DEGREES),
                    groundCombat
                ),
                idleGroup(
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
                            IgnivorusStatProfile.Brain.ROOST_RETURN_AIR_SPEED
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
        );
    }

    @Override
    protected boolean canFight(Ignivorus dragon, LivingEntity target) {
        if (!super.canFight(dragon, target)
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
