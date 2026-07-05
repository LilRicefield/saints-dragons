package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonAirCombatHelper;
import com.leon.saintsdragons.server.ai.goals.base.DragonAsyncAirMovementHelper;
import com.leon.saintsdragons.server.ai.goals.base.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBeamAbility;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RaevyxAirCombatGoal extends Goal {
    private final Raevyx dragon;
    private static final double CHASE_SPEED = 6.0D;
    private static final double DIVE_CHASE_SPEED = 7.5D;
    private static final double DIVE_CHASE_MIN_HEIGHT_ADVANTAGE = 7.0D;
    private static final double DIVE_CHASE_MAX_HORIZONTAL_DISTANCE = 42.0D;
    private static final double BITE_TRIGGER_RANGE = 7.0;
    private static final double ENGAGEMENT_DISTANCE = 30.0;
    private static final double BITE_APPROACH_DISTANCE = 3.5D;
    private static final double BEAM_MIN_RANGE = 20.0D;
    private static final double BEAM_MAX_RANGE = 70.0D;
    private static final double TAKEOFF_CHASE_MIN_HEIGHT_ABOVE_GROUND = 8.0D;
    private static final double TAKEOFF_CHASE_MIN_HEIGHT_ABOVE_DRAGON = 5.0D;
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

        boolean dragonAirborne = dragon.isAerial();
        boolean targetAirborne = isTargetAirborne(target);
        if (!targetAirborne && !dragonAirborne) {
            return false;
        }

        if (targetAirborne && !dragonAirborne) {
            if (!canTriggerFlight(target)) {
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

        if (dragon.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM)
            || dragon.isAbilityActive(ModAbilities.RAEVYX_BITE)) {
            return true;
        }

        if (!isTargetAirborne(target)) {
            if (dragon.isAerial()) {
                dragon.getAIMovement().setLandingWaypoint(target, 1.6D);
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
        attackCooldown = 0;
        repositionCooldown = 0;
        DragonAirCombatHelper.stopAirCombatAndLandWhenTargetLost(
                dragon,
                dragon.getTarget(),
                1.6D,
                this::isTargetAirborne,
                getMaxAggroDistanceSqr()
        );
    }

    @Override
    public void start() {
        DragonAirCombatHelper.startAirCombat(dragon, Raevyx.TAKEOFF_ANIMATION_TICKS);
    }

    @Override
    public void tick() {
        if (dragon.isLanding()) {
            if (!dragon.getAIMovement().isPathing()) {
                LivingEntity landingTarget = dragon.getTarget();
                if (landingTarget != null
                        && dragon.isTargetValid(landingTarget)
                        && !isTargetAirborne(landingTarget)
                        && dragon.getAIMovement().trySetLandingWaypoint(landingTarget, 1.6D)) {
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
        if (DragonAirCombatHelper.stopIfTargetInvalid(dragon, this::stop)) {
            return;
        }

        checkEmergencyLanding(target);
        if (dragon.isLanding()) {
            return;
        }

        if (!isTargetAirborne(target)) {
            if (dragon.isAerial()) {
                dragon.getAIMovement().trySetLandingWaypoint(target, 1.6D);
            }
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distance = dragon.distanceTo(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        if (distance <= BITE_TRIGGER_RANGE && hasLineOfSight) {
            if (!isCurrentlyAttacking()) {
                tryAttack(target, distance);
            }
            maintainBitePosition(target);
        } else if (!DragonTargetingHelper.isBiteOnlyPreyTarget(target)
                && !RaevyxBeamAbility.isAtAiBeamMercyThreshold(target)
                && distance >= BEAM_MIN_RANGE
                && distance <= BEAM_MAX_RANGE
                && hasLineOfSight
                && beamCooldown <= 0) {
            if (!isCurrentlyAttacking()) {
                tryAttack(target, distance);
            }
            if (dragon.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM)) {
                DragonAsyncAirMovementHelper.holdPosition(dragon);
            } else {
                maintainCombatPosition(target);
            }
        } else {
            chaseTarget(target);
        }
    }

    private boolean isCurrentlyAttacking() {
        return dragon.isAbilityActive(ModAbilities.RAEVYX_BITE)
            || dragon.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM);
    }


    private void tryAttack(LivingEntity target, double distance) {
        if (attackCooldown > 0 || isCurrentlyAttacking()) {
            return;
        }

        if (!dragon.getSensing().hasLineOfSight(target)) {
            return;
        }

        if (distance <= BITE_TRIGGER_RANGE) {
            if (!canUseAiAbility(ModAbilities.RAEVYX_BITE, false)) {
                return;
            }
            if (startAiAbility(ModAbilities.RAEVYX_BITE, false, 20, 20, 0, 18)) {
                attackCooldown = 20;
            }
        } else if (!DragonTargetingHelper.isBiteOnlyPreyTarget(target)
                && !RaevyxBeamAbility.isAtAiBeamMercyThreshold(target)
                && distance >= BEAM_MIN_RANGE
                && distance <= BEAM_MAX_RANGE
                && beamCooldown <= 0) {
            if (!canUseAiAbility(ModAbilities.RAEVYX_LIGHTNING_BEAM, true)) {
                return;
            }
            if (startAiAbility(ModAbilities.RAEVYX_LIGHTNING_BEAM, true, 60, BEAM_COOLDOWN_TICKS, 160, 80)) {
                attackCooldown = 60;
                beamCooldown = BEAM_COOLDOWN_TICKS;
            }
        }
    }


    private void chaseTarget(LivingEntity target) {
        if (shouldDiveChase(target)) {
            DragonAirCombatHelper.chasePredicted(dragon, target, 3.0D, -0.25D, 0.08D, 0.12D, DIVE_CHASE_SPEED);
            return;
        }
        DragonAirCombatHelper.chasePredicted(dragon, target, 5.0D, 0.5D, 0.15D, 0.5D, CHASE_SPEED);
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
        DragonAirCombatHelper.holdMeleePosition(dragon, target, 0.0D, BITE_APPROACH_DISTANCE, 1.2D, 0.6D);
    }

    private boolean isTargetAirborne(LivingEntity target) {
        return DragonAirCombatHelper.isTargetAirborne(dragon, target, 8.0D);
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
        dragon.getAIMovement().trySetLandingWaypoint(dragon.getTarget(), 1.6D);
        shotFromBelowCounter = 0;
    }

    private double getMaxAggroDistanceSqr() {
        return DragonAirCombatHelper.maxAggroDistanceSqr(dragon, 64.0D);
    }

    private boolean canTriggerFlight(LivingEntity target) {
        return DragonAirCombatHelper.canTriggerAiFlightForTarget(
                dragon,
                target,
                TAKEOFF_CHASE_MIN_HEIGHT_ABOVE_GROUND,
                TAKEOFF_CHASE_MIN_HEIGHT_ABOVE_DRAGON
        );
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
