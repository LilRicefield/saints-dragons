package com.leon.saintsdragons.server.ai.goals.ignivorus;

import com.leon.saintsdragons.common.registry.ignivorus.IgnivorusAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonAggroLandingHelper;
import com.leon.saintsdragons.server.ai.goals.base.DragonDirectAirCombatMovementHelper;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;


public class IgnivorusAirCombatGoal extends Goal {
    private static final double FLIGHT_ACCEL = 0.12D;
    private static final double FLIGHT_DRAG = 0.94D;
    private static final double DIRECT_CHASE_SPEED = 3.75D;

    private final Ignivorus dragon;

    // Combat ranges
    private static final double BITE_APPROACH_DISTANCE = 10.0D;
    private final double biteRange = 16.0;              // Close-range melee
    private final double fireBreathMinRange = 20.0;   // Fire breath at medium-long range
    private final double fireBreathMaxRange = 64.0;   // Max effective range

    // Flight positioning
    private static final double ENGAGEMENT_DISTANCE = 25.0; // Preferred combat distance

    private int attackCooldown = 0;
    private int repositionCooldown = 0;

    // Fire breath cooldown (AI only - 2 minute cooldown for air combat)
    private int breathCooldown = 0;
    private static final int BREATH_COOLDOWN_TICKS = 2400; // 2 minutes

    // Emergency landing mechanic
    private int shotFromBelowCounter = 0;
    private static final int SHOT_FROM_BELOW_THRESHOLD = 3; // Land after 3 hits from below
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

        // Don't attack creative/spectator players
        if (target instanceof net.minecraft.world.entity.player.Player player) {
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

        // Check if target is airborne (flying, riding flying mount, or significantly off ground)
        if (!isTargetAirborne(target)) {
            return false;
        }

        // If target is airborne but dragon is grounded, check if we should take off
        if (!dragon.isFlying() && !dragon.isHovering() && !dragon.isTakeoff() && !dragon.isLanding()) {
            // Only take off if we can actually fly
            if (!canTriggerFlight()) {
                return false;
            }
            // Target is airborne and we can fly - activate to trigger takeoff
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

        // Stop attacking if player switches to creative/spectator
        if (target instanceof net.minecraft.world.entity.player.Player player) {
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

        // Keep goal active while using abilities
        if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_BITE)) {
            return true;
        }

        // Stop if target lands (switch to ground combat)
        if (!isTargetAirborne(target)) {
            if (dragon.isFlying() || dragon.isHovering()) {
                DragonAggroLandingHelper.beginAggroLanding(dragon, target, 1.0D);
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

        // Don't call setLanding() if it triggers setTakeoff() - just clear flight flags
        if (target != null
                && dragon.isTargetValid(target)
                && !isTargetAirborne(target)
                && (dragon.isFlying() || dragon.isHovering())
                && !dragon.isLanding()) {
            DragonAggroLandingHelper.tryBeginAggroLanding(dragon, target, 1.0D);
        }
    }

    @Override
    public void start() {
        dragon.setAggressive(true);

        // Use the same takeoff path as other Ignivorus flight states so physics/flags stay coherent.
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
                        && DragonAggroLandingHelper.tryBeginAggroLanding(dragon, landingTarget, 1.0D)) {
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

        // Transition from takeoff to flying once airborne
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

        // Check for emergency landing (shot from below)
        checkEmergencyLanding(target);

        double distance = dragon.distanceTo(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        // Attack logic based on distance
        if (distance <= biteRange && hasLineOfSight) {
            // Close range - bite attack
            if (!isCurrentlyAttacking()) {
                tryAttack(target, distance);
            }
            // Maintain position for bite
            maintainBitePosition(target);
        } else if (distance >= fireBreathMinRange && distance <= fireBreathMaxRange && hasLineOfSight && breathCooldown <= 0) {
            // Medium-long range - fire breath
            if (!isCurrentlyAttacking()) {
                tryAttack(target, distance);
            }
            // Hold position while breathing fire
            if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)) {
                DragonDirectAirCombatMovementHelper.holdPosition(dragon, FLIGHT_DRAG);
            } else {
                maintainCombatPosition(target);
            }
        } else {
            // Out of range or no line of sight - chase target
            chaseTarget(target);
        }
    }

    /**
     * Check if dragon is currently executing an attack ability
     */
    private boolean isCurrentlyAttacking() {
        return dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_BITE)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)
            || dragon.isLeaping()
            || dragon.isLeapImpactRecovering();
    }

    /**
     * Try to attack target based on distance
     */
    private void tryAttack(LivingEntity target, double distance) {
        if (attackCooldown > 0 || isCurrentlyAttacking()) {
            return;
        }

        if (!dragon.getSensing().hasLineOfSight(target)) {
            return;
        }

        if (distance <= biteRange) {
            // Close range - bite attack
            if (!canUseAiAbility(IgnivorusAbilities.IGNIVORUS_BITE, false)) {
                return;
            }
            dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_BITE);
            dragon.getAiCombatPacing().recordUse(IgnivorusAbilities.IGNIVORUS_BITE, 30, 30, false, 0, 24);
            attackCooldown = 30;
        } else if (distance >= fireBreathMinRange && distance <= fireBreathMaxRange && breathCooldown <= 0) {
            // Medium-long range - fire breath
            if (!canUseAiAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH, true)) {
                return;
            }
            dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
            dragon.getAiCombatPacing().recordUse(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH, 60, BREATH_COOLDOWN_TICKS, true, 180, 80);
            attackCooldown = 60; // Longer cooldown after breath
            breathCooldown = BREATH_COOLDOWN_TICKS; // 2 minute cooldown for AI breath in air
        }
    }

    /**
     * Chase target in 3D space using flight controls (based on FollowOwnerGoal pattern)
     */
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

    /**
     * Maintain combat position (circle or hold distance)
     */
    private void maintainCombatPosition(LivingEntity target) {
        if (repositionCooldown > 0) {
            return;
        }

        double distance = dragon.distanceTo(target);
        double targetY = target.getY() + target.getBbHeight() * 0.5D;

        // Get target's look vector for positioning
        Vec3 targetLook = target.getLookAngle();

        // Position to the side for strafing (alternate sides based on time)
        double angle = (dragon.tickCount * 0.05) % (Math.PI * 2);
        double offsetX = Math.cos(angle) * ENGAGEMENT_DISTANCE;
        double offsetZ = Math.sin(angle) * ENGAGEMENT_DISTANCE;

        double posX = target.getX() + offsetX;
        double posZ = target.getZ() + offsetZ;

        // Add vertical variation
        double verticalOffset = Math.sin(dragon.tickCount * 0.1) * 1.0;

        DragonDirectAirCombatMovementHelper.flyToward(
                dragon,
                new Vec3(posX, targetY + verticalOffset, posZ),
                1.0D,
                FLIGHT_ACCEL,
                FLIGHT_DRAG
        );

        repositionCooldown = 20; // Reposition every second
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

    /**
     * Check if target is airborne (flying, riding flying mount, or off ground)
     */
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
        double groundY = dragon.level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, target.blockPosition()).getY();
        double heightAboveGround = target.getY() - groundY;
        if (heightAboveGround > Math.max(2.5D, target.getBbHeight() * 0.75D)) {
            return true;
        }

        return false;
    }

    /**
     * Check if dragon is being shot from below - emergency landing trigger
     */
    private void checkEmergencyLanding(LivingEntity target) {
        long currentTick = dragon.level().getGameTime();

        // Check if we took damage recently
        if (dragon.hurtTime > 0 && currentTick != lastDamageTick) {
            lastDamageTick = currentTick;

            // Check if target is below us
            if (target.getY() < dragon.getY() - 5.0) {
                shotFromBelowCounter++;

                // Trigger emergency landing if hit threshold
                if (shotFromBelowCounter >= SHOT_FROM_BELOW_THRESHOLD) {
                    triggerEmergencyLanding();
                }
            }
        }

        // Reset counter if we haven't been hit in a while (100 ticks = 5 seconds)
        if (currentTick - lastDamageTick > 100) {
            shotFromBelowCounter = 0;
        }
    }

    /**
     * Force dragon to land (shot from below)
     */
    private void triggerEmergencyLanding() {
        DragonAggroLandingHelper.tryBeginAggroLanding(dragon, dragon.getTarget(), 1.0D);
        shotFromBelowCounter = 0; // Reset counter
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = this.dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 64.0D; // Larger range for air combat
        }
        return followRange * followRange;
    }

    /**
     * Check if dragon is allowed to take flight (from FollowOwnerGoal pattern)
     */
    private boolean canTriggerFlight() {
        return !dragon.isOrderedToSit() &&
                !dragon.isBaby() &&
                (dragon.onGround() || dragon.isInWater()) &&
                dragon.getPassengers().isEmpty() &&
                dragon.getControllingPassenger() == null &&
                dragon.getActiveAbility() == null; // Don't interrupt abilities
    }

    private boolean canUseAiAbility(com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType) && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

}
