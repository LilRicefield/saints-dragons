package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.registry.ModSensorTypes;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainOwner;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ApplyMovementIntentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AsyncWaterChaseTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.LookAtAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.MoveToGroundWalkTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ReturnToRoostBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.SetWalkTargetToAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.varasuchus.VarasuchusCombatBehaviour;
import com.leon.saintsdragons.server.ai.goals.base.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public class VarasuchusCombatBrain implements DragonBrainOwner<Varasuchus> {
    private static final int RECENT_ATTACKER_PRIORITY_TICKS = 20 * 30;

    @Override
    public List<SensorType<? extends Sensor<? super Varasuchus>>> getDragonBrainSensors() {
        return List.of(
                ModSensorTypes.DRAGON_TARGET.get(),
                ModSensorTypes.DRAGON_MOVEMENT_STATE.get()
        );
    }

    @Override
    public void updateActivity(Brain<Varasuchus> brain, Varasuchus dragon) {
        LivingEntity target = dragon.getTarget();
        if (target != null
                && dragon.hasRoostTerritory()
                && !dragon.isWithinRoostTerritory(target.position())) {
            dragon.setTarget(null);
            target = null;
        }

        boolean wantsSleep = wantsRoostSleep(dragon);
        boolean defendingAgainstRecentAttacker = wantsSleep && isRecentAttacker(dragon, target);
        if (target != null && wantsSleep && !defendingAgainstRecentAttacker) {
            dragon.setTarget(null);
            target = null;
        }

        if (dragon.isOutsideRoostTerritory()) {
            brain.useDefaultActivity();
            return;
        }

        if ((defendingAgainstRecentAttacker || !wantsSleep) && canFight(dragon)) {
            brain.setActiveActivityIfPossible(Activity.FIGHT);
        } else {
            brain.useDefaultActivity();
        }
    }

    @Override
    public boolean shouldTakeControl(Varasuchus dragon) {
        if (dragon.isOutsideRoostTerritory()) {
            return true;
        }
        if (wantsRoostSleep(dragon) && !isRecentAttacker(dragon, dragon.getTarget())) {
            return true;
        }
        if (canFight(dragon)) {
            return true;
        }
        if (dragon.getTarget() != null && dragon.getTarget().isAlive()) {
            return false;
        }
        return DragonBrainOwner.super.shouldTakeControl(dragon);
    }

    @Override
    public List<DragonBehaviourGroup<Varasuchus>> getDragonBrainBehaviourGroups() {
        VarasuchusCombatBehaviour combat = new VarasuchusCombatBehaviour();
        return List.of(
                DragonBehaviourGroup.<Varasuchus>activity(Activity.CORE)
                        .behaviours(
                                new ApplyMovementIntentBehaviour<>(),
                                new MoveToGroundWalkTargetBehaviour<>(),
                                new LookAtAttackTargetBehaviour<>(30.0F, 30.0F)
                        )
                        .build(),
                DragonBehaviourGroup.<Varasuchus>activity(Activity.FIGHT)
                        .behaviours(
                                new SetWalkTargetToAttackTargetBehaviour<Varasuchus>(
                                        VarasuchusCombatBehaviour.CHASE_SPEED,
                                        (dragon, target) ->
                                                groundStopRange(dragon, target)
                                                        + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D,
                                        (dragon, target) -> combat.isMovementLocked()
                                ),
                                new AsyncWaterChaseTargetBehaviour<Varasuchus>(
                                        (dragon, target) -> 0.30D,
                                        8.0F,
                                        (dragon, target) -> combat.isMovementLocked()
                                ),
                                combat
                        )
                        .clearWhenStopped(
                                DragonMemories.MOVEMENT_INTENT,
                                DragonMemories.WALK_TARGET,
                                DragonMemories.PATH,
                                DragonMemories.CANT_REACH_WALK_TARGET_SINCE
                        )
                        .build(),
                DragonBehaviourGroup.<Varasuchus>activity(Activity.IDLE)
                        .behaviours(new ReturnToRoostBehaviour<>(
                                Varasuchus.ROOST_SLEEP_RADIUS,
                                Varasuchus.ROOST_TERRITORY_RADIUS,
                                Varasuchus.ROOST_TERRITORY_RETURN_RADIUS,
                                1.0F,
                                0.25D,
                                8.0F
                        ))
                        .build()
        );
    }

    private boolean canFight(Varasuchus dragon) {
        LivingEntity target = dragon.getTarget();
        if (target == null
                || !dragon.isTargetValid(target)
                || dragon.isBaby()
                || dragon.isVehicle()
                || dragon.isOrderedToSit()
                || (dragon.hasRoostTerritory()
                        && !dragon.isWithinRoostTerritory(target.position()))) {
            return false;
        }
        double followRange = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 16.0D;
        }
        return dragon.distanceToSqr(target) <= followRange * followRange;
    }

    private boolean wantsRoostSleep(Varasuchus dragon) {
        return dragon.hasRoostTerritory()
                && dragon.supportsSleep()
                && dragon.getSleepPreferences().canSleepDuringConditions(dragon.level())
                && dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleeping()
                && !dragon.isSleepTransitioning();
    }

    private boolean isRecentAttacker(Varasuchus dragon, LivingEntity target) {
        if (target == null || !target.isAlive() || !dragon.isTargetValid(target)) {
            return false;
        }

        int attackTick;
        if (dragon.getLastDamager() == target) {
            attackTick = dragon.getLastDamagerTimestamp();
        } else if (dragon.getLastHurtByMob() == target) {
            attackTick = dragon.getLastHurtByMobTimestamp();
        } else {
            return false;
        }
        int ticksSinceAttack = dragon.tickCount - attackTick;
        return attackTick > 0
                && ticksSinceAttack >= 0
                && ticksSinceAttack < RECENT_ATTACKER_PRIORITY_TICKS;
    }

    private static double groundStopRange(Varasuchus dragon, LivingEntity target) {
        return DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)
                ? VarasuchusCombatBehaviour.LAND_PREY_BITE_RANGE
                : VarasuchusCombatBehaviour.BITE_RANGE;
    }
}
