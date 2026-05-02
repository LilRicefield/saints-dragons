package com.leon.saintsdragons.server.ai.goals.ignivorus;

import com.leon.saintsdragons.common.registry.ignivorus.IgnivorusAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonLandingHelper;
import com.leon.saintsdragons.server.ai.goals.base.DragonDirectAirCombatMovementHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;


public class IgnivorusAirCombatGoal extends Goal {
    private static final double FLIGHT_ACCEL = 0.12D;
    private static final double FLIGHT_DRAG = 0.94D;
    private static final double DIRECT_CHASE_SPEED = 3.75D;
    private static final double LANDING_SPEED = 1.5D;
    private final Ignivorus dragon;
    private static final double BITE_APPROACH_DISTANCE = 10.0D;
    private final double biteRange = 16.0;
    private final double fireBreathMinRange = 20.0;
    private final double fireBreathMaxRange = 64.0;
    private static final double ENGAGEMENT_DISTANCE = 25.0;
    private int attackCooldown = 0;
    private int repositionCooldown = 0;
    private int breathCooldown = 0;
    private static final int BREATH_COOLDOWN_TICKS = 2400;
    private int shotFromBelowCounter = 0;
    private static final int SHOT_FROM_BELOW_THRESHOLD = 3;
    private long lastDamageTick = 0;
    public IgnivorusAirCombatGoal(Ignivorus dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (dragon.isBaby()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();

        if (!dragon.isTargetValid(target)) {
            return false;
        }

        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        if (dragon.isVehicle() || dragon.isOrderedToSit()) {
            return false;
        }
        if (dragon.isAiSpecialCombatActive()) {
            return false;
        }
        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()) {
            return false;
        }

        if (!isTargetAirborne(target)) {
            return false;
        }

        if (!dragon.isFlying() && !dragon.isHovering() && !dragon.isTakeoff() && !dragon.isLanding()) {
            if (!canTriggerFlight()) {
                return false;
            }
        }

        if (dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (dragon.isLanding()) {
            return !dragon.onGround();
        }

        if (dragon.isBaby()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();

        if (!dragon.isTargetValid(target)) {
            return false;
        }

        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        if (dragon.isVehicle() || dragon.isOrderedToSit()) {
            return false;
        }
        if (dragon.isAiSpecialCombatActive()) {
            return false;
        }
        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()) {
            return false;
        }

        if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_BITE)) {
            return true;
        }

        if (!isTargetAirborne(target)) {
            if (dragon.isFlying() || dragon.isHovering()) {
                DragonLandingHelper.beginAggroLanding(dragon, target, LANDING_SPEED);
                return true;
            }
            return false;
        }

        if (dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
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
        dragon.setAggressive(false);
        attackCooldown = 0;
        repositionCooldown = 0;
        LivingEntity target = dragon.getTarget();
        if (target != null
                && dragon.isTargetValid(target)
                && !isTargetAirborne(target)
                && (dragon.isFlying() || dragon.isHovering())
                && !dragon.isLanding()) {
            DragonLandingHelper.tryBeginAggroLanding(dragon, target, LANDING_SPEED);
        }
    }

    @Override
    public void start() {
        dragon.setAggressive(true);

        if (dragon.onGround() && !dragon.isFlying() && !dragon.isHovering() && !dragon.isTakeoff() && !dragon.isLanding()) {
            dragon.beginAiTakeoff(Ignivorus.TAKEOFF_ANIMATION_TICKS);
        } else if (dragon.isFlying() || dragon.isHovering()) {
            dragon.beginAiFlight();
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
                        && DragonLandingHelper.tryBeginAggroLanding(dragon, landingTarget, LANDING_SPEED)) {
                    return;
                }
                dragon.setLanding(false);
            }
            return;
        }

        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()) {
            dragon.getNavigation().stop();
            return;
        }

        if (dragon.isTakeoff() && !dragon.onGround()) {
            dragon.beginAiFlight();
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        if (breathCooldown > 0) {
            breathCooldown--;
        }

        if (repositionCooldown > 0) {
            repositionCooldown--;
        }
        LivingEntity target = dragon.getTarget();
        if (!dragon.isTargetValid(target)) {
            dragon.setTarget(null);
            stop();
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
        checkEmergencyLanding(target);

        double distance = dragon.distanceTo(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        if (distance <= biteRange && hasLineOfSight) {
            if (!isCurrentlyAttacking()) {
                tryAttack(target, distance);
            }
            maintainBitePosition(target);
        } else if (distance >= fireBreathMinRange && distance <= fireBreathMaxRange && hasLineOfSight && breathCooldown <= 0) {
            if (!isCurrentlyAttacking()) {
                tryAttack(target, distance);
            }
            if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)) {
                DragonDirectAirCombatMovementHelper.holdPosition(dragon, FLIGHT_DRAG);
            } else {
                maintainCombatPosition(target);
            }
        } else {
            chaseTarget(target);
        }
    }

    private boolean isCurrentlyAttacking() {
        return dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_BITE)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)
            || dragon.isLeaping()
            || dragon.isLeapImpactRecovering();
    }

    private void tryAttack(LivingEntity target, double distance) {
        if (attackCooldown > 0 || isCurrentlyAttacking()) {
            return;
        }

        if (!dragon.getSensing().hasLineOfSight(target)) {
            return;
        }

        if (distance <= biteRange) {
            if (!canUseAiAbility(IgnivorusAbilities.IGNIVORUS_BITE, false)) {
                return;
            }
            dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_BITE);
            dragon.getAiCombatPacing().recordUse(IgnivorusAbilities.IGNIVORUS_BITE, 30, 30, false, 0, 24);
            attackCooldown = 30;
        } else if (distance >= fireBreathMinRange && distance <= fireBreathMaxRange && breathCooldown <= 0) {
            if (!canUseAiAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH, true)) {
                return;
            }
            dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
            dragon.getAiCombatPacing().recordUse(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH, 60, BREATH_COOLDOWN_TICKS, true, 180, 80);
            attackCooldown = 60;
            breathCooldown = BREATH_COOLDOWN_TICKS;
        }
    }

    private void chaseTarget(LivingEntity target) {
        DragonDirectAirCombatMovementHelper.chasePredictedTarget(
                dragon,
                target,
                5.0D,
                0.5D,
                0.15D,
                0.5D,
                DIRECT_CHASE_SPEED,
                FLIGHT_ACCEL,
                FLIGHT_DRAG
        );
    }


    private void maintainCombatPosition(LivingEntity target) {
        if (repositionCooldown > 0) {
            return;
        }

        double distance = dragon.distanceTo(target);
        double targetY = target.getY() + target.getBbHeight() * 0.5D;
        Vec3 targetLook = target.getLookAngle();
        double angle = (dragon.tickCount * 0.05) % (Math.PI * 2);
        double offsetX = Math.cos(angle) * ENGAGEMENT_DISTANCE;
        double offsetZ = Math.sin(angle) * ENGAGEMENT_DISTANCE;
        double posX = target.getX() + offsetX;
        double posZ = target.getZ() + offsetZ;
        double verticalOffset = Math.sin(dragon.tickCount * 0.1) * 1.0;

        DragonDirectAirCombatMovementHelper.flyToward(
                dragon,
                new Vec3(posX, targetY + verticalOffset, posZ),
                1.0D,
                FLIGHT_ACCEL,
                FLIGHT_DRAG
        );

        repositionCooldown = 20;
    }

    private void maintainBitePosition(LivingEntity target) {
        double targetY = target.getY() + target.getBbHeight() * 0.5D;

        Vec3 toTarget = new Vec3(
                target.getX() - dragon.getX(),
                targetY - dragon.getY(),
                target.getZ() - dragon.getZ()
        );

        double dist = toTarget.length();
        if (dist < 1.0E-4D) {
            return;
        }

        Vec3 dir = toTarget.scale(1.0D / dist);
        Vec3 desired = new Vec3(target.getX(), targetY, target.getZ()).subtract(dir.scale(BITE_APPROACH_DISTANCE));

        double speed = dist > BITE_APPROACH_DISTANCE ? 1.2D : 0.6D;
        DragonDirectAirCombatMovementHelper.flyToward(dragon, desired, speed, FLIGHT_ACCEL, FLIGHT_DRAG);
    }

    private boolean isTargetAirborne(LivingEntity target) {
        if (target.onGround()) {
            return false;
        }

        if (target.getVehicle() instanceof LivingEntity vehicle) {
            return !vehicle.onGround();
        }
        if (target.isFallFlying()) {
            return true;
        }
        double groundY = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target.blockPosition()).getY();
        double heightAboveGround = target.getY() - groundY;
        if (heightAboveGround > Math.max(2.5D, target.getBbHeight() * 0.75D)) {
            return true;
        }

        return false;
    }


    private void checkEmergencyLanding(LivingEntity target) {
        long currentTick = dragon.level().getGameTime();

        if (dragon.hurtTime > 0 && currentTick != lastDamageTick) {
            lastDamageTick = currentTick;

            if (target.getY() < dragon.getY() - 5.0) {
                shotFromBelowCounter++;

                if (shotFromBelowCounter >= SHOT_FROM_BELOW_THRESHOLD) {
                    triggerEmergencyLanding();
                }
            }
        }

        if (currentTick - lastDamageTick > 100) {
            shotFromBelowCounter = 0;
        }
    }


    private void triggerEmergencyLanding() {
        DragonLandingHelper.tryBeginAggroLanding(dragon, dragon.getTarget(), LANDING_SPEED);
        shotFromBelowCounter = 0;
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = this.dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 64.0D;
        }
        return followRange * followRange;
    }


    private boolean canTriggerFlight() {
        return !dragon.isOrderedToSit() &&
                !dragon.isBaby() &&
                (dragon.onGround() || dragon.isInWater()) &&
                dragon.getPassengers().isEmpty() &&
                dragon.getControllingPassenger() == null &&
                dragon.getActiveAbility() == null;
    }

    private boolean canUseAiAbility(DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType) && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

}