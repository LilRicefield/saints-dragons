package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.config.dragon.profile.RaevyxStatProfile;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.FlyingDragonBrain;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AsyncWaterChaseTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonBreedBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonDrinkBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFollowOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFollowParentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonRescueFallingOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonWaterEscapeBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.SetWalkTargetToAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx.RaevyxAirCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx.RaevyxAutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx.RaevyxGroundCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx.RaevyxTargetingBehaviour;
import com.leon.saintsdragons.server.ai.DragonAirCombatHelper;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class RaevyxBrain extends FlyingDragonBrain<Raevyx> {
    @Override
    public boolean usesCombatDecisionSupport() { return true; }

    private static final DragonRescueFallingOwnerBehaviour.Config RESCUE_CONFIG =
            DragonRescueFallingOwnerBehaviour.Config.raevyx();

    @Override
    protected DragonRescueFallingOwnerBehaviour.Config getRescueConfig() {
        return RESCUE_CONFIG;
    }

    @Override
    protected List<DragonBehaviourGroup<Raevyx>> createBehaviourGroups() {
        return List.of(
                huntingCoreGroup(30.0F,
                    new RaevyxTargetingBehaviour(),
                    new DragonWaterEscapeBehaviour<>(RaevyxStatProfile.Brain.WATER_ESCAPE_TURN_DEGREES, RaevyxStatProfile.Brain.WATER_ESCAPE_SPEED)
                ),
                fightGroup(
                    new RaevyxAirCombatBehaviour(),
                    new SetWalkTargetToAttackTargetBehaviour<>(
                            RaevyxGroundCombatBehaviour.CHASE_SPEED,
                            (dragon, target) ->
                                    RaevyxGroundCombatBehaviour.meleeStopRange(dragon, target)
                                            + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D,
                            (dragon, target) -> dragon.getActiveAbility() != null
                                    || dragon.isDashing()
                                    || dragon.isDodging()
                                    || dragon.isGroundRending()
                    ),
                    new RaevyxGroundCombatBehaviour(),
                    new AsyncWaterChaseTargetBehaviour<>(RaevyxStatProfile.Brain.WATER_CHASE_SPEED, RaevyxStatProfile.Brain.WATER_CHASE_TURN_DEGREES)
                ),
                idleGroup(
                    new DragonFollowParentBehaviour<>(Raevyx.class, RaevyxStatProfile.Brain.FOLLOW_PARENT_SPEED),
                    new DragonBreedBehaviour<>(
                            RaevyxStatProfile.Brain.BREED_SPEED,
                            Raevyx.class,
                            Raevyx.BREED_PARTNER_RANGE,
                            Raevyx.BREED_DISTANCE_SQR
                    ),
                    new DragonFollowOwnerBehaviour<>(
                            DragonFollowOwnerBehaviour.Config.raevyx(),
                            dragon -> dragon.startTakeoffSequence(
                                    0.12D,
                                    Raevyx.TAKEOFF_ANIMATION_TICKS
                            )
                    ),
                    new DragonDrinkBehaviour<>(DragonDrinkBehaviour.Config.standard()),
                    new RaevyxAutonomousFlightBehaviour(),
                    new DragonGroundWanderBehaviour<>(RaevyxStatProfile.Brain.GROUND_WANDER_SPEED, 60)
                )
        );
    }

    @Override
    protected boolean canFight(Raevyx dragon, LivingEntity target) {
        if (!super.canFight(dragon, target)) {
            return false;
        }
        return dragon.isAerial()
                || dragon.distanceToSqr(target) <= DragonAirCombatHelper.maxAggroDistanceSqr(dragon, 32.0D);
    }
}
