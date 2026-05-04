package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.common.registry.raevyx.RaevyxAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonAsyncAirMovementHelper;
import com.leon.saintsdragons.server.ai.goals.base.DragonLandingHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RaevyxAirCombatGoal extends Goal {
    private final Raevyx dragon;
    private static final double DIRECT_CHASE_SPEED = 6.0D;
    private static final double BITE_TRIGGER_RANGE = 7.0;
    private static final double ENGAGEMENT_DISTANCE = 30.0;
    private static final double BITE_APPROACH_DISTANCE = 10.0;
    private final double beamMinRange = 20.0;
    private final double beamMaxRange = 70.0;
    private int attackCooldown = 0;
    private int repositionCooldown = 0;
    private int beamCooldown = 0;
    private static final int BEAM_COOLDOWN_TICKS = 2400;
    private int shotFromBelowCounter = 0;
    private static final int SHOT_FROM_BELOW_THRESHOLD = 3;
    private long lastDamageTick = 0;

    public RaevyxAirCombatGoal(Raevyx dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = dragon.getTarget();

        if (!dragon.isTargetValid(target)) {
            return false;
        }

        if (target instanceof net.minecraft.world.entity.player.Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        if (dragon.isVehicle() || dragon.isOrderedToSit()) {
            return false;
        }

        boolean dragonAirborne = dragon.isFlying() || dragon.isHovering() || dragon.isTakeoff() || dragon.isLanding();
        boolean targetAirborne = isTargetAirborne(target);
        if (!targetAirborne && !dragonAirborne) {
            return false;
        }

        if (targetAirborne && !dragonAirborne) {
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

        LivingEntity target = dragon.getTarget();

        if (!dragon.isTargetValid(target)) {
            return false;
        }

        if (target instanceof net.minecraft.world.entity.player.Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        if (dragon.isVehicle() || dragon.isOrderedToSit()) {
            return false;
        }

        if (dragon.isAbilityActive(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM)
            || dragon.isAbilityActive(RaevyxAbilities.RAEVYX_BITE)) {
            return true;
        }

        if (!isTargetAirborne(target)) {
            if (dragon.isFlying() || dragon.isHovering()) {
                DragonLandingHelper.beginAggroLanding(dragon, target, 1.6D);
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
            DragonLandingHelper.tryBeginAggroLanding(dragon, target, 1.6D);
        }
    }

    @Override
    public void start() {
        dragon.setAggressive(true);

        if (dragon.onGround() && !dragon.isFlying() && !dragon.isHovering() && !dragon.isTakeoff() && !dragon.isLanding()) {
            dragon.beginAiTakeoff(Raevyx.TAKEOFF_ANIMATION_TICKS);
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
                        && DragonLandingHelper.tryBeginAggroLanding(dragon, landingTarget, 1.6D)) {
                    return;
                }
                dragon.setLanding(false);
            }
            return;
        }

        if (dragon.isTakeoff() && dragon.isFlying() && !dragon.onGround()) {
            dragon.beginAiFlight();
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        if (beamCooldown > 0) {
            beamCooldown--;
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

        if (!isTargetAirborne(target)) {
            if (dragon.isFlying() || dragon.isHovering() || dragon.isTakeoff()) {
                DragonLandingHelper.tryBeginAggroLanding(dragon, target, 1.6D);
            }
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        checkEmergencyLanding(target);
        double distance = dragon.distanceTo(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        if (distance <= BITE_TRIGGER_RANGE && hasLineOfSight) {
            if (!isCurrentlyAttacking()) {
                tryAttack(target, distance);
            }
            maintainBitePosition(target);
        } else if (distance >= beamMinRange && distance <= beamMaxRange && hasLineOfSight && beamCooldown <= 0) {
            if (!isCurrentlyAttacking()) {
                tryAttack(target, distance);
            }
            if (dragon.isAbilityActive(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM)) {
                DragonAsyncAirMovementHelper.holdPosition(dragon);
            } else {
                maintainCombatPosition(target);
            }
        } else {
            chaseTarget(target);
        }
    }

    private boolean isCurrentlyAttacking() {
        return dragon.isAbilityActive(RaevyxAbilities.RAEVYX_BITE)
            || dragon.isAbilityActive(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM);
    }


    private void tryAttack(LivingEntity target, double distance) {
        if (attackCooldown > 0 || isCurrentlyAttacking()) {
            return;
        }

        if (!dragon.getSensing().hasLineOfSight(target)) {
            return;
        }

        if (distance <= BITE_TRIGGER_RANGE) {
            if (!canUseAiAbility(RaevyxAbilities.RAEVYX_BITE, false)) {
                return;
            }
            if (startAiAbility(RaevyxAbilities.RAEVYX_BITE, false, 20, 20, 0, 18)) {
                attackCooldown = 20;
            }
        } else if (distance >= beamMinRange && distance <= beamMaxRange && beamCooldown <= 0) {
            if (!canUseAiAbility(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM, true)) {
                return;
            }
            if (startAiAbility(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM, true, 60, BEAM_COOLDOWN_TICKS, 160, 80)) {
                attackCooldown = 60;
                beamCooldown = BEAM_COOLDOWN_TICKS;
            }
        }
    }


    private void chaseTarget(LivingEntity target) {
        DragonAsyncAirMovementHelper.chasePredictedTarget(
                dragon,
                target,
                5.0D,
                0.5D,
                0.15D,
                0.5D,
                DIRECT_CHASE_SPEED
        );
    }

    private void maintainCombatPosition(LivingEntity target) {
        if (repositionCooldown > 0) {
            return;
        }

        double targetY = target.getY() + target.getBbHeight() * 0.5D;
        double angle = (dragon.tickCount * 0.05) % (Math.PI * 2);
        double offsetX = Math.cos(angle) * ENGAGEMENT_DISTANCE;
        double offsetZ = Math.sin(angle) * ENGAGEMENT_DISTANCE;
        double posX = target.getX() + offsetX;
        double posZ = target.getZ() + offsetZ;
        double verticalOffset = Math.sin(dragon.tickCount * 0.1) * 1.0;
        DragonAsyncAirMovementHelper.moveToward(
                dragon,
                new Vec3(posX, targetY + verticalOffset, posZ),
                1.0D
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
        if (dist < 1.0E-4) {
            return;
        }

        Vec3 dir = toTarget.scale(1.0 / dist);
        Vec3 desired = new Vec3(target.getX(), targetY, target.getZ()).subtract(dir.scale(BITE_APPROACH_DISTANCE));

        double speed = dist > BITE_APPROACH_DISTANCE ? 1.2 : 0.6;
        DragonAsyncAirMovementHelper.moveToward(dragon, desired, speed);
    }

    private boolean isTargetAirborne(LivingEntity target) {
        if (target.onGround()) {
            return false;
        }

        if (target.isPassenger() && target.getVehicle() != null) {
            return true;
        }
        double groundY = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target.blockPosition()).getY();
        if (target.getY() - groundY > 8.0) {
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
        DragonLandingHelper.tryBeginAggroLanding(dragon, dragon.getTarget(), 1.6D);
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

    private boolean startAiAbility(DragonAbilityType<?, ?> abilityType,
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
}
