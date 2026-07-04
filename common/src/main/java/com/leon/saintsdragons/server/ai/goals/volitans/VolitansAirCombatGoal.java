package com.leon.saintsdragons.server.ai.goals.volitans;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonAirCombatHelper;
import com.leon.saintsdragons.server.ai.goals.base.DragonAsyncAirMovementHelper;
import com.leon.saintsdragons.server.ai.goals.base.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class VolitansAirCombatGoal extends Goal {
    private static final double MELEE_RANGE = 6.0D;
    private static final double BREATH_MIN_RANGE = 10.0D;
    private static final double BREATH_MAX_RANGE = 24.0D;
    private static final double POISON_MAX_RANGE = 32.0D;
    private static final double ROAR_MAX_RANGE = 12.0D;
    private static final double CHASE_HEIGHT_OFFSET = 2.0D;
    private static final double MELEE_HEIGHT_OFFSET = 0.0D;
    private static final double CHASE_SPEED = 2.0D;
    private static final double DIVE_CHASE_SPEED = 3.1D;
    private static final double DIVE_CHASE_MIN_HEIGHT_ADVANTAGE = 7.0D;
    private static final double DIVE_CHASE_MAX_HORIZONTAL_DISTANCE = 42.0D;
    private static final double POSITION_SPEED = 0.85D;
    private static final double BITE_APPROACH_DISTANCE = 3.5D;
    private static final int MELEE_CADENCE_TICKS = 30;

    private final Volitans dragon;
    private int attackCooldown = 0;
    private int breathHoldTicks = 0;
    private int poisonHoldTicks = 0;

    public VolitansAirCombatGoal(Volitans dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (dragon.isBaby() || dragon.isVehicle() || dragon.isOrderedToSit()) {
            return false;
        }
        if (dragon.isInWater() || dragon.isInWaterOrBubble() || dragon.isInLava()) {
            return false;
        }
        if (dragon.isAiSpecialCombatActive() || dragon.isAiSpecialCombatReserved()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        if (!isValidTarget(target)) {
            return false;
        }
        if (!isTargetAirborne(target)) {
            return false;
        }
        if (!dragon.isAerial() && !DragonAirCombatHelper.canTriggerAiFlightForTarget(dragon, target)) {
            return false;
        }
        return dragon.distanceToSqr(target) <= getMaxAggroDistanceSqr();
    }

    @Override
    public boolean canContinueToUse() {
        if (dragon.isLanding()) {
            return !dragon.onGround();
        }

        if (dragon.isBaby() || dragon.isVehicle() || dragon.isOrderedToSit()) {
            return false;
        }
        if (dragon.isInWater() || dragon.isInWaterOrBubble() || dragon.isInLava()) {
            return false;
        }
        if (dragon.isAiSpecialCombatActive() || dragon.isAiSpecialCombatReserved()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        if (!isValidTarget(target)) {
            return false;
        }
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH)
                || dragon.isAbilityActive(ModAbilities.VOLITANS_POISON_BALL)
                || dragon.isAbilityActive(ModAbilities.VOLITANS_ROAR)
                || dragon.isAbilityActive(ModAbilities.VOLITANS_BITE)
                || dragon.isAbilityActive(ModAbilities.VOLITANS_CLAW)
                || dragon.isAbilityActive(ModAbilities.VOLITANS_HORN_GORE)) {
            return true;
        }
        if (!isTargetAirborne(target)) {
            if (dragon.isAerial()) {
                dragon.getAIMovement().setLandingWaypoint(target, 1.0D);
                return true;
            }
            return false;
        }
        return dragon.distanceToSqr(target) <= getMaxAggroDistanceSqr();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        DragonAirCombatHelper.startAirCombat(dragon, Volitans.TAKEOFF_ANIMATION_TICKS);
    }

    @Override
    public void stop() {
        dragon.setAiSpecialCombatReserved(false);
        attackCooldown = 0;
        LivingEntity target = dragon.getTarget();
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_POISON_BALL)) {
            dragon.requestPoisonBallRelease();
        }
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH)) {
            dragon.forceEndActiveAbility();
        }
        DragonAirCombatHelper.stopAirCombatAndLandWhenTargetLost(
                dragon,
                target,
                1.0D,
                this::isTargetAirborne,
                getMaxAggroDistanceSqr()
        );
    }

    @Override
    public void tick() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        if (dragon.isLanding()) {
            if (!dragon.getAIMovement().isPathing()) {
                LivingEntity landingTarget = dragon.getTarget();
                if (landingTarget != null
                        && dragon.isTargetValid(landingTarget)
                        && !isTargetAirborne(landingTarget)
                        && dragon.getAIMovement().trySetLandingWaypoint(landingTarget, 1.0D)) {
                    return;
                }
                dragon.setLanding(false);
                dragon.beginAiFlight();
            }
            return;
        }

        LivingEntity target = dragon.getTarget();
        if (DragonAirCombatHelper.stopIfTargetInvalid(dragon, this::stop)) {
            return;
        }
        if (!isValidTarget(target)) {
            dragon.setTarget(null);
            stop();
            return;
        }

        dragon.getLookControl().setLookAt(target, 35.0F, 35.0F);

        if (dragon.isTakeoff() && dragon.onGround()) {
            dragon.getAIMovement().stop();
            return;
        }

        if (dragon.isTakeoff() && !dragon.onGround()) {
            dragon.beginAiFlight();
        }

        double distance = dragon.distanceTo(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        if (dragon.isAbilityActive(ModAbilities.VOLITANS_POISON_BALL)) {
            flyTowardTarget(target, POSITION_SPEED * 0.75D, CHASE_HEIGHT_OFFSET);
            if (--poisonHoldTicks <= 0 || !hasLineOfSight || distance < 8.0D || distance > 36.0D) {
                dragon.requestPoisonBallRelease();
            }
            return;
        }

        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH)) {
            flyTowardTarget(target, POSITION_SPEED * 0.65D, CHASE_HEIGHT_OFFSET);
            if (--breathHoldTicks <= 0 || !hasLineOfSight || distance < 7.0D || distance > 26.0D) {
                dragon.forceEndActiveAbility();
            }
            return;
        }

        if (dragon.isAbilityActive(ModAbilities.VOLITANS_ROAR)) {
            flyTowardTarget(target, POSITION_SPEED * 0.85D, CHASE_HEIGHT_OFFSET);
            return;
        }

        if (distance <= MELEE_RANGE && hasLineOfSight) {
            if (attackCooldown <= 0 && dragon.getAiCombatPacing().getCadenceCooldownTicks() <= 0) {
                tryMelee(target);
            }
            maintainMeleePosition(target);
            return;
        }

        if (attackCooldown <= 0 && distance > ROAR_MAX_RANGE && distance <= POISON_MAX_RANGE && hasLineOfSight && canUseAiAbility(ModAbilities.VOLITANS_POISON_BALL, true)) {
            if (!startAiAbility(ModAbilities.VOLITANS_POISON_BALL, true, 14, 120, 90, 36)) {
                return;
            }
            poisonHoldTicks = 20 + dragon.getRandom().nextInt(8);
            flyTowardTarget(target, POSITION_SPEED * 0.8D, CHASE_HEIGHT_OFFSET);
            return;
        }

        if (attackCooldown <= 0 && distance >= BREATH_MIN_RANGE && distance <= BREATH_MAX_RANGE && hasLineOfSight && canUseAiAbility(ModAbilities.VOLITANS_BREATH, true)) {
            dragon.setBreathMode(dragon.getRandom().nextFloat() < 0.65F ? 1 : 0);
            if (!startAiAbility(ModAbilities.VOLITANS_BREATH, true, 16, 140, 110, 42)) {
                return;
            }
            breathHoldTicks = 50 + dragon.getRandom().nextInt(30);
            flyTowardTarget(target, POSITION_SPEED * 0.7D, CHASE_HEIGHT_OFFSET);
            return;
        }

        if (attackCooldown <= 0 && distance <= ROAR_MAX_RANGE && hasLineOfSight && canUseAiAbility(ModAbilities.VOLITANS_ROAR, true)) {
            if (!startAiAbility(ModAbilities.VOLITANS_ROAR, true, 12, 140, 120, 48)) {
                return;
            }
            flyTowardTarget(target, POSITION_SPEED * 0.9D, CHASE_HEIGHT_OFFSET);
            return;
        }

        chaseTarget(target);
    }

    private void tryMelee(LivingEntity target) {
        if (attackCooldown > 0) {
            return;
        }
        if (DragonTargetingHelper.isBiteOnlyPreyTarget(target)) {
            if (canUseAiAbility(ModAbilities.VOLITANS_BITE, false)) {
                startAiAbility(ModAbilities.VOLITANS_BITE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
            }
            return;
        }
        float roll = dragon.getRandom().nextFloat();
        if (roll < 0.40F && canUseAiAbility(ModAbilities.VOLITANS_BITE, false)) {
            startAiAbility(ModAbilities.VOLITANS_BITE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
        } else if (roll < 0.72F && canUseAiAbility(ModAbilities.VOLITANS_CLAW, false)) {
            startAiAbility(ModAbilities.VOLITANS_CLAW, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
        } else if (canUseAiAbility(ModAbilities.VOLITANS_HORN_GORE, false)) {
            startAiAbility(ModAbilities.VOLITANS_HORN_GORE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
        }
    }

    private boolean canUseAiAbility(com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType) && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

    private boolean startAiAbility(com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType,
                                   boolean majorAbility,
                                   int cadenceTicks,
                                   int abilityCooldownTicks,
                                   int majorCooldownTicks,
                                   int repeatLockoutTicks) {
        boolean started = dragon.combatManager.tryUseAiAbility(
                abilityType,
                majorAbility,
                cadenceTicks,
                abilityCooldownTicks,
                majorCooldownTicks,
                repeatLockoutTicks
        );
        if (started) {
            attackCooldown = Math.max(attackCooldown, cadenceTicks);
        }
        return started;
    }

    private void maintainMeleePosition(LivingEntity target) {
        DragonAirCombatHelper.holdMeleePosition(
                dragon,
                target,
                MELEE_HEIGHT_OFFSET,
                BITE_APPROACH_DISTANCE,
                1.2D,
                0.7D
        );
    }

    private void flyTowardTarget(LivingEntity target, double speedScale, double heightOffset) {
        DragonAirCombatHelper.chasePredicted(dragon, target, 5.0D, heightOffset, 0.12D, 0.5D, speedScale);
    }

    private void chaseTarget(LivingEntity target) {
        if (shouldDiveChase(target)) {
            DragonAirCombatHelper.chasePredicted(dragon, target, 3.0D, -0.25D, 0.08D, 0.12D, DIVE_CHASE_SPEED);
            return;
        }
        flyTowardTarget(target, CHASE_SPEED, CHASE_HEIGHT_OFFSET);
    }

    private boolean shouldDiveChase(LivingEntity target) {
        return DragonAirCombatHelper.shouldDiveChase(
                dragon,
                target,
                8.0D,
                DIVE_CHASE_MIN_HEIGHT_ADVANTAGE,
                DIVE_CHASE_MAX_HORIZONTAL_DISTANCE
        );
    }

    private boolean isValidTarget(LivingEntity target) {
        if (!dragon.isTargetValid(target) || target == null) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return true;
    }

    private double getMaxAggroDistanceSqr() {
        return DragonAirCombatHelper.maxAggroDistanceSqr(dragon, 48.0D);
    }

    private boolean isTargetAirborne(LivingEntity target) {
        return DragonAirCombatHelper.isTargetAirborne(dragon, target, 8.0D);
    }
}
