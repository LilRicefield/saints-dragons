package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class DragonAirCombatHelper {
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
                && dragon.distanceToSqr(target) <= maxAggroDistanceSqr(dragon, fallbackFollowRange);
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

    public static void startOrResumeFlight(RideableDragonBase dragon, int takeoffTicks) {
        if (!(dragon instanceof DragonFlightCapable flightCapable)) {
            return;
        }
        if (dragon.isGroundedForAi()) {
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
            DragonLandingHelper.tryBeginAggroLanding(dragon, target, landingSpeed);
            return;
        }

        if (clearInvalidOrMissingTarget && (target == null || !dragon.isTargetValid(target))) {
            dragon.setTarget(null);
            if (dragon.isAerial() && !dragon.isLanding()) {
                DragonLandingHelper.tryBeginAggroLanding(dragon, null, landingSpeed);
            }
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
        if (!dragon.getNavigation().isInProgress()) {
            if (target != null
                    && dragon.isTargetValid(target)
                    && !isTargetAirborne(dragon, target)
                    && DragonLandingHelper.tryBeginAggroLanding(dragon, target, landingSpeed)) {
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
            DragonLandingHelper.tryBeginAggroLanding(dragon, target, landingSpeed);
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

    public static void holdMeleePosition(RideableFlyingDragon dragon, LivingEntity target,
                                         double targetHeightOffset, double approachDistance,
                                         double farSpeed, double nearSpeed) {
        double targetY = target.getY() + target.getBbHeight() * 0.5D + targetHeightOffset;
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
        Vec3 desired = new Vec3(target.getX(), targetY, target.getZ()).subtract(dir.scale(approachDistance));
        DragonAsyncAirMovementHelper.moveToward(dragon, desired, dist > approachDistance ? farSpeed : nearSpeed);
    }

    public static boolean isTargetAirborne(RideableDragonBase dragon, LivingEntity target) {
        return isTargetAirborne(dragon, target, 8.0D);
    }

    public static boolean isTargetAirborne(RideableDragonBase dragon, LivingEntity target, double minHeightAboveGround) {
        if (target == null || target.onGround()) {
            return false;
        }
        if (target.getVehicle() instanceof LivingEntity vehicle) {
            return !vehicle.onGround();
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
