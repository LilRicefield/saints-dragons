package com.leon.saintsdragons.server.ai.goals.varasuchus;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumSet;


public class VarasuchusCombatGoal extends Goal {
    private static final double CHASE_SPEED = 1.5D;
    private static final double WATER_CHASE_START_MULTIPLIER = 0.72D;
    private static final double WATER_CHASE_RECALC_MULTIPLIER = 0.66D;
    private static final double WATER_CHASE_FAR_BOOST = 1.12D;
    private static final double BITE_RANGE = 5.0D;
    private static final double LAND_PREY_BITE_RANGE = 1.45D;
    private static final double HORN_RANGE = 5.0D;
    private static final double CLAW_RANGE = 3.5D;
    private static final double DASH_MIN_GAP = 9.0D;
    private static final int MELEE_CADENCE_TICKS = 30;
    private static final int AI_DASH_COOLDOWN_TICKS = 8 * 20;
    private static final float PHASE_TWO_HEALTH_THRESHOLD = 0.5F;

    private final Varasuchus drake;
    private int aiDashCooldown;
    private int attackCooldown;
    private int pathRecalcCooldown = 0;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;

    public VarasuchusCombatGoal(Varasuchus drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (drake.isBaby()) {
            return false;
        }
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
        if (drake.isBaby()) {
            return false;
        }
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
        drake.setAggressive(true);

        LivingEntity target = drake.getTarget();
        if (target != null) {
            drake.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (drake.isInWaterOrBubble()) {
                updateWaterChase(target, WATER_CHASE_START_MULTIPLIER);
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
        attackCooldown = 0;
        pathRecalcCooldown = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (aiDashCooldown > 0) {
            aiDashCooldown--;
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        LivingEntity target = drake.getTarget();
        if (target == null) {
            return;
        }

        drake.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (drake.isAbilityActive(ModAbilities.VARASUCHUS_PHASE_SHIFT)) {
            return;
        }

        if (shouldEnterPhaseTwo()) {
            drake.combatManager.tryUseAbility(ModAbilities.VARASUCHUS_PHASE_SHIFT);
            if (drake.isAbilityActive(ModAbilities.VARASUCHUS_PHASE_SHIFT)) {
                return;
            }
        }

        double gap = getGapToTarget(target);
        boolean hasLineOfSight = drake.getSensing().hasLineOfSight(target);

        // Phase 2 AI dash: close distance only, with explicit 8 second AI cooldown.
        if (shouldUseAIDash(gap, hasLineOfSight, target)) {
            if (drake.tryAIGroundDash(target)) {
                aiDashCooldown = AI_DASH_COOLDOWN_TICKS;
                drake.getAiCombatPacing().setCadenceCooldownMin(12);
                return;
            }
        }

        double meleeStopRange = getMeleeStopRange(target);
        if (gap <= HORN_RANGE) {
            if (gap <= meleeStopRange || isPerformingAttack()) {
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
            if (!isPerformingAttack()) {
                updateChasePath(target);
            }
        }
    }

    private void tryPerformAttacks(LivingEntity target) {
        if (attackCooldown > 0 || drake.getAiCombatPacing().getCadenceCooldownTicks() > 0 || isPerformingAttack()) {
            return;
        }

        if (!drake.getSensing().hasLineOfSight(target)) {
            return;
        }

        DragonAbilityType<Varasuchus, ?> ability = choosePrimaryAttack(target);
        if (ability != null && drake.combatManager.canStart(ability) && drake.getAiCombatPacing().canUse(ability, false)) {
            drake.combatManager.tryUseAbility(ability);
            drake.getAiCombatPacing().recordUse(ability, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, false, 0, 24);
            attackCooldown = MELEE_CADENCE_TICKS;
        }
    }

    private DragonAbilityType<Varasuchus, ?> choosePrimaryAttack(LivingEntity target) {
        double gap = getGapToTarget(target);
        boolean phaseTwo = drake.isPhaseTwoActive();
        if (DragonTargetingHelper.isBiteOnlyPreyTarget(target)) {
            double biteRange = getMeleeStopRange(target);
            if (gap <= biteRange) {
                return phaseTwo ? ModAbilities.VARASUCHUS_BITE2 : ModAbilities.VARASUCHUS_BITE;
            }
            return null;
        }

        if (gap <= CLAW_RANGE) {
            return ModAbilities.VARASUCHUS_HORN_GORE;
        }

        if (phaseTwo && gap <= BITE_RANGE && drake.getRandom().nextFloat() < 0.30F) {
            return ModAbilities.VARASUCHUS_SLASH_BARRAGE;
        }

        if (phaseTwo && gap <= HORN_RANGE) {
            return drake.getRandom().nextBoolean()
                    ? ModAbilities.VARASUCHUS_BITE2
                    : ModAbilities.VARASUCHUS_CLAW;
        }

        if (!phaseTwo && gap > CLAW_RANGE && gap <= HORN_RANGE && drake.getRandom().nextFloat() < 0.35f) {
            return ModAbilities.VARASUCHUS_TAIL_ATTACK;
        }

        if (gap <= BITE_RANGE) {
            return phaseTwo ? ModAbilities.VARASUCHUS_BITE2 : ModAbilities.VARASUCHUS_BITE;
        }

        if (gap <= HORN_RANGE) {
            return ModAbilities.VARASUCHUS_HORN_GORE;
        }

        return null;
    }

    private boolean isPerformingAttack() {
        return drake.isAbilityActive(ModAbilities.VARASUCHUS_BITE)
            || drake.isAbilityActive(ModAbilities.VARASUCHUS_BITE2)
            || drake.isAbilityActive(ModAbilities.VARASUCHUS_SLASH_BARRAGE)
            || drake.isAbilityActive(ModAbilities.VARASUCHUS_CLAW)
            || drake.isAbilityActive(ModAbilities.VARASUCHUS_HORN_GORE)
            || drake.isAbilityActive(ModAbilities.VARASUCHUS_TAIL_ATTACK)
            || drake.isAbilityActive(ModAbilities.VARASUCHUS_ROAR);
    }

    private boolean shouldUseAIDash(double gap, boolean hasLineOfSight, LivingEntity target) {
        if (!drake.isPhaseTwoActive()) return false;
        if (aiDashCooldown > 0) return false;
        if (attackCooldown > 0) return false;
        if (!hasLineOfSight) return false;
        if (gap < DASH_MIN_GAP) return false;
        if (drake.getAiCombatPacing().getCadenceCooldownTicks() > 0) return false;
        if (isPerformingAttack()) return false;
        if (drake.isAbilityActive(ModAbilities.VARASUCHUS_PHASE_SHIFT)) return false;
        return isWithinAggroRange(target);
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

    private double getGapToTarget(LivingEntity target) {
        double distance = drake.distanceTo(target);
        double combinedRadii = (drake.getBbWidth() + target.getBbWidth()) * 0.5;
        return Math.max(0.0D, distance - combinedRadii);
    }

    private double getMeleeStopRange(LivingEntity target) {
        return DragonTargetingHelper.isBiteOnlyPreyTarget(target) && !drake.isInWaterOrBubble()
                ? LAND_PREY_BITE_RANGE
                : BITE_RANGE;
    }

    private void updateChasePath(LivingEntity target) {
        if (drake.isInWaterOrBubble()) {
            updateWaterChase(target, WATER_CHASE_RECALC_MULTIPLIER);
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
            speed *= WATER_CHASE_FAR_BOOST;
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
