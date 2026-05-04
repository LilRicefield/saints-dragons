package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.common.registry.cindervane.CindervaneAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonAsyncAirMovementHelper;
import com.leon.saintsdragons.server.ai.goals.base.DragonLandingHelper;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CindervaneAirCombatGoal extends Goal {
    private static final int LOST_SIGHT_LANDING_TICKS = 30;
    private static final double BITE_TRIGGER_RANGE = 6.0D;
    private static final double BITE_APPROACH_DISTANCE = 3.5D;
    private static final double AIR_CHASE_SPEED = 4.0D;
    private static final double LANDING_SPEED = 2.2D;
    private static final double FIRE_BODY_ACTIVATION_RANGE = 8.0D;
    private final Cindervane amphithere;
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

        boolean dragonAirborne = amphithere.isFlying() || amphithere.isHovering() || amphithere.isTakeoff() || amphithere.isLanding();
        boolean targetAirborne = isTargetAirborne(target);

        if (!targetAirborne && !dragonAirborne) {
            return false;
        }

        if (targetAirborne && !dragonAirborne && !amphithere.canTakeoff()) {
            return false;
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

        boolean dragonAirborne = amphithere.isFlying() || amphithere.isHovering() || amphithere.isTakeoff();
        boolean targetAirborne = isTargetAirborne(target);
        return dragonAirborne || targetAirborne;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        lostSightTicks = 0;
        if (amphithere.onGround() && !amphithere.isFlying() && !amphithere.isHovering() && !amphithere.isTakeoff() && !amphithere.isLanding()) {
            amphithere.beginAiTakeoff(Cindervane.TAKEOFF_ANIMATION_TICKS);
        } else if (amphithere.isFlying() || amphithere.isHovering()) {
            amphithere.beginAiFlight();
        }
    }

    @Override
    public void stop() {
        lostSightTicks = 0;
        deactivateFireBodyIfActive();

        LivingEntity target = amphithere.getTarget();
        if (target != null
                && amphithere.isTargetValid(target)
                && !isTargetAirborne(target)
                && (amphithere.isFlying() || amphithere.isHovering())
                && !amphithere.isLanding()) {
            DragonLandingHelper.tryBeginAggroLanding(amphithere, target, LANDING_SPEED);
        }
    }

    @Override
    public void tick() {
        if (fireBodyCheckCooldown > 0) {
            fireBodyCheckCooldown--;
        }
        LivingEntity target = amphithere.getTarget();
        if (!amphithere.isTargetValid(target)) {
            amphithere.setTarget(null);
            stop();
            deactivateFireBodyIfActive();
            return;
        }

        if (amphithere.isLanding()) {
            if (!amphithere.getNavigation().isInProgress()) {
                if (!isTargetAirborne(target) && DragonLandingHelper.tryBeginAggroLanding(amphithere, target, LANDING_SPEED)) {
                    return;
                }
                amphithere.setLanding(false);
            }
            return;
        }

        if (!isTargetAirborne(target)) {
            if (amphithere.isFlying() || amphithere.isHovering() || amphithere.isTakeoff()) {
                DragonLandingHelper.tryBeginAggroLanding(amphithere, target, LANDING_SPEED);
            }
            return;
        }

        amphithere.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distance = amphithere.distanceTo(target);
        boolean hasLineOfSight = amphithere.getSensing().hasLineOfSight(target);
        lostSightTicks = hasLineOfSight ? 0 : lostSightTicks + 1;

        if (!hasLineOfSight && lostSightTicks >= LOST_SIGHT_LANDING_TICKS) {
            if ((amphithere.isFlying() || amphithere.isHovering() || amphithere.isTakeoff()) && !amphithere.isLanding()) {
                if (DragonLandingHelper.tryBeginAggroLanding(amphithere, target, LANDING_SPEED)) {
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
        DragonAsyncAirMovementHelper.chasePredictedTarget(
                amphithere,
                target,
                4.0D,
                0.5D,
                0.15D,
                0.35D,
                AIR_CHASE_SPEED
        );
    }

    private void maintainBitePosition(LivingEntity target) {
        double targetY = target.getY() + target.getBbHeight() * 0.5D;
        Vec3 toTarget = new Vec3(
                target.getX() - amphithere.getX(),
                targetY - amphithere.getY(),
                target.getZ() - amphithere.getZ()
        );

        double dist = toTarget.length();
        if (dist < 1.0E-4) {
            return;
        }

        Vec3 dir = toTarget.scale(1.0D / dist);
        Vec3 desired = new Vec3(target.getX(), targetY, target.getZ()).subtract(dir.scale(BITE_APPROACH_DISTANCE));
        double speed = dist > BITE_APPROACH_DISTANCE ? 1.2D : 0.7D;
        DragonAsyncAirMovementHelper.moveToward(amphithere, desired, speed);
    }

    private void tryPerformBite(LivingEntity target) {
        if (amphithere.getAiCombatPacing().getCadenceCooldownTicks() > 0 || amphithere.isAbilityActive(CindervaneAbilities.BITE)) {
            return;
        }
        if (!amphithere.getSensing().hasLineOfSight(target)) {
            return;
        }
        if (!amphithere.combatManager.canStart(CindervaneAbilities.BITE)
                || !amphithere.getAiCombatPacing().canUse(CindervaneAbilities.BITE, false)) {
            return;
        }

        amphithere.combatManager.tryUseAbility(CindervaneAbilities.BITE);
        amphithere.getAiCombatPacing().recordUse(CindervaneAbilities.BITE, 40, 40, false, 0, 28);
    }

    private void handleFireBodyActivation(LivingEntity target) {
        if (fireBodyCheckCooldown > 0 || amphithere.isVehicle() || amphithere.isInWaterOrBubble()) {
            return;
        }

        boolean fireBodyActive = amphithere.isAbilityActive(CindervaneAbilities.FIRE_BODY);
        double distanceToTarget = amphithere.distanceTo(target);

        if (!fireBodyActive && distanceToTarget < FIRE_BODY_ACTIVATION_RANGE) {
            amphithere.combatManager.tryUseAbility(CindervaneAbilities.FIRE_BODY);
            fireBodyCheckCooldown = 40;
        } else if (fireBodyActive && distanceToTarget > FIRE_BODY_ACTIVATION_RANGE * 1.5D) {
            amphithere.forceEndAbility(CindervaneAbilities.FIRE_BODY);
            fireBodyCheckCooldown = 40;
        }
    }

    private void deactivateFireBodyIfActive() {
        if (!amphithere.isVehicle() && amphithere.isAbilityActive(CindervaneAbilities.FIRE_BODY)) {
            amphithere.forceEndAbility(CindervaneAbilities.FIRE_BODY);
        }
    }

    private boolean isTargetAirborne(LivingEntity target) {
        if (target.onGround()) {
            return false;
        }
        if (target.getVehicle() instanceof LivingEntity vehicle) {
            return !vehicle.onGround();
        }
        return target.getY() - target.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getBlockX(), target.getBlockZ()) > 2.0D;
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = amphithere.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 16.0D;
        }
        return followRange * followRange;
    }
}
