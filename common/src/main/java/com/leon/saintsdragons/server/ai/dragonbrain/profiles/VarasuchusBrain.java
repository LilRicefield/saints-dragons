package com.leon.saintsdragons.server.ai.dragonbrain.profiles;

import com.leon.saintsdragons.common.config.dragon.profile.VarasuchusStatProfile;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourGroup;
import com.leon.saintsdragons.server.ai.dragonbrain.GroundDragonBrain;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonTargetLifecycle;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AsyncWaterChaseTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonBreedBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFindWaterBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFollowParentBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundFollowOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonGroundWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonSwimFollowBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonSwimWanderBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonWaterEscapeBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ReturnToRoostBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.SetWalkTargetToAttackTargetBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.varasuchus.VarasuchusCombatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.varasuchus.VarasuchusTargetingBehaviour;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.Brain;

import java.util.List;

public class VarasuchusBrain extends GroundDragonBrain<Varasuchus> {

    @Override
    public boolean usesDragonScent() {
        return true;
    }

    @Override
    protected boolean selectPriorityActivity(Brain<Varasuchus> brain, Varasuchus dragon) {
        LivingEntity target = brain.getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target != null && !withinAggroRange(dragon, target)) {
            DragonTargetLifecycle.clearCombatTarget(brain, dragon, true);
            target = null;
        }

        boolean wantsSleep = wantsRoostSleep(dragon);
        boolean defendingAgainstRecentAttacker = wantsSleep && isRecentAttacker(dragon, target);
        boolean defendingRoostIntruder = wantsSleep
                && dragon.isWildAggressionEnabled()
                && target instanceof Player player
                && !player.isCreative()
                && !player.isSpectator()
                && dragon.isInsideRoostStructure(player.position());
        if (target != null
                && wantsSleep
                && !defendingAgainstRecentAttacker
                && !defendingRoostIntruder) {
            DragonTargetLifecycle.clearCombatTarget(brain, dragon, true);
        }

        if (dragon.isOutsideRoostTerritory()
                || wantsSleep && !defendingAgainstRecentAttacker && !defendingRoostIntruder) {
            brain.useDefaultActivity();
            return true;
        }
        return false;
    }

    @Override
    protected List<DragonBehaviourGroup<Varasuchus>> createBehaviourGroups() {
        VarasuchusCombatBehaviour combat = new VarasuchusCombatBehaviour();
        return List.of(
                huntingCoreGroup(30.0F, new VarasuchusTargetingBehaviour()),
                fightGroup(
                    new SetWalkTargetToAttackTargetBehaviour<>(
                            VarasuchusCombatBehaviour.CHASE_SPEED,
                            (dragon, target) ->
                                    groundStopRange(dragon, target)
                                            + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D,
                            (dragon, target) -> combat.isMovementLocked()
                    ),
                    new AsyncWaterChaseTargetBehaviour<>(
                            (dragon, target) -> VarasuchusStatProfile.Brain.WATER_CHASE_SPEED,
                            VarasuchusStatProfile.Brain.WATER_CHASE_TURN_DEGREES,
                            (dragon, target) -> combat.isMovementLocked()
                    ),
                    combat
                ),
                idleGroup(
                    new DragonBreedBehaviour<>(
                            VarasuchusStatProfile.Brain.BREED_SPEED,
                            Varasuchus.class,
                            Varasuchus.BREED_PARTNER_RANGE,
                            Varasuchus.BREED_DISTANCE_SQR
                    ),
                    new ReturnToRoostBehaviour<>(
                            Varasuchus.ROOST_SLEEP_RADIUS,
                            Varasuchus.ROOST_TERRITORY_RADIUS,
                            Varasuchus.ROOST_TERRITORY_RETURN_RADIUS,
                            VarasuchusStatProfile.Brain.ROOST_RETURN_GROUND_SPEED,
                            VarasuchusStatProfile.Brain.ROOST_RETURN_SWIM_SPEED,
                            VarasuchusStatProfile.Brain.ROOST_RETURN_SWIM_TURN_DEGREES
                    ),
                    new DragonWaterEscapeBehaviour<>(
                            VarasuchusStatProfile.Brain.WATER_ESCAPE_TURN_DEGREES,
                            VarasuchusStatProfile.Brain.WATER_ESCAPE_SPEED,
                            Varasuchus::shouldLeaveWater,
                            VarasuchusBrain::canContinueLeavingWater
                    ),
                    new DragonFindWaterBehaviour<>(VarasuchusStatProfile.Brain.FIND_WATER_SPEED),
                    new DragonGroundFollowOwnerBehaviour<>(
                            DragonGroundFollowOwnerBehaviour.Config.standardAdult()),
                    new DragonSwimFollowBehaviour<>(
                            Varasuchus.class, VarasuchusStatProfile.Brain.SWIM_FOLLOW_TURN_DEGREES, VarasuchusStatProfile.Brain.SWIM_FOLLOW_SPEED, 20.0D, 16.0D),
                    new DragonSwimWanderBehaviour<>(
                            VarasuchusStatProfile.Brain.SWIM_WANDER_TURN_DEGREES,
                            VarasuchusStatProfile.Brain.SWIM_WANDER_SPEED,
                            30,
                            dragon -> !dragon.shouldSuspendRoostWandering(),
                            Varasuchus::isWithinRoostTerritory
                    ),
                    new DragonFollowParentBehaviour<>(Varasuchus.class, VarasuchusStatProfile.Brain.FOLLOW_PARENT_SPEED),
                    new DragonGroundWanderBehaviour<>(
                            VarasuchusStatProfile.Brain.GROUND_WANDER_SPEED,
                            100,
                            10,
                            dragon -> !dragon.isInWaterOrBubble()
                                    && !dragon.shouldSuspendRoostWandering(),
                            Varasuchus::isWithinRoostTerritory
                    )
                )
        );
    }

    @Override
    protected boolean canFight(Varasuchus dragon, LivingEntity target) {
        return super.canFight(dragon, target) && !dragon.isBaby();
    }

    @Override
    protected boolean withinAggroRange(Varasuchus dragon, LivingEntity target) {
        // Roost residents pursue within their territory rather than a follow-range sphere.
        return !dragon.hasRoostTerritory() || dragon.isWithinRoostTerritory(target.position());
    }

    private boolean wantsRoostSleep(Varasuchus dragon) {
        return dragon.hasRoostTerritory()
                && dragon.wantsToSleep()
                && dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleeping()
                && !dragon.isSleepTransitioning();
    }

    private boolean isRecentAttacker(Varasuchus dragon, LivingEntity target) {
        if (!DragonTargetLifecycle.isValidTarget(dragon, target)) {
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
                && ticksSinceAttack < Varasuchus.RECENT_ATTACKER_PRIORITY_TICKS;
    }

    private static double groundStopRange(Varasuchus dragon, LivingEntity target) {
        return DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)
                ? VarasuchusCombatBehaviour.LAND_PREY_BITE_RANGE
                : VarasuchusCombatBehaviour.BITE_RANGE;
    }

    private static boolean canContinueLeavingWater(Varasuchus dragon) {
        if (!dragon.canSwim() || dragon.isOrderedToSit()) {
            return false;
        }
        LivingEntity owner = dragon.getOwner();
        return !dragon.isTame()
                || dragon.getCommand() != 0
                || owner == null
                || !owner.isAlive()
                || owner.level() != dragon.level()
                || !owner.isInWaterOrBubble();
    }

}
