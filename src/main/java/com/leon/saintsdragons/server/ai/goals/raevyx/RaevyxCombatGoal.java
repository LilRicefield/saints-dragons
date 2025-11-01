package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.common.registry.raevyx.RaevyxAbilities;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumSet;

/**
 * All-in-one combat goal for Raevyx - handles movement, attack selection, and execution.
 * No state machine, no windup phases - just instant attacks when in range.
 */
public class RaevyxCombatGoal extends Goal {
    private final Raevyx wyvern;
    private final double biteRange = 3.0;
    private final double goreRange = 4.5;
    private final double chaseSpeed = 1.40D;
    private int attackCooldown = 0;
    private int pathRecalcCooldown = 0;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;

    public RaevyxCombatGoal(Raevyx wyvern) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = wyvern.getTarget();

        if (target == null || !target.isAlive()) {
            return false;
        }

        if (wyvern.isVehicle() || wyvern.isOrderedToSit()) {
            return false;
        }

        if (wyvern.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = wyvern.getTarget();

        if (target == null || !target.isAlive()) {
            return false;
        }

        if (wyvern.isVehicle() || wyvern.isOrderedToSit()) {
            return false;
        }

        if (wyvern.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
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
        wyvern.getNavigation().stop();
        wyvern.setRunning(false);
        wyvern.setAggressive(false);
        pathRecalcCooldown = 0;
    }

    @Override
    public void start() {
        wyvern.setRunning(true);
        wyvern.setAggressive(true);

        LivingEntity target = wyvern.getTarget();
        if (target != null) {
            wyvern.getLookControl().setLookAt(target, 30.0F, 30.0F);
            wyvern.getNavigation().moveTo(target, chaseSpeed);
            rememberTargetPosition(target);

            // Try attacking immediately if in range
            tryAttack(target);
        }
    }

    @Override
    public void tick() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        LivingEntity target = wyvern.getTarget();
        if (target != null) {
            wyvern.getLookControl().setLookAt(target, 30.0F, 30.0F);

            double gap = getGapToTarget(target);
            boolean hasLineOfSight = wyvern.getSensing().hasLineOfSight(target);

            // If not in attack range or no LOS, chase
            if (gap > goreRange || !hasLineOfSight) {
                if (!isCurrentlyAttacking()) {
                    updateChasePath(target);
                }
            } else {
                // In range - stop moving and attack
                wyvern.getNavigation().stop();
                pathRecalcCooldown = 0;
                tryAttack(target);
            }
        }
    }

    /**
     * Check if wyvern is currently executing an attack ability
     */
    private boolean isCurrentlyAttacking() {
        return wyvern.isAbilityActive(RaevyxAbilities.RAEVYX_BITE)
            || wyvern.isAbilityActive(RaevyxAbilities.RAEVYX_HORN_GORE);
    }

    /**
     * Try to attack target based on distance. Instant execution, no windup.
     */
    private void tryAttack(LivingEntity target) {
        if (attackCooldown > 0 || isCurrentlyAttacking()) {
            return;
        }

        if (!wyvern.getSensing().hasLineOfSight(target)) {
            return;
        }

        double gap = getGapToTarget(target);

        // Choose attack based on distance - fire immediately
        if (gap <= biteRange) {
            // Close range - bite attack
            wyvern.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_BITE);
            attackCooldown = 20;
        } else if (gap <= goreRange) {
            // Medium range - horn gore
            wyvern.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_HORN_GORE);
            attackCooldown = 20;
        }
    }

    /**
     * Get the gap between entity edges (not centers)
     */
    private double getGapToTarget(LivingEntity target) {
        double centerDistance = this.wyvern.distanceTo(target);
        double combinedRadii = (this.wyvern.getBbWidth() + target.getBbWidth()) * 0.5;
        return Math.max(0.0, centerDistance - combinedRadii);
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = this.wyvern.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 32.0D;
        }
        return followRange * followRange;
    }

    private void updateChasePath(LivingEntity target) {
        if (--pathRecalcCooldown <= 0 || targetMovedSignificantly(target)) {
            rememberTargetPosition(target);
            double distance = wyvern.distanceTo(target);
            pathRecalcCooldown = Mth.clamp((int) (distance * 0.6D), 5, 20);
            wyvern.getNavigation().moveTo(target, chaseSpeed);
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
