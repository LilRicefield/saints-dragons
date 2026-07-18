package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.registry.ModSensorTypes;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainOwner;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ApplyMovementIntentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonIdleLookBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.FirstApplicableDragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw.NulljawBreedBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw.NulljawFloatWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw.NulljawFollowOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw.NulljawFollowParentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw.NulljawPackFollowBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw.NulljawShulkerBulletSensorBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw.NulljawTacticalCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw.NulljawTargetingBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw.NulljawTemptBehaviour;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public final class NulljawBrain implements DragonBrainOwner<Nulljaw> {
    @Override
    public List<SensorType<? extends Sensor<? super Nulljaw>>> getDragonBrainSensors() {
        return List.of(ModSensorTypes.DRAGON_MOVEMENT_STATE.get());
    }

    @Override
    public void updateActivity(Brain<Nulljaw> brain, Nulljaw dragon) {
        Entity projectile = brain.getMemory(DragonMemories.INTERCEPT_PROJECTILE).orElse(null);
        LivingEntity target = brain.getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        if (NulljawShulkerBulletSensorBehaviour.isValidThreat(dragon, projectile)
                || canFight(dragon, target)) {
            brain.setActiveActivityIfPossible(Activity.FIGHT);
        } else {
            brain.useDefaultActivity();
        }
    }

    @Override
    public List<DragonBehaviourGroup<Nulljaw>> getDragonBrainBehaviourGroups() {
        return List.of(
                DragonBehaviourGroup.<Nulljaw>activity(Activity.CORE)
                        .behaviours(
                                new NulljawShulkerBulletSensorBehaviour(),
                                new NulljawTargetingBehaviour(),
                                new DragonIdleLookBehaviour<>(12.0D),
                                new ApplyMovementIntentBehaviour<>()
                        )
                        .build(),
                DragonBehaviourGroup.<Nulljaw>activity(Activity.FIGHT)
                        .behaviours(new NulljawTacticalCombatBehaviour())
                        .clearWhenStopped(DragonMemories.MOVEMENT_INTENT)
                        .build(),
                DragonBehaviourGroup.<Nulljaw>activity(Activity.IDLE)
                        .behaviours(
                                new FirstApplicableDragonBehaviour<>(
                                        new NulljawFollowParentBehaviour(),
                                        new NulljawFollowOwnerBehaviour(),
                                        new NulljawTemptBehaviour(),
                                        new NulljawBreedBehaviour(
                                                1.0D,
                                                Nulljaw.BREED_PARTNER_RANGE,
                                                Nulljaw.BREED_DISTANCE_SQR
                                        ),
                                        new NulljawPackFollowBehaviour(),
                                        new NulljawFloatWanderBehaviour()
                                )
                        )
                        .clearWhenStopped(DragonMemories.MOVEMENT_INTENT)
                        .build()
        );
    }

    private boolean canFight(Nulljaw dragon, LivingEntity target) {
        if (target == null
                || !target.isAlive()
                || target.level() != dragon.level()
                || !target.attackable()
                || dragon.isAlly(target)
                || dragon.isBaby()
                || dragon.isVehicle()
                || dragon.isPassenger()
                || dragon.isOrderedToSit()
                || target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        double range = Math.max(16.0D, dragon.getAttributeValue(Attributes.FOLLOW_RANGE));
        return dragon.distanceToSqr(target) <= range * range;
    }
}
