package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonAirCombatHelper;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CindervaneAirCombatGoal extends Goal {
    private static final int LOST_SIGHT_LANDING_TICKS = 30;
    private static final double BITE_TRIGGER_RANGE = 6.0D;
    private static final double BITE_APPROACH_DISTANCE = 3.5D;
    private static final double CHASE_SPEED = 2.0D;
    private static final double LANDING_SPEED = 2.2D;
    private static final double FIRE_BODY_ACTIVATION_RANGE = 8.0D;
    private static final double TAKEOFF_CHASE_MIN_HEIGHT_ABOVE_GROUND = 8.0D;
    private static final double TAKEOFF_CHASE_MIN_HEIGHT_ABOVE_DRAGON = 5.0D;
    private final Cindervane amphithere;
    private int attackCooldown = 0;
    private int fireBodyCheckCooldown = 0;
    private int lostSightTicks = 0;
    public CindervaneAirCombatGoal(Cindervane amphithere) {
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
        if (amphithere.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }

        boolean dragonAirborne = amphithere.isAerial();
        boolean targetAirborne = isTargetAirborne(target);

        if (!targetAirborne && !dragonAirborne) {
            return false;
        }

        if (targetAirborne && !dragonAirborne) {
            if (!amphithere.canTakeoff()
                    || !DragonAirCombatHelper.canTriggerAiFlightForTarget(
                    amphithere,
                    target,
                    TAKEOFF_CHASE_MIN_HEIGHT_ABOVE_GROUND,
                    TAKEOFF_CHASE_MIN_HEIGHT_ABOVE_DRAGON)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (amphithere.isLanding()) {
            return !amphithere.onGround();
        }

        LivingEntity target = amphithere.getTarget();
        if (!amphithere.isTargetValid(target)) {
            return false;
        }
        if (amphithere.isVehicle() || amphithere.isOrderedToSit()) {
            return false;
        }
        if (amphithere.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }

        boolean dragonAirborne = amphithere.isAerial();
        boolean targetAirborne = isTargetAirborne(target);
        return dragonAirborne || targetAirborne;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        attackCooldown = 0;
        lostSightTicks = 0;
        DragonAirCombatHelper.startAirCombat(amphithere, Cindervane.TAKEOFF_ANIMATION_TICKS);
    }

    @Override
    public void stop() {
        attackCooldown = 0;
        lostSightTicks = 0;
        deactivateFireBodyIfActive();
        DragonAirCombatHelper.stopAirCombatAndLandWhenTargetLost(
                amphithere,
                amphithere.getTarget(),
                LANDING_SPEED,
                this::isTargetAirborne,
                getMaxAggroDistanceSqr()
        );
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
        if (DragonAirCombatHelper.stopIfTargetInvalid(amphithere, this::stop)) {
            return;
        }

        if (amphithere.isLanding()) {
            if (!amphithere.getAIMovement().isPathing()) {
                if (!isTargetAirborne(target) && amphithere.getAIMovement().trySetLandingWaypoint(target, LANDING_SPEED)) {
                    return;
                }
                amphithere.setLanding(false);
            }
            return;
        }

        if (!isTargetAirborne(target)) {
            if (amphithere.isAerial()) {
                amphithere.getAIMovement().trySetLandingWaypoint(target, LANDING_SPEED);
            }
            return;
        }

        amphithere.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distance = amphithere.distanceTo(target);
        boolean hasLineOfSight = amphithere.getSensing().hasLineOfSight(target);
        lostSightTicks = hasLineOfSight ? 0 : lostSightTicks + 1;

        if (!hasLineOfSight && lostSightTicks >= LOST_SIGHT_LANDING_TICKS) {
            if (amphithere.isAerial() && !amphithere.isLanding()) {
                if (amphithere.getAIMovement().trySetLandingWaypoint(target, LANDING_SPEED)) {
                    return;
                }
                amphithere.setHovering(false);
                amphithere.setTakeoff(false);
            }
        }

        if (distance <= BITE_TRIGGER_RANGE && hasLineOfSight) {
            maintainBitePosition(target);
            tryPerformBite(target);
        } else {
            chaseTarget(target);
        }

        handleFireBodyActivation(target);
    }

    private void chaseTarget(LivingEntity target) {
        DragonAirCombatHelper.chasePredicted(amphithere, target, 4.0D, 0.5D, 0.15D, 0.35D, CHASE_SPEED);
    }

    private void maintainBitePosition(LivingEntity target) {
        DragonAirCombatHelper.holdMeleePosition(amphithere, target, 0.0D, BITE_APPROACH_DISTANCE, 1.2D, 0.7D);
    }

    private void tryPerformBite(LivingEntity target) {
        if (attackCooldown > 0 || amphithere.getAiCombatPacing().getCadenceCooldownTicks() > 0 || amphithere.isAbilityActive(ModAbilities.CINDERVANE_BITE)) {
            return;
        }
        if (!amphithere.getSensing().hasLineOfSight(target)) {
            return;
        }
        if (!amphithere.combatManager.canStart(ModAbilities.CINDERVANE_BITE)
                || !amphithere.getAiCombatPacing().canUse(ModAbilities.CINDERVANE_BITE, false)) {
            return;
        }

        amphithere.combatManager.tryUseAbility(ModAbilities.CINDERVANE_BITE);
        amphithere.getAiCombatPacing().recordUse(ModAbilities.CINDERVANE_BITE, 40, 40, false, 0, 28);
        attackCooldown = 40;
    }

    private void handleFireBodyActivation(LivingEntity target) {
        if (fireBodyCheckCooldown > 0 || amphithere.isVehicle() || amphithere.isInWaterOrBubble()) {
            return;
        }

        boolean fireBodyActive = amphithere.isAbilityActive(ModAbilities.CINDERVANE_FIRE_BODY);
        double distanceToTarget = amphithere.distanceTo(target);

        if (!fireBodyActive && distanceToTarget < FIRE_BODY_ACTIVATION_RANGE) {
            amphithere.combatManager.tryUseAbility(ModAbilities.CINDERVANE_FIRE_BODY);
            fireBodyCheckCooldown = 40;
        } else if (fireBodyActive && distanceToTarget > FIRE_BODY_ACTIVATION_RANGE * 1.5D) {
            amphithere.forceEndAbility(ModAbilities.CINDERVANE_FIRE_BODY);
            fireBodyCheckCooldown = 40;
        }
    }

    private void deactivateFireBodyIfActive() {
        if (!amphithere.isVehicle() && amphithere.isAbilityActive(ModAbilities.CINDERVANE_FIRE_BODY)) {
            amphithere.forceEndAbility(ModAbilities.CINDERVANE_FIRE_BODY);
        }
    }

    private boolean isTargetAirborne(LivingEntity target) {
        return DragonAirCombatHelper.isTargetAirborne(amphithere, target, 2.0D);
    }

    private double getMaxAggroDistanceSqr() {
        return DragonAirCombatHelper.maxAggroDistanceSqr(amphithere, 16.0D);
    }
}
