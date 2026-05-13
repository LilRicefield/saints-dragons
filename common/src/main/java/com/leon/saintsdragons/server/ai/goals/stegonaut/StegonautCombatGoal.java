package com.leon.saintsdragons.server.ai.goals.stegonaut;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;


public class StegonautCombatGoal extends Goal {
    private final Stegonaut dragon;
    private final double attackRangeGround = 3.4D;
    private final double attackRangeWater = 6.0D;
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
        attackCooldown = 0;
        pathRecalcCooldown = 0;
        dragon.setTarget(null);
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
        boolean inAttackRange = distanceSq <= getAttackReachSqr(target, dragon.isInWaterOrBubble());
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        if (dragon.isInWaterOrBubble()) {
            handleWaterCombatChase(target, inAttackRange, hasLineOfSight);
            return;
        }

        if (!inAttackRange || !hasLineOfSight) {
            if (!isCurrentlyAttacking()) {
                updateChasePath(target);
            }
            return;
        }

        if (attackCooldown > 0 || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0) {
            updateChasePath(target);
            return;
        }

        dragon.getNavigation().stop();
        pathRecalcCooldown = 0;
        tryPerformMelee(target);
    }

    private void tryPerformMelee(LivingEntity target) {
        if (attackCooldown > 0 || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0 || isCurrentlyAttacking()) {
            return;
        }
        if (!dragon.getSensing().hasLineOfSight(target)) {
            return;
        }
        var ability = dragon.getRandomAiAttackAbility();
        if (!dragon.combatManager.canStart(ability) || !dragon.getAiCombatPacing().canUse(ability, false)) {
            return;
        }
        dragon.combatManager.tryUseAbility(ability);
        dragon.getAiCombatPacing().recordUse(ability, 26, 26, false, 0, 22);
        attackCooldown = 26;
    }

    private boolean isCurrentlyAttacking() {
        return dragon.combatManager.isAbilityActive(ModAbilities.STEGONAUT_BITE)
                || dragon.combatManager.isAbilityActive(ModAbilities.STEGONAUT_CHIN_SLAM);
    }

    private double getAttackReachSqr(LivingEntity target, boolean inWater) {
        double combinedRadii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        double reach = (inWater ? attackRangeWater : attackRangeGround) + combinedRadii;
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

    private void handleWaterCombatChase(LivingEntity target, boolean inAttackRange, boolean hasLineOfSight) {
        dragon.getNavigation().stop();

        Vec3 current = dragon.getDeltaMovement();
        Vec3 toTarget = target.position().subtract(dragon.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
        Vec3 desiredHorizontal = horizontal.lengthSqr() > 1.0E-4
                ? horizontal.normalize().scale(0.18D)
                : Vec3.ZERO;

        double nx = current.x + (desiredHorizontal.x - current.x) * 0.35D;
        double nz = current.z + (desiredHorizontal.z - current.z) * 0.35D;

        double targetY = target.getY() + target.getBbHeight() * 0.5D;
        double yDiff = targetY - dragon.getY();
        double ny = current.y;
        if (yDiff > 0.7D) {
            ny = Math.max(current.y + 0.045D, 0.07D);
        } else if (yDiff < -1.0D) {
            ny = Math.min(current.y - 0.04D, -0.09D);
        } else {
            ny = current.y + 0.008D;
        }

        dragon.setDeltaMovement(nx, ny, nz);
        dragon.getMoveControl().setWantedPosition(target.getX(), targetY, target.getZ(), 1.0D);

        if (inAttackRange && hasLineOfSight) {
            tryPerformMelee(target);
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
