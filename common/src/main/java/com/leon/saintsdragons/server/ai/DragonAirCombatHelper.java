package com.leon.saintsdragons.server.ai;

import com.leon.saintsdragons.server.ai.navigation.async.DragonAsyncAirMovementHelper;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class DragonAirCombatHelper {
    private static final double DEFAULT_TAKEOFF_TARGET_MIN_HEIGHT_ABOVE_GROUND = 8.0D;
    private static final double DEFAULT_TAKEOFF_TARGET_MIN_HEIGHT_ABOVE_DRAGON = 5.0D;

    private DragonAirCombatHelper() {
    }

    public static boolean isValidCombatTarget(RideableDragonBase dragon, LivingEntity target) {
        if (!dragon.isTargetValid(target)) {
            return false;
        }
        return !(target instanceof Player player) || (!player.isCreative() && !player.isSpectator());
    }

    public static boolean canUseAirCombat(RideableDragonBase dragon, LivingEntity target, double fallbackFollowRange) {
        return dragon.canFly()
                && isValidCombatTarget(dragon, target)
                && !dragon.isVehicle()
                && !dragon.isOrderedToSit()
                && dragon.distanceToSqr(DragonTargetingHelper.movementAnchor(target))
                <= maxAggroDistanceSqr(dragon, fallbackFollowRange);
    }

    public static boolean canEngageAirborneTarget(RideableDragonBase dragon,
                                                   LivingEntity target,
                                                   DragonAirCombatSettings settings) {
        return canEngageAirborneTarget(dragon, target, settings, settings.targetAirborneHeight());
    }

    public static boolean canEngageAirborneTarget(RideableDragonBase dragon,
                                                   LivingEntity target,
                                                   DragonAirCombatSettings settings,
                                                   double targetAirborneHeight) {
        if (dragon.isPassenger()
                || !canUseAirCombat(dragon, target, settings.fallbackFollowRange())
                || !isTargetAirborne(dragon, target, targetAirborneHeight)) {
            return false;
        }
        return dragon.isAerial()
                || (dragon.canTakeoff() && canTriggerAiFlightForTarget(
                dragon,
                target,
                settings.takeoffTargetMinHeightAboveGround(),
                settings.takeoffTargetMinHeightAboveDragon()
        ));
    }

    public static boolean canTriggerAiFlight(RideableDragonBase dragon) {
        return dragon.canFly()
                && !dragon.isOrderedToSit()
                && !dragon.isBaby()
                && (dragon.onGround() || dragon.isInWater())
                && dragon.getPassengers().isEmpty()
                && dragon.getControllingPassenger() == null
                && !dragon.isPassenger()
                && dragon.getActiveAbility() == null;
    }

    public static boolean canTriggerAiFlightForTarget(RideableDragonBase dragon, LivingEntity target) {
        return canTriggerAiFlightForTarget(
                dragon,
                target,
                DEFAULT_TAKEOFF_TARGET_MIN_HEIGHT_ABOVE_GROUND,
                DEFAULT_TAKEOFF_TARGET_MIN_HEIGHT_ABOVE_DRAGON
        );
    }

    public static boolean canTriggerAiFlightForTarget(RideableDragonBase dragon,
                                                      LivingEntity target,
                                                      double minHeightAboveGround,
                                                      double minHeightAboveDragon) {
        return canTriggerAiFlight(dragon)
                && isTargetHighEnoughForAiTakeoff(dragon, target, minHeightAboveGround, minHeightAboveDragon);
    }

    public static boolean isTargetHighEnoughForAiTakeoff(RideableDragonBase dragon,
                                                         LivingEntity target,
                                                         double minHeightAboveGround,
                                                         double minHeightAboveDragon) {
        if (target == null) {
            return false;
        }
        Entity movementAnchor = DragonTargetingHelper.movementAnchor(target);
        if (movementAnchor.onGround()) {
            return false;
        }
        double groundY = dragon.level()
                .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, movementAnchor.blockPosition())
                .getY();
        return movementAnchor.getY() - groundY > minHeightAboveGround
                && movementAnchor.getY() - dragon.getY() > minHeightAboveDragon;
    }

    public static void startOrResumeFlight(RideableDragonBase dragon, int takeoffTicks) {
        if (!(dragon instanceof DragonFlightCapable flightCapable)) {
            return;
        }
        if (dragon instanceof RideableFlyingDragon flyingDragon
                && (dragon.isInWaterOrBubble() || dragon.isInLava())) {
            flyingDragon.startAiWaterBreachTakeoffSequence(0.18D, takeoffTicks);
        } else if (dragon.isGroundedForAi()) {
            flightCapable.beginAiTakeoff(takeoffTicks);
        } else if (dragon.isFlying() || dragon.isHovering()) {
            flightCapable.beginAiFlight();
        }
    }

    public static void startAirCombat(RideableDragonBase dragon, int takeoffTicks) {
        dragon.setAggressive(true);
        startOrResumeFlight(dragon, takeoffTicks);
    }

    public static void stopAirCombat(RideableDragonBase dragon,
                                     LivingEntity target,
                                     double landingSpeed,
                                     TargetAirborneCheck targetAirborneCheck,
                                     boolean clearInvalidOrMissingTarget) {
        dragon.setAggressive(false);
        if (target != null
                && dragon.isTargetValid(target)
                && !targetAirborneCheck.isTargetAirborne(target)
                && dragon.isAerial()
                && !dragon.isLanding()) {
            dragon.getAIMovement().requestGroundTransition(
                    DragonTargetingHelper.livingMovementAnchor(target),
                    landingSpeed
            );
            return;
        }

        if (clearInvalidOrMissingTarget && (target == null || !dragon.isTargetValid(target))) {
            dragon.setTarget(null);
            if (dragon.isAerial() && !dragon.isLanding()) {
                dragon.getAIMovement().requestGroundTransition((LivingEntity) null, landingSpeed);
            }
        }
    }

    public static void stopAirCombatAndLandWhenTargetLost(RideableDragonBase dragon,
                                                          LivingEntity target,
                                                          double landingSpeed,
                                                          TargetAirborneCheck targetAirborneCheck,
                                                          double maxAggroDistanceSqr) {
        boolean validTarget = target != null && dragon.isTargetValid(target);
        boolean targetOutOfRange = validTarget
                && dragon.distanceToSqr(DragonTargetingHelper.movementAnchor(target)) > maxAggroDistanceSqr;
        if (!validTarget || targetOutOfRange) {
            dragon.setAggressive(false);
            dragon.setTarget(null);
            if (dragon.isAerial() && !dragon.isLanding()) {
                dragon.getAIMovement().requestGroundTransition((LivingEntity) null, landingSpeed);
            }
            return;
        }

        if (!targetAirborneCheck.isTargetAirborne(target) && dragon.isAerial() && !dragon.isLanding()) {
            dragon.getAIMovement().requestGroundTransition(
                    DragonTargetingHelper.livingMovementAnchor(target),
                    landingSpeed
            );
        }
    }

    public static boolean stopIfTargetInvalid(RideableDragonBase dragon, Runnable stopCombat) {
        if (dragon.isTargetValid(dragon.getTarget())) {
            return false;
        }
        dragon.setTarget(null);
        stopCombat.run();
        return true;
    }

    public static boolean handleLandingTick(RideableDragonBase dragon, LivingEntity target, double landingSpeed) {
        if (!dragon.isLanding()) {
            return false;
        }
        if (!dragon.getAIMovement().isPathing()) {
            if (target != null
                    && dragon.isTargetValid(target)
                    && !isTargetAirborne(dragon, target)
                    && dragon.getAIMovement().requestGroundTransition(
                            DragonTargetingHelper.livingMovementAnchor(target),
                            landingSpeed
                    )) {
                return true;
            }
            if (dragon instanceof DragonFlightCapable flightCapable) {
                flightCapable.setLanding(false);
            }
        }
        return true;
    }

    public static boolean landIfTargetGrounded(RideableDragonBase dragon, LivingEntity target, double landingSpeed) {
        if (target == null || isTargetAirborne(dragon, target)) {
            return false;
        }
        if (dragon.isAerial()) {
            dragon.getAIMovement().requestGroundTransition(
                    DragonTargetingHelper.livingMovementAnchor(target),
                    landingSpeed
            );
            return true;
        }
        return false;
    }

    public static void chasePredicted(RideableFlyingDragon dragon, LivingEntity target,
                                      double predictionTicks, double heightOffset,
                                      double leadScale, double verticalLeadScale, double speedScale) {
        DragonAsyncAirMovementHelper.chasePredictedTarget(
                dragon,
                target,
                predictionTicks,
                heightOffset,
                leadScale,
                verticalLeadScale,
                speedScale
        );
    }

    public static boolean shouldDiveChase(RideableFlyingDragon dragon,
                                          LivingEntity target,
                                          double minTargetHeightAboveGround,
                                          double minHeightAdvantage,
                                          double maxHorizontalDistance) {
        if (!dragon.isFlying() || dragon.isLanding() || !isTargetAirborne(dragon, target, minTargetHeightAboveGround)) {
            return false;
        }
        Entity movementAnchor = DragonTargetingHelper.movementAnchor(target);
        double heightAdvantage = dragon.getY()
                - (movementAnchor.getY() + movementAnchor.getBbHeight() * 0.5D);
        if (heightAdvantage < minHeightAdvantage) {
            return false;
        }
        double dx = movementAnchor.getX() - dragon.getX();
        double dz = movementAnchor.getZ() - dragon.getZ();
        return dx * dx + dz * dz <= maxHorizontalDistance * maxHorizontalDistance;
    }

    public static void holdMeleePosition(RideableFlyingDragon dragon, LivingEntity target,
                                         double targetHeightOffset, double approachDistance,
                                         double farSpeed, double nearSpeed) {
        Entity movementAnchor = DragonTargetingHelper.movementAnchor(target);
        double targetY = movementAnchor.getY()
                + movementAnchor.getBbHeight() * 0.5D
                + targetHeightOffset;
        Vec3 toTarget = new Vec3(
                movementAnchor.getX() - dragon.getX(),
                targetY - dragon.getY(),
                movementAnchor.getZ() - dragon.getZ()
        );
        double dist = toTarget.length();
        if (dist < 1.0E-4D) {
            return;
        }
        Vec3 dir = toTarget.scale(1.0D / dist);
        Vec3 desired = new Vec3(movementAnchor.getX(), targetY, movementAnchor.getZ())
                .subtract(dir.scale(approachDistance));
        DragonAsyncAirMovementHelper.moveToward(dragon, desired, dist > approachDistance ? farSpeed : nearSpeed);
    }

    public static boolean isTargetAirborne(RideableDragonBase dragon, LivingEntity target) {
        return isTargetAirborne(dragon, target, 8.0D);
    }

    public static boolean isTargetAirborne(RideableDragonBase dragon, LivingEntity target, double minHeightAboveGround) {
        if (target == null) {
            return false;
        }
        Entity movementAnchor = DragonTargetingHelper.movementAnchor(target);
        if (movementAnchor != target) {
            if (movementAnchor instanceof DragonFlightCapable flightCapable) {
                return flightCapable.isFlying()
                        || flightCapable.isTakeoff()
                        || flightCapable.isHovering()
                        || (flightCapable.isLanding() && !movementAnchor.onGround());
            }
            return !movementAnchor.onGround();
        }
        if (target.onGround()) {
            return false;
        }
        if (target instanceof Player player && player.isFallFlying()) {
            return true;
        }
        double groundY = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target.blockPosition()).getY();
        return target.getY() - groundY > minHeightAboveGround;
    }

    public static double maxAggroDistanceSqr(RideableDragonBase dragon, double fallbackFollowRange) {
        double followRange = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = fallbackFollowRange;
        }
        return followRange * followRange;
    }

    @FunctionalInterface
    public interface TargetAirborneCheck {
        boolean isTargetAirborne(LivingEntity target);
    }
}
