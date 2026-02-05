package com.leon.saintsdragons.server.ai.goals.stegonaut;

import com.leon.saintsdragons.common.registry.stegonaut.StegonautAbilities;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Ground combat goal for Stegonaut.
 * Uses currently selected melee mode (bite/chin slam).
 */
public class StegonautCombatGoal extends Goal {
    private final Stegonaut dragon;
    private final double attackRange = 2.9D;
    private final double chaseSpeed = 0.75D;
    private int attackCooldown = 0;
    private int pathRecalcCooldown = 0;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;

    public StegonautCombatGoal(Stegonaut dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = dragon.getTarget();
        if (!dragon.isTargetValid(target)) return false;
        if (!dragon.canTarget(target)) return false;
        if (dragon.isVehicle() || dragon.isOrderedToSit()) return false;
        return dragon.distanceToSqr(target) <= getMaxAggroDistanceSqr();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = dragon.getTarget();
        if (!dragon.isTargetValid(target)) return false;
        if (!dragon.canTarget(target)) return false;
        if (dragon.isVehicle() || dragon.isOrderedToSit()) return false;
        return dragon.distanceToSqr(target) <= getMaxAggroDistanceSqr();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        LivingEntity target = dragon.getTarget();
        if (target != null) {
            dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
            dragon.getNavigation().moveTo(target, chaseSpeed);
            rememberTargetPosition(target);
        }
    }

    @Override
    public void stop() {
        dragon.getNavigation().stop();
        pathRecalcCooldown = 0;
    }

    @Override
    public void tick() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        LivingEntity target = dragon.getTarget();
        if (target == null) {
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distanceSq = dragon.distanceToSqr(target);
        boolean inAttackRange = distanceSq <= getAttackReachSqr(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        if (!inAttackRange || !hasLineOfSight) {
            if (!isCurrentlyAttacking()) {
                updateChasePath(target);
            }
            return;
        }

        // Keep closing distance while waiting for cooldown so we don't stall out at edge range.
        if (attackCooldown > 0) {
            updateChasePath(target);
            return;
        }

        dragon.getNavigation().stop();
        pathRecalcCooldown = 0;
        tryPerformMelee(target);
    }

    private void tryPerformMelee(LivingEntity target) {
        if (attackCooldown > 0 || isCurrentlyAttacking()) {
            return;
        }
        if (!dragon.getSensing().hasLineOfSight(target)) {
            return;
        }

        dragon.combatManager.tryUseAbility(dragon.getPrimaryAttackAbility());
        attackCooldown = 26;
    }

    private boolean isCurrentlyAttacking() {
        return dragon.combatManager.isAbilityActive(StegonautAbilities.STEGONAUT_BITE)
                || dragon.combatManager.isAbilityActive(StegonautAbilities.STEGONAUT_CHIN_SLAM);
    }

    private double getAttackReachSqr(LivingEntity target) {
        double combinedRadii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        double reach = attackRange + combinedRadii;
        return reach * reach;
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 16.0D;
        }
        return followRange * followRange;
    }

    private void updateChasePath(LivingEntity target) {
        if (--pathRecalcCooldown <= 0 || targetMovedSignificantly(target)) {
            rememberTargetPosition(target);
            double distance = dragon.distanceTo(target);
            pathRecalcCooldown = Mth.clamp((int) (distance * 0.6D), 5, 20);
            dragon.getNavigation().moveTo(target, chaseSpeed);
        }
    }

    private void rememberTargetPosition(LivingEntity target) {
        this.lastTargetX = target.getX();
        this.lastTargetY = target.getY();
        this.lastTargetZ = target.getZ();
    }

    private boolean targetMovedSignificantly(LivingEntity target) {
        double dx = target.getX() - this.lastTargetX;
        double dy = target.getY() - this.lastTargetY;
        double dz = target.getZ() - this.lastTargetZ;
        return dx * dx + dy * dy + dz * dz > 4.0D;
    }
}
