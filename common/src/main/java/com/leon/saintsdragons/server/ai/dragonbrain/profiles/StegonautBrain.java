package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.registry.ModSensorTypes;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainOwner;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonTargetLifecycle;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ApplyMovementIntentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AsyncWaterChaseTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonBreedBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFollowParentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundFollowOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundPackFollowBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonIdleLookBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonWaterEscapeBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.FirstApplicableDragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.LookAtAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.MoveToGroundWalkTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.SetWalkTargetToAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.stegonaut.StegonautGroundCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.stegonaut.StegonautTargetingBehaviour;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public class StegonautBrain implements DragonBrainOwner<Stegonaut> {
    private static final float GROUND_CHASE_SPEED = 0.75F;
    private static final double BREED_PARTNER_RANGE = 20.0D;
    private static final double BREED_DISTANCE_SQR = 2500.0D;

    @Override
    public List<SensorType<? extends Sensor<? super Stegonaut>>> getDragonBrainSensors() {
        return List.of(
                ModSensorTypes.DRAGON_MOVEMENT_STATE.get()
        );
    }

    @Override
    public void updateActivity(Brain<Stegonaut> brain, Stegonaut dragon) {
        if (canFight(dragon)) {
            brain.setActiveActivityIfPossible(Activity.FIGHT);
            return;
        }

        LivingEntity target = brain.getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target != null && (!DragonTargetLifecycle.isValidTarget(dragon, target)
                || !withinAggroRange(dragon, target))) {
            DragonTargetLifecycle.clearCombatTarget(brain, dragon, true);
        }
        brain.useDefaultActivity();
    }

    @Override
    public List<DragonBehaviourGroup<Stegonaut>> getDragonBrainBehaviourGroups() {
        return List.of(
                DragonBehaviourGroup.<Stegonaut>activity(Activity.CORE)
                        .behaviours(
                                new StegonautTargetingBehaviour(),
                                new DragonIdleLookBehaviour<>(8.0D),
                                new ApplyMovementIntentBehaviour<>(),
                                new MoveToGroundWalkTargetBehaviour<>(),
                                new LookAtAttackTargetBehaviour<>(30.0F, 30.0F)
                        )
                        .build(),
                DragonBehaviourGroup.<Stegonaut>activity(Activity.FIGHT)
                        .behaviours(
                                new SetWalkTargetToAttackTargetBehaviour<Stegonaut>(
                                        GROUND_CHASE_SPEED,
                                        (dragon, target) ->
                                                StegonautGroundCombatBehaviour.GROUND_ATTACK_RANGE
                                                        + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D,
                                        (dragon, target) -> StegonautGroundCombatBehaviour.isAttacking(dragon)
                                ),
                                new AsyncWaterChaseTargetBehaviour<>(0.12D, 8.0F),
                                new StegonautGroundCombatBehaviour()
                        )
                        .clearWhenStopped(
                                DragonMemories.MOVEMENT_INTENT,
                                DragonMemories.WALK_TARGET,
                                DragonMemories.PATH,
                                DragonMemories.CANT_REACH_WALK_TARGET_SINCE
                        )
                        .build(),
                DragonBehaviourGroup.<Stegonaut>activity(Activity.IDLE)
                        .behaviours(
                                new FirstApplicableDragonBehaviour<>(
                                        new DragonWaterEscapeBehaviour<>(8.0F, 0.12D),
                                        new DragonFollowParentBehaviour<>(Stegonaut.class, 0.70D),
                                        new DragonBreedBehaviour<>(1.0D, Stegonaut.class,
                                                BREED_PARTNER_RANGE, BREED_DISTANCE_SQR),
                                        new DragonGroundFollowOwnerBehaviour<>(
                                                DragonGroundFollowOwnerBehaviour.Config.stegonaut()),
                                        new DragonGroundPackFollowBehaviour<>(
                                                Stegonaut.class, 0.75D, 16.0D, 8.0D),
                                        new DragonGroundWanderBehaviour<>(0.35D, 120)
                                )
                        )
                        .build()
        );
    }

    private boolean canFight(Stegonaut dragon) {
        LivingEntity target = dragon.getTarget();
        return target != null
                && DragonTargetLifecycle.isValidTarget(dragon, target)
                && dragon.canTarget(target)
                && !dragon.isVehicle()
                && !dragon.isOrderedToSit()
                && !dragon.isInLove()
                && withinAggroRange(dragon, target);
    }

    private boolean withinAggroRange(Stegonaut dragon, LivingEntity target) {
        double followRange = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 16.0D;
        }
        return dragon.distanceToSqr(target) <= followRange * followRange;
    }
}
