package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.config.dragon.profile.StegonautStatProfile;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.GroundDragonBrain;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AsyncWaterChaseTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonBreedBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFollowParentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundFollowOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundPackFollowBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonWaterEscapeBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.SetWalkTargetToAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.stegonaut.StegonautGroundCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.stegonaut.StegonautTargetingBehaviour;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public class StegonautBrain extends GroundDragonBrain<Stegonaut> {
    private static final float GROUND_CHASE_SPEED = StegonautStatProfile.Brain.GROUND_CHASE_SPEED;

    @Override
    protected List<DragonBehaviourGroup<Stegonaut>> createBehaviourGroups() {
        return List.of(
                coreGroup(30.0F, new StegonautTargetingBehaviour()),
                fightGroup(
                    new SetWalkTargetToAttackTargetBehaviour<>(
                            GROUND_CHASE_SPEED,
                            (dragon, target) ->
                                    StegonautGroundCombatBehaviour.GROUND_ATTACK_RANGE
                                            + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D,
                            (dragon, target) -> StegonautGroundCombatBehaviour.isAttacking(dragon)
                    ),
                    new AsyncWaterChaseTargetBehaviour<>(StegonautStatProfile.Brain.WATER_CHASE_SPEED, StegonautStatProfile.Brain.WATER_CHASE_TURN_DEGREES),
                    new StegonautGroundCombatBehaviour()
                ),
                idleGroup(
                    new DragonWaterEscapeBehaviour<>(StegonautStatProfile.Brain.WATER_ESCAPE_TURN_DEGREES, StegonautStatProfile.Brain.WATER_ESCAPE_SPEED),
                    new DragonFollowParentBehaviour<>(Stegonaut.class, StegonautStatProfile.Brain.FOLLOW_PARENT_SPEED),
                    new DragonBreedBehaviour<>(StegonautStatProfile.Brain.BREED_SPEED,
                            Stegonaut.class,
                            Stegonaut.BREED_PARTNER_RANGE,
                            Stegonaut.BREED_DISTANCE_SQR),
                    new DragonGroundFollowOwnerBehaviour<>(
                            DragonGroundFollowOwnerBehaviour.Config.standardAdult()),
                    new DragonGroundPackFollowBehaviour<>(
                            Stegonaut.class, 16.0D, 8.0D),
                    new DragonGroundWanderBehaviour<>(StegonautStatProfile.Brain.GROUND_WANDER_SPEED, 120)
                )
        );
    }

    @Override
    protected boolean canFight(Stegonaut dragon, LivingEntity target) {
        return super.canFight(dragon, target)
                && dragon.canTarget(target)
                && !dragon.isInLove();
    }

    @Override
    protected boolean withinAggroRange(Stegonaut dragon, LivingEntity target) {
        double followRange = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 16.0D;
        }
        return dragon.distanceToSqr(target) <= followRange * followRange;
    }
}
