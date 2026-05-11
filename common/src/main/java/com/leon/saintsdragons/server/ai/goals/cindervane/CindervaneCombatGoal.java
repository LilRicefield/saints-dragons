package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.common.registry.cindervane.CindervaneAbilities;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;


public class CindervaneCombatGoal extends Goal {
    private final Cindervane amphithere;
    private final double attackRange = 4.5; // Amphithere has longer neck, slightly more range
    private final double fireBodyActivationRange = 8.0; // Activate FireBody when enemy is within this range
    private final double chaseSpeed = 1.0D;
    private int attackCooldown = 0;
    private int fireBodyCheckCooldown = 0;
    private int pathRecalcCooldown = 0;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;

    public CindervaneCombatGoal(Cindervane amphithere) {
        this.amphithere = amphithere;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = amphithere.getTarget();

        if (!amphithere.isTargetValid(target)) {
            return false;
        }

        if (amphithere.isVehicle() || amphithere.isOrderedToSit()) {
            return false;
        }

        if (amphithere.isFlying() || amphithere.isHovering() || amphithere.isTakeoff() || amphithere.isLanding()) {
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

        if (!amphithere.isTargetValid(target)) {
            return false;
        }

        if (amphithere.isVehicle() || amphithere.isOrderedToSit()) {
            return false;
        }

        if (amphithere.isFlying() || amphithere.isHovering() || amphithere.isTakeoff() || amphithere.isLanding()) {
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
        attackCooldown = 0;
        pathRecalcCooldown = 0;
    }

    @Override
    public void start() {
        LivingEntity target = amphithere.getTarget();
        if (target != null) {
            amphithere.getLookControl().setLookAt(target, 30.0F, 30.0F);
            amphithere.getNavigation().moveTo(target, chaseSpeed);
            rememberTargetPosition(target);

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

            if (amphithere.isInWaterOrBubble()) {
                handleWaterCombat(target);
                return;
            }

            double distanceSq = amphithere.distanceToSqr(target);
            double attackReachSq = getAttackReachSqr(target);
            boolean inAttackRange = distanceSq <= attackReachSq;
            boolean hasLineOfSight = amphithere.getSensing().hasLineOfSight(target);

            if (!inAttackRange || !hasLineOfSight) {
                if (!isCurrentlyBiting()) {
                    updateChasePath(target);
                }
            } else {
                amphithere.getNavigation().stop();
                pathRecalcCooldown = 0;
                tryPerformBite(target);
            }

            handleFireBodyActivation(target);
        } else {
            deactivateFireBodyIfActive();
        }
    }

    private boolean isCurrentlyBiting() {
        return amphithere.isAbilityActive(CindervaneAbilities.BITE);
    }

    private void tryPerformBite(LivingEntity target) {
        if (attackCooldown > 0 || amphithere.getAiCombatPacing().getCadenceCooldownTicks() > 0 || isCurrentlyBiting()) {
            return;
        }

        if (!amphithere.getSensing().hasLineOfSight(target)) {
            return;
        }

        if (!amphithere.combatManager.canStart(CindervaneAbilities.BITE)
                || !amphithere.getAiCombatPacing().canUse(CindervaneAbilities.BITE, false)) {
            return;
        }

        amphithere.combatManager.tryUseAbility(CindervaneAbilities.BITE);
        amphithere.getAiCombatPacing().recordUse(CindervaneAbilities.BITE, 40, 40, false, 0, 28);
        attackCooldown = 40;
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

        boolean fireBodyActive = amphithere.isAbilityActive(CindervaneAbilities.FIRE_BODY);
        double distanceToTarget = amphithere.distanceTo(target);

        if (!fireBodyActive && distanceToTarget < fireBodyActivationRange) {
            amphithere.combatManager.tryUseAbility(CindervaneAbilities.FIRE_BODY);
            fireBodyCheckCooldown = 40; // 2 second cooldown before checking again
        } else if (fireBodyActive && distanceToTarget > fireBodyActivationRange * 1.5) {
            amphithere.forceEndAbility(CindervaneAbilities.FIRE_BODY);
            fireBodyCheckCooldown = 40;
        }
    }

    private void deactivateFireBodyIfActive() {
        // Don't interfere if being ridden
        if (amphithere.isVehicle()) {
            return;
        }

        if (amphithere.isAbilityActive(CindervaneAbilities.FIRE_BODY)) {
            amphithere.forceEndAbility(CindervaneAbilities.FIRE_BODY);
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

    private void updateChasePath(LivingEntity target) {
        if (amphithere.isInWaterOrBubble()) {
            return;
        }
        if (--pathRecalcCooldown <= 0 || targetMovedSignificantly(target)) {
            rememberTargetPosition(target);
            double distance = amphithere.distanceTo(target);
            pathRecalcCooldown = Mth.clamp((int) (distance * 0.6D), 5, 20);
            amphithere.getNavigation().moveTo(target, chaseSpeed);
        }
    }

    private void handleWaterCombat(LivingEntity target) {
        amphithere.getNavigation().stop();
        deactivateFireBodyIfActive();

        double distanceSq = amphithere.distanceToSqr(target);
        boolean inAttackRange = distanceSq <= getAttackReachSqr(target);
        boolean hasLineOfSight = amphithere.getSensing().hasLineOfSight(target);

        Vec3 current = amphithere.getDeltaMovement();
        Vec3 toTarget = target.position().subtract(amphithere.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        Vec3 desiredHorizontal = horizontal.lengthSqr() > 1.0E-4
                ? horizontal.normalize().scale(0.22D)
                : Vec3.ZERO;

        double nx = current.x + (desiredHorizontal.x - current.x) * 0.30D;
        double nz = current.z + (desiredHorizontal.z - current.z) * 0.30D;

        double targetY = target.getY() + target.getBbHeight() * 0.45D;
        double yDiff = targetY - amphithere.getY();
        double ny = current.y;
        if (yDiff > 0.9D) {
            ny = Math.max(current.y + 0.035D, 0.06D);
        } else if (yDiff < -1.3D) {
            ny = Math.min(current.y - 0.03D, -0.08D);
        } else {
            ny = current.y + 0.012D;
        }

        amphithere.setDeltaMovement(nx, ny, nz);
        amphithere.getMoveControl().setWantedPosition(target.getX(), targetY, target.getZ(), 1.0D);

        if (inAttackRange && hasLineOfSight) {
            tryPerformBite(target);
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
