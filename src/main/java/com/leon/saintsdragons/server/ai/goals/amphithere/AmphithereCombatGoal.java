package com.leon.saintsdragons.server.ai.goals.amphithere;

import com.leon.saintsdragons.common.registry.amphithere.AmphithereAbilities;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumSet;

/**
 * Combat goal for Amphithere - uses bite attack and FireBody ability when in combat
 */
public class AmphithereCombatGoal extends Goal {
    private final AmphithereEntity amphithere;
    private final double attackRange = 4.5; // Amphithere has longer neck, slightly more range
    private final double fireBodyActivationRange = 8.0; // Activate FireBody when enemy is within this range
    private final double chaseSpeed = 1.2D;
    private int attackCooldown = 0;
    private int fireBodyCheckCooldown = 0;

    public AmphithereCombatGoal(AmphithereEntity amphithere) {
        this.amphithere = amphithere;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = amphithere.getTarget();

        if (target == null || !target.isAlive()) {
            return false;
        }

        if (amphithere.isVehicle() || amphithere.isOrderedToSit()) {
            return false;
        }

        if (amphithere.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = amphithere.getTarget();

        if (target == null || !target.isAlive()) {
            return false;
        }

        if (amphithere.isVehicle() || amphithere.isOrderedToSit()) {
            return false;
        }

        if (amphithere.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        amphithere.getNavigation().stop();
        deactivateFireBodyIfActive();
    }

    @Override
    public void start() {
        LivingEntity target = amphithere.getTarget();
        if (target != null) {
            amphithere.getLookControl().setLookAt(target, 30.0F, 30.0F);
            amphithere.getNavigation().moveTo(target, chaseSpeed);

            double distanceSq = amphithere.distanceToSqr(target);
            if (distanceSq <= getAttackReachSqr(target)) {
                tryPerformBite(target);
            }
        }
    }

    @Override
    public void tick() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (fireBodyCheckCooldown > 0) {
            fireBodyCheckCooldown--;
        }

        LivingEntity target = amphithere.getTarget();
        if (target != null) {
            amphithere.getLookControl().setLookAt(target, 30.0F, 30.0F);

            double distanceSq = amphithere.distanceToSqr(target);
            double attackReachSq = getAttackReachSqr(target);
            boolean inAttackRange = distanceSq <= attackReachSq;
            boolean hasLineOfSight = amphithere.getSensing().hasLineOfSight(target);

            if (!inAttackRange || !hasLineOfSight) {
                if (!isCurrentlyBiting()) {
                    amphithere.getNavigation().moveTo(target, chaseSpeed);
                }
            } else {
                amphithere.getNavigation().stop();
                tryPerformBite(target);
            }

            handleFireBodyActivation(target);
        } else {
            deactivateFireBodyIfActive();
        }
    }

    private boolean isCurrentlyBiting() {
        return amphithere.isAbilityActive(AmphithereAbilities.BITE);
    }

    private void tryPerformBite(LivingEntity target) {
        if (attackCooldown > 0 || isCurrentlyBiting()) {
            return;
        }

        if (!amphithere.getSensing().hasLineOfSight(target)) {
            return;
        }

        amphithere.combatManager.tryUseAbility(AmphithereAbilities.BITE);
        attackCooldown = 40; // 2 second cooldown
    }

    /**
     * Activates FireBody when enemies are nearby to create a defensive/offensive aura
     */
    private void handleFireBodyActivation(LivingEntity target) {
        // Only check every 2 seconds to avoid spam
        if (fireBodyCheckCooldown > 0) {
            return;
        }

        // Don't activate if being ridden (let rider control it)
        if (amphithere.isVehicle()) {
            return;
        }

        // Don't activate if in water (FireBody doesn't work in water)
        if (amphithere.isInWaterOrBubble()) {
            return;
        }

        boolean fireBodyActive = amphithere.isAbilityActive(AmphithereAbilities.FIRE_BODY);
        double distanceToTarget = amphithere.distanceTo(target);

        if (!fireBodyActive && distanceToTarget < fireBodyActivationRange) {
            amphithere.combatManager.tryUseAbility(AmphithereAbilities.FIRE_BODY);
            fireBodyCheckCooldown = 40; // 2 second cooldown before checking again
        } else if (fireBodyActive && distanceToTarget > fireBodyActivationRange * 1.5) {
            amphithere.forceEndAbility(AmphithereAbilities.FIRE_BODY);
            fireBodyCheckCooldown = 40;
        }
    }


    /**
     * Deactivates FireBody when there's no target (enemy killed or lost)
     */
    private void deactivateFireBodyIfActive() {
        // Don't interfere if being ridden
        if (amphithere.isVehicle()) {
            return;
        }

        if (amphithere.isAbilityActive(AmphithereAbilities.FIRE_BODY)) {
            amphithere.forceEndAbility(AmphithereAbilities.FIRE_BODY);
        }
    }


    private double getAttackReachSqr(LivingEntity target) {
        double combinedRadii = (this.amphithere.getBbWidth() + target.getBbWidth()) * 0.5;
        double reach = this.attackRange + combinedRadii;
        return reach * reach;
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = this.amphithere.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 16.0D;
        }
        return followRange * followRange;
    }
}
