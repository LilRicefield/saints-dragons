package com.leon.saintsdragons.server.ai.goals.ignivorus;

import com.leon.saintsdragons.common.registry.ignivorus.IgnivorusAbilities;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Air-to-air combat goal for Ignivorus - handles flying targets (players riding dragons, etc).
 *
 * Features:
 * - 3D chase movement using flight controls
 * - Fire breath at range (longer duration in air)
 * - Bite attacks up close
 * - Emergency landing when shot from below
 */
public class IgnivorusAirCombatGoal extends Goal {
    private final Ignivorus dragon;

    // Combat ranges
    private final double biteRange = 16.0;              // Close-range melee
    private final double fireBreathMinRange = 20.0;   // Fire breath at medium-long range
    private final double fireBreathMaxRange = 64.0;   // Max effective range

    // Flight positioning
    private static final double HOVER_HEIGHT_OFFSET = 2.0; // Stay slightly above target
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
            return false;
        }

        // Stop if we are actually landing (not just touching ground briefly)
        if (dragon.isLanding()) {
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

        // Don't call setLanding() if it triggers setTakeoff() - just clear flight flags
        if (dragon.isFlying() || dragon.isHovering()) {
            dragon.setFlying(false);
            dragon.setTakeoff(false);
            dragon.setHovering(false);
        }
    }

    @Override
    public void start() {
        dragon.setAggressive(true);

        // If grounded, trigger takeoff sequence
        // Only set takeoff if truly grounded (not already flying/hovering)
        if (dragon.onGround() && !dragon.isFlying() && !dragon.isHovering() && !dragon.isTakeoff() && !dragon.isLanding()) {
            // Set both flying and takeoff - physics needs flying=true to actually fly
            // tick() will clear takeoff flag once airborne
            dragon.setFlying(true);
            dragon.setTakeoff(true);
            dragon.setLanding(false);
            dragon.setHovering(false);
        } else if (dragon.isFlying() || dragon.isHovering()) {
            // Already airborne - just ensure takeoff flag is cleared and set flying
            dragon.setTakeoff(false);
            dragon.setFlying(true);
            dragon.setLanding(false);
        }
    }

    @Override
    public void tick() {
        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()) {
            dragon.getNavigation().stop();
            return;
        }

        // Transition from takeoff to flying once airborne
        if (dragon.isTakeoff() && !dragon.onGround()) {
            dragon.setTakeoff(false);
            dragon.setFlying(true);
            dragon.setHovering(false);
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
            maintainCombatPosition(target);
        } else if (distance >= fireBreathMinRange && distance <= fireBreathMaxRange && hasLineOfSight && breathCooldown <= 0) {
            // Medium-long range - fire breath
            if (!isCurrentlyAttacking()) {
                tryAttack(target, distance);
            }
            // Hold position while breathing fire
            if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)) {
                dragon.getMoveControl().setWantedPosition(
                    dragon.getX(),
                    dragon.getY(),
                    dragon.getZ(),
                    0.5 // Slow movement while breathing
                );
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
            dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_BITE);
            attackCooldown = 30;
        } else if (distance >= fireBreathMinRange && distance <= fireBreathMaxRange && breathCooldown <= 0) {
            // Medium-long range - fire breath
            dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
            attackCooldown = 60; // Longer cooldown after breath
            breathCooldown = BREATH_COOLDOWN_TICKS; // 2 minute cooldown for AI breath in air
        }
    }

    /**
     * Chase target in 3D space using flight controls (based on FollowOwnerGoal pattern)
     */
    private void chaseTarget(LivingEntity target) {
        // Calculate target position - slightly above and closing distance
        double targetY = target.getY() + target.getBbHeight() + HOVER_HEIGHT_OFFSET;

        // Get direction to target for positioning
        Vec3 toTarget = new Vec3(
            target.getX() - dragon.getX(),
            targetY - dragon.getY(),
            target.getZ() - dragon.getZ()
        ).normalize();

        // Calculate intercept position (predict where target will be)
        Vec3 targetVelocity = target.getDeltaMovement();
        double targetX = target.getX() + targetVelocity.x * 5.0; // Predict 5 ticks ahead
        double targetZ = target.getZ() + targetVelocity.z * 5.0;

        // Add slight vertical bobbing for natural flight
        double verticalOffset = Math.sin(dragon.tickCount * 0.15) * 0.5;

        dragon.getMoveControl().setWantedPosition(
            targetX,
            targetY + verticalOffset,
            targetZ,
            3.5 // Aggressive chase speed
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
        double targetY = target.getY() + target.getBbHeight() + HOVER_HEIGHT_OFFSET;

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

        dragon.getMoveControl().setWantedPosition(
            posX,
            targetY + verticalOffset,
            posZ,
            1.0 // Moderate positioning speed
        );

        repositionCooldown = 20; // Reposition every second
    }

    /**
     * Check if target is airborne (flying, riding flying mount, or off ground)
     */
    private boolean isTargetAirborne(LivingEntity target) {
        // Check if target is on ground
        if (target.onGround()) {
            return false;
        }

        // Check if riding something (might be a flying dragon)
        if (target.isPassenger() && target.getVehicle() != null) {
            return true; // Assume mounted targets are valid air targets
        }

        // Check if significantly off ground (more than 8 blocks up for elytra/flight stability)
        // Increased from 3 to prevent low elytra gliding from triggering constant takeoff
        double groundY = dragon.level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, target.blockPosition()).getY();
        if (target.getY() - groundY > 8.0) {
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
        dragon.setLanding(true);
        dragon.setFlying(false);
        dragon.setHovering(true);
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
}
