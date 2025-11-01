package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.common.registry.nulljaw.NulljawAbilities;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumSet;

/**
 * Ground combat coordinator for the Rift Drake.
 * Keeps the drake neutral until provoked, then selects the appropriate melee ability.
 */
public class NulljawCombatGoal extends Goal {
    private static final double CHASE_SPEED = 1.5D;
    private static final double BITE_RANGE = 3.5D;
    private static final double HORN_RANGE = 4.5D;
    private static final int MIN_ATTACK_COOLDOWN_TICKS = 10;

    private final Nulljaw drake;
    private int attackCooldown;

    public NulljawCombatGoal(Nulljaw drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = drake.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (drake.isVehicle() || drake.isOrderedToSit()) {
            return false;
        }
        return isWithinAggroRange(target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = drake.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (drake.isVehicle() || drake.isOrderedToSit()) {
            return false;
        }
        return isWithinAggroRange(target);
    }

    @Override
    public void start() {
        this.attackCooldown = 0;
        drake.setAggressive(true);

        LivingEntity target = drake.getTarget();
        if (target != null) {
            drake.getNavigation().moveTo(target, CHASE_SPEED);
        }
    }

    @Override
    public void stop() {
        drake.getNavigation().stop();
        drake.setAggressive(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        LivingEntity target = drake.getTarget();
        if (target == null) {
            return;
        }

        drake.getLookControl().setLookAt(target, 35.0F, 35.0F);

        double distanceSq = drake.distanceToSqr(target);
        double attackReachSq = getAttackReachSqr(target);
        boolean inRange = distanceSq <= attackReachSq;
        boolean hasLineOfSight = drake.getSensing().hasLineOfSight(target);

        if (!inRange || !hasLineOfSight) {
            if (!isPerformingAttack()) {
                drake.getNavigation().moveTo(target, CHASE_SPEED);
            }
            return;
        }

        drake.getNavigation().stop();
        tryPerformAttacks(target);
    }

    private void tryPerformAttacks(LivingEntity target) {
        if (attackCooldown > 0) {
            return;
        }

        DragonAbilityType<Nulljaw, ?> ability = choosePrimaryAttack(target);
        if (ability != null && drake.combatManager.canStart(ability)) {
            drake.combatManager.tryUseAbility(ability);
            attackCooldown = MIN_ATTACK_COOLDOWN_TICKS;
        }
    }

    private DragonAbilityType<Nulljaw, ?> choosePrimaryAttack(LivingEntity target) {
        double gap = getGapToTarget(target);
        boolean phaseTwo = drake.isPhaseTwoActive();

        if (gap <= BITE_RANGE) {
            // Phase 2: alternate between bite2 and claw
            if (phaseTwo && drake.getRandom().nextBoolean()) {
                return NulljawAbilities.NULLJAW_CLAW;
            }
            return phaseTwo ? NulljawAbilities.NULLJAW_BITE2 : NulljawAbilities.NULLJAW_BITE;
        }
        if (gap <= HORN_RANGE) {
            // Phase 2: alternate between horn gore and claw
            if (phaseTwo && drake.getRandom().nextBoolean()) {
                return NulljawAbilities.NULLJAW_CLAW;
            }
            return NulljawAbilities.NULLJAW_HORN_GORE;
        }
        return null;
    }

    private boolean isPerformingAttack() {
        return drake.getActiveAbility() != null
                || drake.combatManager.isAbilityActive(NulljawAbilities.NULLJAW_CLAW);
    }

    private boolean isWithinAggroRange(LivingEntity target) {
        double followRange = drake.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 16.0D;
        }
        double maxDistanceSq = followRange * followRange;
        return drake.distanceToSqr(target) <= maxDistanceSq;
    }

    private double getAttackReachSqr(LivingEntity target) {
        double combinedRadii = (drake.getBbWidth() + target.getBbWidth()) * 0.5;
        double reach = HORN_RANGE + combinedRadii;
        return reach * reach;
    }

    private double getGapToTarget(LivingEntity target) {
        double distance = drake.distanceTo(target);
        double combinedRadii = (drake.getBbWidth() + target.getBbWidth()) * 0.5;
        return Math.max(0.0D, distance - combinedRadii);
    }
}
