package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.registry.ModSensorTypes;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainOwner;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ApplyMovementIntentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AsyncWaterChaseTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.LookAtAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.MoveToGroundWalkTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.SetWalkTargetToAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.stegonaut.StegonautGroundCombatBehaviour;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public class StegonautCombatBrain implements DragonBrainOwner<Stegonaut> {
    private static final float GROUND_CHASE_SPEED = 0.75F;

    @Override
    public List<SensorType<? extends Sensor<? super Stegonaut>>> getDragonBrainSensors() {
        return List.of(
                ModSensorTypes.DRAGON_TARGET.get(),
                ModSensorTypes.DRAGON_MOVEMENT_STATE.get()
        );
    }

    @Override
    public void updateActivity(Brain<Stegonaut> brain, Stegonaut dragon) {
        if (canFight(dragon)) {
            brain.setActiveActivityIfPossible(Activity.FIGHT);
            return;
        }

        LivingEntity target = dragon.getTarget();
        if (target != null && (!dragon.isTargetValid(target) || !withinAggroRange(dragon, target))) {
            dragon.setTarget(null);
            brain.eraseMemory(DragonMemories.ATTACK_TARGET);
        }
        brain.useDefaultActivity();
    }

    @Override
    public boolean shouldTakeControl(Stegonaut dragon) {
        if (canFight(dragon)) {
            return true;
        }
        if (dragon.getTarget() != null && dragon.getTarget().isAlive()) {
            return false;
        }
        return DragonBrainOwner.super.shouldTakeControl(dragon);
    }

    @Override
    public List<DragonBehaviourGroup<Stegonaut>> getDragonBrainBehaviourGroups() {
        return List.of(
                DragonBehaviourGroup.<Stegonaut>activity(Activity.CORE)
                        .behaviours(
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
                DragonBehaviourGroup.<Stegonaut>activity(Activity.IDLE).build()
        );
    }

    private boolean canFight(Stegonaut dragon) {
        LivingEntity target = dragon.getTarget();
        return target != null
                && dragon.isTargetValid(target)
                && dragon.canTarget(target)
                && !dragon.isVehicle()
                && !dragon.isOrderedToSit()
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
