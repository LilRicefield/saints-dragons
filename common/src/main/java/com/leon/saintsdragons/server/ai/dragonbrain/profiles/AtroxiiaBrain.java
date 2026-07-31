package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.registry.ModSensorTypes;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainOwner;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ApplyMovementIntentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AsyncWaterChaseTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundFollowOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonBreedBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonIdleLookBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonWaterEscapeBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.FirstApplicableDragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.LookAtAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.MoveToGroundWalkTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.SetWalkTargetToAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.atroxiia.AtroxiiaGroundCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.atroxiia.AtroxiiaTargetingBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.atroxiia.AtroxiiaWaterCombatBehaviour;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public final class AtroxiiaBrain implements DragonBrainOwner<Atroxiia> {
    private static final double IDLE_WANDER_SPEED = 0.80D;

    @Override
    public List<SensorType<? extends Sensor<? super Atroxiia>>> getDragonBrainSensors() {
        return List.of(ModSensorTypes.DRAGON_MOVEMENT_STATE.get());
    }

    @Override
    public void updateActivity(Brain<Atroxiia> brain, Atroxiia dragon) {
        LivingEntity target = brain.getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        if (canFight(dragon, target)) {
            brain.setActiveActivityIfPossible(Activity.FIGHT);
            return;
        }

        if (target != null && (!dragon.isTargetValid(target) || !withinAggroRange(dragon, target))) {
            dragon.setTarget(null);
            brain.eraseMemory(DragonMemories.ATTACK_TARGET);
            brain.eraseMemory(DragonMemories.TARGET_VISIBLE);
            brain.eraseMemory(DragonMemories.LAST_SEEN_TARGET);
            brain.eraseMemory(DragonMemories.INVESTIGATION_TARGET);
            brain.eraseMemory(DragonMemories.HEARD_TARGET);
        }
        brain.useDefaultActivity();
    }

    @Override
    public List<DragonBehaviourGroup<Atroxiia>> getDragonBrainBehaviourGroups() {
        return List.of(
                DragonBehaviourGroup.<Atroxiia>activity(Activity.CORE)
                        .behaviours(
                                new AtroxiiaTargetingBehaviour(),
                                new DragonIdleLookBehaviour<>(8.0D),
                                new ApplyMovementIntentBehaviour<>(),
                                new MoveToGroundWalkTargetBehaviour<>(),
                                new LookAtAttackTargetBehaviour<>(30.0F, 30.0F)
                        )
                        .build(),
                DragonBehaviourGroup.<Atroxiia>activity(Activity.FIGHT)
                        .behaviours(
                                new SetWalkTargetToAttackTargetBehaviour<Atroxiia>(
                                        AtroxiiaGroundCombatBehaviour.CHASE_SPEED,
                                        AtroxiiaGroundCombatBehaviour::meleeStopRange,
                                        (dragon, target) -> AtroxiiaGroundCombatBehaviour.isMovementCommitted(dragon)
                                ),
                                new AsyncWaterChaseTargetBehaviour<Atroxiia>(0.30D, 8.0F),
                                new AtroxiiaGroundCombatBehaviour(),
                                new AtroxiiaWaterCombatBehaviour()
                        )
                        .clearWhenStopped(
                                DragonMemories.MOVEMENT_INTENT,
                                DragonMemories.WALK_TARGET,
                                DragonMemories.PATH,
                                DragonMemories.CANT_REACH_WALK_TARGET_SINCE
                        )
                        .build(),
                DragonBehaviourGroup.<Atroxiia>activity(Activity.IDLE)
                        .behaviours(
                                new FirstApplicableDragonBehaviour<>(
                                        new DragonWaterEscapeBehaviour<>(8.0F, 0.12D),
                                        new DragonBreedBehaviour<>(1.0D, Atroxiia.class,
                                                Atroxiia.BREED_PARTNER_RANGE, Atroxiia.BREED_DISTANCE_SQR),
                                        new DragonGroundFollowOwnerBehaviour<>(
                                                DragonGroundFollowOwnerBehaviour.Config.atroxiia()),
                                        new DragonGroundWanderBehaviour<>(IDLE_WANDER_SPEED, 100)
                                )
                        )
                        .clearWhenStopped(DragonMemories.MOVEMENT_INTENT)
                        .build()
        );
    }

    private boolean canFight(Atroxiia dragon, LivingEntity target) {
        return target != null
                && dragon.isTargetValid(target)
                && dragon.canTarget(target)
                && !dragon.isBaby()
                && !dragon.isTamingStunned()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && (dragon.isGroundedForAction() || dragon.isInWaterOrBubble())
                && withinAggroRange(dragon, target);
    }

    private boolean withinAggroRange(Atroxiia dragon, LivingEntity target) {
        double followRange = Math.max(16.0D, dragon.getAttributeValue(Attributes.FOLLOW_RANGE));
        return dragon.distanceToSqr(target) <= followRange * followRange;
    }
}
