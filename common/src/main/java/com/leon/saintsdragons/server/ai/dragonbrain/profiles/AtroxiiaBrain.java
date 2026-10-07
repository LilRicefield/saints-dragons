package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.config.dragon.profile.AtroxiiaStatProfile;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.GroundDragonBrain;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AsyncWaterChaseTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundFollowOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonBreedBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonWaterEscapeBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.SetWalkTargetToAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.atroxiia.AtroxiiaGroundCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.atroxiia.AtroxiiaTargetingBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.atroxiia.AtroxiiaWaterCombatBehaviour;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public final class AtroxiiaBrain extends GroundDragonBrain<Atroxiia> {

    @Override
    public boolean usesDragonScent() {
        return true;
    }

    @Override
    protected List<DragonBehaviourGroup<Atroxiia>> createBehaviourGroups() {
        return List.of(
                coreGroup(30.0F, new AtroxiiaTargetingBehaviour()),
                fightGroup(
                    new SetWalkTargetToAttackTargetBehaviour<>(
                            AtroxiiaGroundCombatBehaviour.CHASE_SPEED,
                            AtroxiiaGroundCombatBehaviour::meleeStopRange,
                            (dragon, target) -> AtroxiiaGroundCombatBehaviour.isMovementCommitted(dragon)
                    ),
                    new AsyncWaterChaseTargetBehaviour<>(AtroxiiaStatProfile.Brain.WATER_CHASE_SPEED, AtroxiiaStatProfile.Brain.WATER_CHASE_TURN_DEGREES),
                    new AtroxiiaGroundCombatBehaviour(),
                    new AtroxiiaWaterCombatBehaviour()
                ),
                idleGroup(
                    new DragonWaterEscapeBehaviour<>(AtroxiiaStatProfile.Brain.WATER_ESCAPE_TURN_DEGREES, AtroxiiaStatProfile.Brain.WATER_ESCAPE_SPEED),
                    new DragonBreedBehaviour<>(AtroxiiaStatProfile.Brain.BREED_SPEED, Atroxiia.class,
                            Atroxiia.BREED_PARTNER_RANGE, Atroxiia.BREED_DISTANCE_SQR),
                    new DragonGroundFollowOwnerBehaviour<>(
                            DragonGroundFollowOwnerBehaviour.Config.standardAdult()),
                    new DragonGroundWanderBehaviour<>(AtroxiiaStatProfile.Brain.GROUND_WANDER_SPEED, 100)
                )
        );
    }

    @Override
    protected boolean canFight(Atroxiia dragon, LivingEntity target) {
        return super.canFight(dragon, target)
                && dragon.canTarget(target)
                && !dragon.isBaby()
                && !dragon.isTamingStunned()
                && !dragon.isPassenger()
                && (dragon.isGroundedForAction() || dragon.isInWaterOrBubble());
    }

    @Override
    protected boolean withinAggroRange(Atroxiia dragon, LivingEntity target) {
        double followRange = Math.max(16.0D, dragon.getAttributeValue(Attributes.FOLLOW_RANGE));
        return dragon.distanceToSqr(target) <= followRange * followRange;
    }
}
