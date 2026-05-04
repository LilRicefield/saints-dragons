package com.leon.saintsdragons.server.ai.goals.volitans;

import com.leon.saintsdragons.common.registry.volitans.VolitansAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonAsyncAirMovementHelper;
import com.leon.saintsdragons.server.ai.goals.base.DragonLandingHelper;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class VolitansAirCombatGoal extends Goal {
    private static final double MELEE_RANGE = 6.0D;
    private static final double BREATH_MIN_RANGE = 10.0D;
    private static final double BREATH_MAX_RANGE = 24.0D;
    private static final double POISON_MIN_RANGE = 14.0D;
    private static final double POISON_MAX_RANGE = 32.0D;
    private static final double ROAR_MIN_RANGE = 6.0D;
    private static final double ROAR_MAX_RANGE = 12.0D;
    private static final double CHASE_HEIGHT_OFFSET = 2.0D;
    private static final double CHASE_SPEED = 2.0D;
    private static final double POSITION_SPEED = 0.85D;
    private static final double BITE_APPROACH_DISTANCE = 3.75D;
    private static final int MELEE_CADENCE_TICKS = 30;

    private final Volitans dragon;
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
        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_BREATH)
                || dragon.isAbilityActive(VolitansAbilities.VOLITANS_POISON_BALL)
                || dragon.isAbilityActive(VolitansAbilities.VOLITANS_ROAR)
                || dragon.isAbilityActive(VolitansAbilities.VOLITANS_BITE)
                || dragon.isAbilityActive(VolitansAbilities.VOLITANS_CLAW)
                || dragon.isAbilityActive(VolitansAbilities.VOLITANS_HORN_GORE)) {
            return true;
        }
        if (!isTargetAirborne(target)) {
            if (dragon.isFlying() || dragon.isHovering()) {
                DragonLandingHelper.beginAggroLanding(dragon, target, 1.0D);
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
        dragon.setAggressive(true);
        if (dragon.onGround() && !dragon.isFlying() && !dragon.isTakeoff() && !dragon.isHovering() && !dragon.isLanding()) {
            dragon.beginAiTakeoff();
        } else {
            dragon.beginAiFlight();
        }
    }

    @Override
    public void stop() {
        dragon.setAggressive(false);
        dragon.setAiSpecialCombatReserved(false);
        LivingEntity target = dragon.getTarget();
        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_POISON_BALL)) {
            dragon.requestPoisonBallRelease();
        }
        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_BREATH)) {
            dragon.forceEndActiveAbility();
        }
        if (target != null
                && dragon.isTargetValid(target)
                && !isTargetAirborne(target)
                && (dragon.isFlying() || dragon.isHovering())) {
            DragonLandingHelper.tryBeginAggroLanding(dragon, target, 1.0D);
            return;
        }
        if (target == null || dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            dragon.setTarget(null);
            dragon.setLanding(false);
            dragon.setHovering(false);
        }
    }

    @Override
    public void tick() {
        if (dragon.isLanding()) {
            if (!dragon.getNavigation().isInProgress()) {
                LivingEntity landingTarget = dragon.getTarget();
                if (landingTarget != null
                        && dragon.isTargetValid(landingTarget)
                        && !isTargetAirborne(landingTarget)
                        && DragonLandingHelper.tryBeginAggroLanding(dragon, landingTarget, 1.0D)) {
                    return;
                }
                dragon.setLanding(false);
                dragon.beginAiFlight();
            }
            return;
        }

        LivingEntity target = dragon.getTarget();
        if (!isValidTarget(target)) {
            dragon.setTarget(null);
            stop();
            return;
        }

        dragon.getLookControl().setLookAt(target, 35.0F, 35.0F);

        if (dragon.isTakeoff() && dragon.onGround()) {
            dragon.getNavigation().stop();
            return;
        }

        if (dragon.isTakeoff() && !dragon.onGround()) {
            dragon.beginAiFlight();
        }

        double distance = dragon.distanceTo(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_POISON_BALL)) {
            flyTowardTarget(target, POSITION_SPEED * 0.75D, CHASE_HEIGHT_OFFSET);
            if (--poisonHoldTicks <= 0 || !hasLineOfSight || distance < 8.0D || distance > 36.0D) {
                dragon.requestPoisonBallRelease();
            }
            return;
        }

        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_BREATH)) {
            flyTowardTarget(target, POSITION_SPEED * 0.65D, CHASE_HEIGHT_OFFSET);
            if (--breathHoldTicks <= 0 || !hasLineOfSight || distance < 7.0D || distance > 26.0D) {
                dragon.forceEndActiveAbility();
            }
            return;
        }

        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_ROAR)) {
            flyTowardTarget(target, POSITION_SPEED * 0.85D, CHASE_HEIGHT_OFFSET);
            return;
        }

        if (distance <= MELEE_RANGE && hasLineOfSight) {
            if (dragon.getAiCombatPacing().getCadenceCooldownTicks() <= 0) {
                tryMelee();
            }
            maintainMeleePosition(target);
            return;
        }

        if (distance >= POISON_MIN_RANGE && distance <= POISON_MAX_RANGE && hasLineOfSight && canUseAiAbility(VolitansAbilities.VOLITANS_POISON_BALL, true)) {
            if (!startAiAbility(VolitansAbilities.VOLITANS_POISON_BALL, true, 14, 120, 90, 36)) {
                return;
            }
            poisonHoldTicks = 20 + dragon.getRandom().nextInt(8);
            flyTowardTarget(target, POSITION_SPEED * 0.8D, CHASE_HEIGHT_OFFSET);
            return;
        }

        if (distance >= BREATH_MIN_RANGE && distance <= BREATH_MAX_RANGE && hasLineOfSight && canUseAiAbility(VolitansAbilities.VOLITANS_BREATH, true)) {
            dragon.setBreathMode(dragon.getRandom().nextFloat() < 0.65F ? 1 : 0);
            if (!startAiAbility(VolitansAbilities.VOLITANS_BREATH, true, 16, 140, 110, 42)) {
                return;
            }
            breathHoldTicks = 50 + dragon.getRandom().nextInt(30);
            flyTowardTarget(target, POSITION_SPEED * 0.7D, CHASE_HEIGHT_OFFSET);
            return;
        }

        if (distance >= ROAR_MIN_RANGE && distance <= ROAR_MAX_RANGE && hasLineOfSight && canUseAiAbility(VolitansAbilities.VOLITANS_ROAR, true)) {
            if (!startAiAbility(VolitansAbilities.VOLITANS_ROAR, true, 12, 140, 120, 48)) {
                return;
            }
            flyTowardTarget(target, POSITION_SPEED * 0.9D, CHASE_HEIGHT_OFFSET);
            return;
        }

        flyTowardTarget(target, CHASE_SPEED, CHASE_HEIGHT_OFFSET);
    }

    private void tryMelee() {
        float roll = dragon.getRandom().nextFloat();
        if (roll < 0.40F && canUseAiAbility(VolitansAbilities.VOLITANS_BITE, false)) {
            startAiAbility(VolitansAbilities.VOLITANS_BITE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
        } else if (roll < 0.72F && canUseAiAbility(VolitansAbilities.VOLITANS_CLAW, false)) {
            startAiAbility(VolitansAbilities.VOLITANS_CLAW, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
        } else if (canUseAiAbility(VolitansAbilities.VOLITANS_HORN_GORE, false)) {
            startAiAbility(VolitansAbilities.VOLITANS_HORN_GORE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
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
        return dragon.combatManager.tryUseAiAbility(
                abilityType,
                majorAbility,
                cadenceTicks,
                abilityCooldownTicks,
                majorCooldownTicks,
                repeatLockoutTicks
        );
    }

    private void maintainMeleePosition(LivingEntity target) {
        double targetY = target.getY() + target.getBbHeight() * 0.5D + CHASE_HEIGHT_OFFSET;
        Vec3 toTarget = new Vec3(target.getX() - dragon.getX(), targetY - dragon.getY(), target.getZ() - dragon.getZ());
        double dist = toTarget.length();
        if (dist < 1.0E-4D) {
            return;
        }
        Vec3 dir = toTarget.scale(1.0D / dist);
        Vec3 desired = new Vec3(target.getX(), targetY, target.getZ()).subtract(dir.scale(BITE_APPROACH_DISTANCE));
        flyToward(desired, dist > BITE_APPROACH_DISTANCE ? POSITION_SPEED : POSITION_SPEED * 0.6D);
    }

    private void flyTowardTarget(LivingEntity target, double speedScale, double heightOffset) {
        DragonAsyncAirMovementHelper.chasePredictedTarget(
                dragon,
                target,
                5.0D,
                heightOffset,
                0.12D,
                0.5D,
                speedScale
        );
    }

    private void flyToward(Vec3 destination, double speedScale) {
        DragonAsyncAirMovementHelper.moveToward(dragon, destination, speedScale);
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
        double followRange = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 48.0D;
        }
        return followRange * followRange;
    }

    private boolean isTargetAirborne(LivingEntity target) {
        if (target.onGround()) {
            return false;
        }
        if (target.isPassenger() && target.getVehicle() != null) {
            return true;
        }
        if (target instanceof Player player && player.isFallFlying()) {
            return true;
        }
        double groundY = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target.blockPosition()).getY();
        return target.getY() - groundY > 8.0D;
    }
}
