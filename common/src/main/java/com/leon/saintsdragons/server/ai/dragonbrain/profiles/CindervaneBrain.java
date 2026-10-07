package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.config.dragon.profile.CindervaneStatProfile;

import com.leon.saintsdragons.server.ai.GroundPursuitFlightSettings;
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
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.GroundPursuitFlightTransitionBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.SetWalkTargetToAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane.CindervaneAirCombatMovementBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane.CindervaneAutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane.CindervaneGroundCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane.CindervanePackFollowBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane.CindervaneTargetingBehaviour;
import com.leon.saintsdragons.server.ai.DragonAirCombatHelper;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class CindervaneBrain extends FlyingDragonBrain<Cindervane> {
    private static final DragonRescueFallingOwnerBehaviour.Config RESCUE_CONFIG =
            DragonRescueFallingOwnerBehaviour.Config.cindervane();

    @Override
    public boolean usesDragonScent() {
        return false;
    }

    @Override
    protected DragonRescueFallingOwnerBehaviour.Config getRescueConfig() {
        return RESCUE_CONFIG;
    }

    @Override
    protected List<DragonBehaviourGroup<Cindervane>> createBehaviourGroups() {
        return List.of(
                huntingCoreGroup(30.0F,
                    new CindervaneTargetingBehaviour(),
                    new DragonWaterEscapeBehaviour<>(CindervaneStatProfile.Brain.WATER_ESCAPE_TURN_DEGREES, CindervaneStatProfile.Brain.WATER_ESCAPE_SPEED)
                ),
                fightGroup(
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
                    new AsyncWaterChaseTargetBehaviour<>(CindervaneStatProfile.Brain.WATER_CHASE_SPEED, CindervaneStatProfile.Brain.WATER_CHASE_TURN_DEGREES),
                    new CindervaneGroundCombatBehaviour()
                ),
                idleGroup(
                    new DragonBreedBehaviour<>(
                            CindervaneStatProfile.Brain.BREED_SPEED,
                            Cindervane.class,
                            Cindervane.BREED_PARTNER_RANGE,
                            Cindervane.BREED_DISTANCE_SQR
                    ),
                    new CindervanePackFollowBehaviour(),
                    new DragonFollowParentBehaviour<>(Cindervane.class, CindervaneStatProfile.Brain.FOLLOW_PARENT_SPEED),
                    new DragonFollowOwnerBehaviour<>(
                            DragonFollowOwnerBehaviour.Config.cindervane(),
                            dragon -> dragon.startTakeoffSequence(
                                    0.12D,
                                    Cindervane.TAKEOFF_ANIMATION_TICKS
                            )
                    ),
                    new DragonDrinkBehaviour<>(DragonDrinkBehaviour.Config.standard()),
                    new CindervaneAutonomousFlightBehaviour(),
                    new DragonGroundWanderBehaviour<>(CindervaneStatProfile.Brain.GROUND_WANDER_SPEED, 160)
                )
        );
    }

    @Override
    protected boolean canFight(Cindervane dragon, LivingEntity target) {
        if (!super.canFight(dragon, target)
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
