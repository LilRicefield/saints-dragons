package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.common.registry.raevyx.RaevyxAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonLandingHelper;
import com.leon.saintsdragons.server.ai.goals.base.DragonDirectAirCombatMovementHelper;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RaevyxAirCombatGoal extends Goal {
    private final Raevyx dragon;
    private static final double FLIGHT_ACCEL = 0.12D;
    private static final double FLIGHT_DRAG = 0.94D;
    private static final double DIRECT_CHASE_SPEED = 6.0D;

    private static final double BITE_TRIGGER_RANGE = 7.0;
    private static final double ENGAGEMENT_DISTANCE = 30.0;
    private static final double BITE_APPROACH_DISTANCE = 10.0;

    private final double beamMinRange = 20.0;
    private final double beamMaxRange = 70.0;
    private int attackCooldown = 0;
    private int repositionCooldown = 0;
    private int beamCooldown = 0;
    private static final int BEAM_COOLDOWN_TICKS = 2400; // 2 minutes

    // Emergency landing mechanic
    private int shotFromBelowCounter = 0;
    private static final int SHOT_FROM_BELOW_THRESHOLD = 3; // Land after 3 hits from below
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

        // Don't attack creative/spectator players
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

        // If target is grounded, only stay in this goal when the dragon itself is already airborne
        // so this goal can own the aggro-landing handoff.
        if (!targetAirborne && !dragonAirborne) {
            return false;
        }

        // If target is airborne but dragon is grounded, check if we should take off
        if (targetAirborne && !dragonAirborne) {
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

        // Keep goal active while using abilities
        if (dragon.isAbilityActive(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM)
            || dragon.isAbilityActive(RaevyxAbilities.RAEVYX_BITE)) {
            return true;
        }

        // Stop if target lands (switch to ground combat)
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

        // Don't call setLanding() - it triggers setTakeoff(true) which causes animation issues
        // Just clear flying/takeoff and let natural landing occur
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

        // Force clear takeoff flag if airborne and flying - don't let it stick
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

        // Check for emergency landing (shot from below)
        checkEmergencyLanding(target);

        double distance = dragon.distanceTo(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        // Attack logic based on distance
        if (distance <= BITE_TRIGGER_RANGE && hasLineOfSight) {
            // Close range - bite attack
            if (!isCurrentlyAttacking()) {
                tryAttack(target, distance);
            }
            // Maintain position for bite
            maintainBitePosition(target);
        } else if (distance >= beamMinRange && distance <= beamMaxRange && hasLineOfSight && beamCooldown <= 0) {
            // Medium-long range - lightning beam
            if (!isCurrentlyAttacking()) {
                tryAttack(target, distance);
            }
            // Hold position while firing beam
            if (dragon.isAbilityActive(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM)) {
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
        return dragon.isAbilityActive(RaevyxAbilities.RAEVYX_BITE)
            || dragon.isAbilityActive(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM);
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

        if (distance <= BITE_TRIGGER_RANGE) {
            // Close range - bite attack
            if (!canUseAiAbility(RaevyxAbilities.RAEVYX_BITE, false)) {
                return;
            }
            dragon.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_BITE);
            dragon.getAiCombatPacing().recordUse(RaevyxAbilities.RAEVYX_BITE, 20, 20, false, 0, 18);
            attackCooldown = 20;
        } else if (distance >= beamMinRange && distance <= beamMaxRange && beamCooldown <= 0) {
            // Medium-long range - lightning beam (smart tracking)
            if (!canUseAiAbility(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM, true)) {
                return;
            }
            dragon.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM);
            dragon.getAiCombatPacing().recordUse(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM, 60, BEAM_COOLDOWN_TICKS, true, 160, 80);
            attackCooldown = 60; // Longer cooldown after beam
            beamCooldown = BEAM_COOLDOWN_TICKS; // 2 minute cooldown for AI beam in air
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

    /**
     * Maintain a tight position for bite attempts.
     */
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
        DragonDirectAirCombatMovementHelper.flyToward(dragon, desired, speed, FLIGHT_ACCEL, FLIGHT_DRAG);
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
        DragonLandingHelper.tryBeginAggroLanding(dragon, dragon.getTarget(), 1.6D);
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
     * Check if dragon is allowed to take flight
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
