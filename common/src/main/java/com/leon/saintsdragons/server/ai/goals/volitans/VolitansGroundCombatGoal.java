package com.leon.saintsdragons.server.ai.goals.volitans;

import com.leon.saintsdragons.common.registry.volitans.VolitansAbilities;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class VolitansGroundCombatGoal extends Goal {
    private static final double BITE_RANGE = 4.1D;
    private static final double CLAW_RANGE = 5.1D;
    private static final double GORE_RANGE = 6.2D;
    private static final double BREATH_MIN_RANGE = 6.0D;
    private static final double BREATH_MAX_RANGE = 16.0D;
    private static final double POISON_BALL_MIN_RANGE = 8.0D;
    private static final double POISON_BALL_MAX_RANGE = 24.0D;
    private static final double ROAR_OPEN_RANGE = 14.0D;
    private static final double CHASE_SPEED = 1.2D;
    private static final double BURROW_MIN_RANGE = 10.0D;
    private static final double BURROW_MAX_RANGE = 30.0D;
    private static final double BURROW_CHASE_SPEED = 1.55D;

    private final Volitans dragon;
    private int pathRecalcCooldown = 0;
    private int burrowCooldown = 0;
    private int poisonBallHoldTicks = 0;
    private int breathHoldTicks = 0;
    private boolean usedRoarOpener = false;
    private int roarOpenerDelay = 0;
    private boolean wasAbilityHoldingLastTick = false;
    private boolean wasBurrowAbilityHoldingLastTick = false;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;

    public VolitansGroundCombatGoal(Volitans dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (dragon.isAiSpecialCombatActive() || dragon.isAiSpecialCombatReserved()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        if (!canFightTarget(target)) {
            return false;
        }
        return !dragon.isFlying() && !dragon.isTakeoff() && !dragon.isLanding() && !dragon.isHovering();
    }

    @Override
    public boolean canContinueToUse() {
        if (dragon.isAiSpecialCombatActive() || dragon.isAiSpecialCombatReserved()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        if (dragon.isGroundCombatAbilityActive() || dragon.isGroundMobilityActive()) {
            return true;
        }
        if (!canFightTarget(target)) {
            return false;
        }
        if (dragon.isFlying() || dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering()) {
            return false;
        }
        return !isTargetAirborne(target);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        dragon.setAggressive(true);
        usedRoarOpener = false;
        roarOpenerDelay = 8;
        wasAbilityHoldingLastTick = false;
        wasBurrowAbilityHoldingLastTick = false;
        LivingEntity target = dragon.getTarget();
        if (target != null) {
            dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
            dragon.getNavigation().moveTo(target, CHASE_SPEED);
            rememberTargetPosition(target);
        }
    }

    @Override
    public void stop() {
        dragon.getNavigation().stop();
        dragon.setAggressive(false);
        pathRecalcCooldown = 0;
        poisonBallHoldTicks = 0;
        breathHoldTicks = 0;
        wasAbilityHoldingLastTick = false;
        wasBurrowAbilityHoldingLastTick = false;
        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_POISON_BALL)) {
            dragon.requestPoisonBallRelease();
        }
        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_BREATH)) {
            dragon.forceEndActiveAbility();
        }
        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_BURROW)) {
            dragon.requestBurrowExit(false);
        }
    }

    @Override
    public void tick() {
        if (burrowCooldown > 0) burrowCooldown--;

        LivingEntity target = dragon.getTarget();
        if (!dragon.isTargetValid(target)) {
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double gap = getGapToTarget(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        if (handleActiveAbility(target, gap, hasLineOfSight)) {
            wasAbilityHoldingLastTick = true;
            return;
        }
        if (wasAbilityHoldingLastTick) {
            // Ability-owned movement just ended; immediately reacquire the chase instead of
            // waiting on a stale path refresh or long post-ability attack cooldown.
            wasAbilityHoldingLastTick = false;
            pathRecalcCooldown = 0;
            if (wasBurrowAbilityHoldingLastTick) {
                dragon.getAiCombatPacing().setGlobalActionLock(10);
                wasBurrowAbilityHoldingLastTick = false;
            }
            updateChasePath(target);
        }

        if (!usedRoarOpener && hasLineOfSight && gap >= 5.0D && gap <= ROAR_OPEN_RANGE && canUseAiAbility(VolitansAbilities.VOLITANS_ROAR, true)) {
            if (roarOpenerDelay > 0) {
                roarOpenerDelay--;
                updateChasePath(target);
            } else {
                dragon.getNavigation().stop();
                dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_ROAR);
                dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_ROAR, 24, 200, true, 120, 48);
                usedRoarOpener = true;
            }
            return;
        }
        usedRoarOpener = true;

        if (!hasLineOfSight) {
            updateChasePath(target);
            return;
        }

        if (gap <= GORE_RANGE) {
            if (gap <= BITE_RANGE) {
                dragon.getNavigation().stop();
                pathRecalcCooldown = 0;
            } else {
                updateChasePath(target);
            }
            if (dragon.getAiCombatPacing().getCadenceCooldownTicks() <= 0 && !dragon.isGroundCombatAbilityActive()) {
                tryMelee(gap);
                return;
            }
            if (gap > BITE_RANGE) {
                updateChasePath(target);
            }
            return;
        }

        if (tryRoarPunish(gap)) {
            return;
        }

        if (tryBurrowApproach(target, gap, hasLineOfSight)) {
            return;
        }

        if (tryPoisonBall(gap)) {
            return;
        }

        if (tryBreath(gap)) {
            return;
        }

        updateChasePath(target);
    }

    private boolean canFightTarget(LivingEntity target) {
        if (!dragon.isTargetValid(target)) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (dragon.isVehicle() || dragon.isOrderedToSit() || dragon.isBaby()) {
            return false;
        }
        if (dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }
        if (dragon.isInWaterOrBubble() || dragon.isUnderWater()) {
            return false;
        }
        return !isTargetAirborne(target);
    }

    private boolean handleActiveAbility(LivingEntity target, double gap, boolean hasLineOfSight) {
        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_POISON_BALL)) {
            dragon.getNavigation().stop();
            if (--poisonBallHoldTicks <= 0 || !hasLineOfSight || gap < 5.0D || gap > 28.0D) {
                dragon.requestPoisonBallRelease();
            }
            return true;
        }
        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_BREATH)) {
            dragon.getNavigation().stop();
            if (--breathHoldTicks <= 0 || !hasLineOfSight || gap < 4.5D || gap > 18.0D) {
                dragon.forceEndActiveAbility();
            }
            return true;
        }
        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_BURROW)) {
            wasBurrowAbilityHoldingLastTick = true;
            if (!dragon.isTargetValid(target) || dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
                dragon.requestBurrowExit(false);
                return true;
            }
            if (dragon.isBurrowing()) {
                dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
                dragon.getNavigation().moveTo(target, BURROW_CHASE_SPEED);
            } else {
                dragon.getNavigation().stop();
            }
            if (dragon.isBurrowing() && gap <= 4.75D) {
                dragon.requestBurrowExit(true);
            }
            return true;
        }
        if (dragon.shouldAiHoldPositionForAbility() || dragon.isGroundMobilityActive()) {
            dragon.getNavigation().stop();
            return true;
        }
        return false;
    }

    private boolean tryRoarPunish(double gap) {
        if (!canUseAiAbility(VolitansAbilities.VOLITANS_ROAR, true) || dragon.isGroundCombatAbilityActive()) {
            return false;
        }
        if (gap < 4.5D || gap > 10.0D) {
            return false;
        }
        dragon.getNavigation().stop();
        dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_ROAR);
        dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_ROAR, 24, 200, true, 120, 48);
        return true;
    }

    private boolean tryPoisonBall(double gap) {
        if (!canUseAiAbility(VolitansAbilities.VOLITANS_POISON_BALL, true) || dragon.isGroundCombatAbilityActive()) {
            return false;
        }
        if (gap < POISON_BALL_MIN_RANGE || gap > POISON_BALL_MAX_RANGE) {
            return false;
        }
        dragon.getNavigation().stop();
        dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_POISON_BALL);
        poisonBallHoldTicks = 18 + dragon.getRandom().nextInt(8);
        dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_POISON_BALL, 16, 110, true, 90, 36);
        return true;
    }

    private boolean tryBurrowApproach(LivingEntity target, double gap, boolean hasLineOfSight) {
        if (dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0 || burrowCooldown > 0 || dragon.isGroundCombatAbilityActive() || dragon.isGroundMobilityActive()) {
            return false;
        }
        if (gap < BURROW_MIN_RANGE || gap > BURROW_MAX_RANGE) {
            return false;
        }
        if (dragon.hurtTime <= 0 && hasLineOfSight && dragon.getRandom().nextFloat() >= 0.18F) {
            return false;
        }
        dragon.getNavigation().stop();
        dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_BURROW);
        burrowCooldown = 220;
        dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_BURROW, 14, 0, true, 80, 28);
        return true;
    }

    private boolean tryBreath(double gap) {
        if (!canUseAiAbility(VolitansAbilities.VOLITANS_BREATH, true) || dragon.isGroundCombatAbilityActive()) {
            return false;
        }
        if (gap < BREATH_MIN_RANGE || gap > BREATH_MAX_RANGE) {
            return false;
        }
        if (dragon.getRandom().nextFloat() >= 0.35F) {
            return false;
        }
        dragon.getNavigation().stop();
        dragon.setBreathMode(dragon.getRandom().nextFloat() < 0.65F ? 1 : 0);
        dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_BREATH);
        breathHoldTicks = 60 + dragon.getRandom().nextInt(35);
        dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_BREATH, 18, 150, true, 110, 42);
        return true;
    }

    private void tryMelee(double gap) {
        if (dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0 || dragon.isGroundCombatAbilityActive()) {
            return;
        }

        if (gap <= BITE_RANGE) {
            float roll = dragon.getRandom().nextFloat();
            if (roll < 0.42F && canUseAiAbility(VolitansAbilities.VOLITANS_CLAW, false)) {
                dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_CLAW);
                dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_CLAW, 14, 18, false, 0, 20);
            } else if (roll < 0.72F && canUseAiAbility(VolitansAbilities.VOLITANS_BITE, false)) {
                dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_BITE);
                dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_BITE, 12, 16, false, 0, 18);
            } else if (canUseAiAbility(VolitansAbilities.VOLITANS_HORN_GORE, false)) {
                dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_HORN_GORE);
                dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_HORN_GORE, 16, 22, false, 0, 24);
            }
            return;
        }

        if (gap <= CLAW_RANGE) {
            if (dragon.getRandom().nextFloat() < 0.58F && canUseAiAbility(VolitansAbilities.VOLITANS_CLAW, false)) {
                dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_CLAW);
                dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_CLAW, 14, 18, false, 0, 20);
            } else if (canUseAiAbility(VolitansAbilities.VOLITANS_HORN_GORE, false)) {
                dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_HORN_GORE);
                dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_HORN_GORE, 16, 22, false, 0, 24);
            }
            return;
        }

        if (gap <= GORE_RANGE && canUseAiAbility(VolitansAbilities.VOLITANS_HORN_GORE, false)) {
            dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_HORN_GORE);
            dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_HORN_GORE, 16, 22, false, 0, 24);
        }
    }

    private boolean canUseAiAbility(com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType) && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

    private double getGapToTarget(LivingEntity target) {
        double centerDistance = dragon.distanceTo(target);
        double combinedRadii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        return Math.max(0.0D, centerDistance - combinedRadii);
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 32.0D;
        }
        return followRange * followRange;
    }

    private void updateChasePath(LivingEntity target) {
        if (--pathRecalcCooldown <= 0 || targetMovedSignificantly(target)) {
            rememberTargetPosition(target);
            double distance = dragon.distanceTo(target);
            pathRecalcCooldown = Mth.clamp((int) (distance * 0.55D), 5, 20);
            dragon.getNavigation().moveTo(target, CHASE_SPEED);
        }
    }

    private void rememberTargetPosition(LivingEntity target) {
        lastTargetX = target.getX();
        lastTargetY = target.getY();
        lastTargetZ = target.getZ();
    }

    private boolean targetMovedSignificantly(LivingEntity target) {
        double dx = target.getX() - lastTargetX;
        double dy = target.getY() - lastTargetY;
        double dz = target.getZ() - lastTargetZ;
        return dx * dx + dy * dy + dz * dz > 4.0D;
    }

    private boolean isTargetAirborne(LivingEntity target) {
        return !target.onGround() && !target.isInWaterOrBubble();
    }
}
