package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.common.registry.nulljaw.NulljawAbilities;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumSet;

/**
 * Ground combat coordinator for Nulljaw.
 * Features phase-based combat with roar opener and optimized pathfinding.
 */
public class NulljawCombatGoal extends Goal {
    private static final double CHASE_SPEED = 1.5D;
    private static final double BITE_RANGE = 5.0D;   // Matched to bite ability (5.5) - slightly conservative for AI
    private static final double HORN_RANGE = 5.0D;   // Matched to horn gore ability (7.0) - slightly conservative for AI
    private static final double CLAW_RANGE = 3.5D;   // Claw requires closer range - it's a swipe attack, not a lunge
    private static final int MIN_ATTACK_COOLDOWN_TICKS = 20;
    private static final float PHASE_TWO_HEALTH_THRESHOLD = 0.5F;

    private final Nulljaw drake;
    private int attackCooldown;
    private int pathRecalcCooldown = 0;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;

    // Roar opener mechanic
    private boolean hasUsedRoarOpener = false;
    private int roarOpenerDelay = 0;

    public NulljawCombatGoal(Nulljaw drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = drake.getTarget();
        if (!drake.isTargetValid(target)) {
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
        if (!drake.isTargetValid(target)) {
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

        // Initialize roar opener - only in phase 2 (roar2 has damaging claw swipes)
        if (drake.isPhaseTwoActive()) {
            hasUsedRoarOpener = false;
            roarOpenerDelay = 5;
        } else {
            // Skip roar opener in phase 1
            hasUsedRoarOpener = true;
            roarOpenerDelay = 0;
        }

        LivingEntity target = drake.getTarget();
        if (target != null) {
            drake.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (drake.isInWaterOrBubble()) {
                updateWaterChase(target, 1.0D);
            } else {
                drake.getNavigation().moveTo(target, CHASE_SPEED);
                rememberTargetPosition(target);
            }
        }
    }

    @Override
    public void stop() {
        drake.getNavigation().stop();
        drake.setAggressive(false);
        pathRecalcCooldown = 0;

        // Reset roar opener for next combat encounter
        hasUsedRoarOpener = false;
        roarOpenerDelay = 0;
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

        drake.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (drake.isAbilityActive(NulljawAbilities.NULLJAW_PHASE_SHIFT)) {
            return;
        }

        if (shouldEnterPhaseTwo()) {
            drake.combatManager.tryUseAbility(NulljawAbilities.NULLJAW_PHASE_SHIFT);
            if (drake.isAbilityActive(NulljawAbilities.NULLJAW_PHASE_SHIFT)) {
                return;
            }
        }

        // Handle roar opener - use roar once at the start of combat (phase 2 only)
        if (!hasUsedRoarOpener) {
            if (roarOpenerDelay > 0) {
                roarOpenerDelay--;
                // Keep chasing during delay
                updateChasePath(target);
            } else {
                // Delay expired - use roar ability and KEEP WALKING toward target
                // Roar2 has 7 claw swipes that need to land, so maintain chase
                drake.combatManager.tryUseAbility(NulljawAbilities.NULLJAW_ROAR);
                hasUsedRoarOpener = true;
                attackCooldown = 100; // Roar lasts 100 ticks, wait for it to finish
            }
            // KEEP CHASING even after roar starts (roar2 needs close range for swipes)
            updateChasePath(target);
            return; // Don't do normal attacks during roar
        }

        // Normal combat after roar opener
        double gap = getGapToTarget(target);
        boolean hasLineOfSight = drake.getSensing().hasLineOfSight(target);

        // In melee range - try to attack, but keep chasing if too far for selected ability
        if (gap <= HORN_RANGE) {
            // Stop and attack only if we're close enough OR already attacking
            if (gap <= BITE_RANGE || isPerformingAttack()) {
                if (drake.isInWaterOrBubble()) {
                    drake.setDeltaMovement(drake.getDeltaMovement().scale(0.85D));
                } else {
                    drake.getNavigation().stop();
                    pathRecalcCooldown = 0;
                }
            } else {
                // In horn range but not bite range - keep approaching for better attacks
                updateChasePath(target);
            }
            tryPerformAttacks(target);
        } else {
            // Out of all melee ranges - chase to get closer
            if (!isPerformingAttack()) {
                updateChasePath(target);
            }
        }
    }

    private void tryPerformAttacks(LivingEntity target) {
        if (attackCooldown > 0 || isPerformingAttack()) {
            return;
        }

        if (!drake.getSensing().hasLineOfSight(target)) {
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

        // Claw is close-range only (3.5 gap) - swipe attack that needs proximity
        if (gap <= CLAW_RANGE) {
            // Phase 2: prefer claw at very close range, alternate with bite2
            if (phaseTwo && drake.getRandom().nextFloat() < 0.6f) {
                return NulljawAbilities.NULLJAW_CLAW;
            }
            return phaseTwo ? NulljawAbilities.NULLJAW_BITE2 : NulljawAbilities.NULLJAW_BITE;
        }

        // Bite range (5.0 gap) - medium close range
        if (gap <= BITE_RANGE) {
            return phaseTwo ? NulljawAbilities.NULLJAW_BITE2 : NulljawAbilities.NULLJAW_BITE;
        }

        // Horn range (5.0 gap) - can be used at bite range too
        if (gap <= HORN_RANGE) {
            return NulljawAbilities.NULLJAW_HORN_GORE;
        }

        return null;
    }

    /**
     * Check if drake is currently executing an attack ability
     */
    private boolean isPerformingAttack() {
        return drake.isAbilityActive(NulljawAbilities.NULLJAW_BITE)
            || drake.isAbilityActive(NulljawAbilities.NULLJAW_BITE2)
            || drake.isAbilityActive(NulljawAbilities.NULLJAW_CLAW)
            || drake.isAbilityActive(NulljawAbilities.NULLJAW_HORN_GORE)
            || drake.isAbilityActive(NulljawAbilities.NULLJAW_ROAR);
    }

    private boolean shouldEnterPhaseTwo() {
        return !drake.isPhaseTwoActive()
            && drake.getHealth() <= drake.getMaxHealth() * PHASE_TWO_HEALTH_THRESHOLD;
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

    /**
     * Get the gap between entity edges (not centers)
     */
    private double getGapToTarget(LivingEntity target) {
        double distance = drake.distanceTo(target);
        double combinedRadii = (drake.getBbWidth() + target.getBbWidth()) * 0.5;
        return Math.max(0.0D, distance - combinedRadii);
    }

    /**
     * Update chase path with optimized recalculation
     */
    private void updateChasePath(LivingEntity target) {
        if (drake.isInWaterOrBubble()) {
            updateWaterChase(target, 0.9D);
            return;
        }

        if (--pathRecalcCooldown <= 0 || targetMovedSignificantly(target)) {
            rememberTargetPosition(target);
            double distance = drake.distanceTo(target);
            pathRecalcCooldown = Mth.clamp((int) (distance * 0.6D), 5, 20);
            drake.getNavigation().moveTo(target, CHASE_SPEED);
        }
    }

    private void updateWaterChase(LivingEntity target, double multiplier) {
        drake.getNavigation().stop();

        double dx = target.getX() - drake.getX();
        double dy = (target.getY() + target.getEyeHeight() * 0.5) - (drake.getY() + drake.getEyeHeight() * 0.5);
        double dz = target.getZ() - drake.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        if (horizontalDist < 1.0E-5D && Math.abs(dy) < 1.0E-5D) {
            return;
        }

        float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        drake.setYRot(Mth.wrapDegrees(targetYaw));
        drake.yBodyRot = drake.getYRot();
        drake.yHeadRot = drake.getYRot();

        float targetPitch = -((float) (Mth.atan2(dy, horizontalDist) * Mth.RAD_TO_DEG));
        targetPitch = Mth.clamp(Mth.wrapDegrees(targetPitch), -85.0F, 85.0F);
        drake.setXRot(targetPitch);

        double speed = drake.getSwimSpeed() * multiplier;
        if (horizontalDist > 15.0D) {
            speed *= 1.3D;
        }

        double yawRad = drake.getYRot() * Mth.DEG_TO_RAD;
        double pitchRad = drake.getXRot() * Mth.DEG_TO_RAD;
        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);

        drake.setDeltaMovement(dirX * speed, dirY * speed, dirZ * speed);
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
