package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public abstract class DragonAutonomousFlightGoal<T extends RideableFlyingDragon> extends Goal {
    protected final T dragon;
    protected final DragonFlightBehaviorProfile profile;
    private final double cruiseSpeed;
    private final double landingSpeed;
    private final int takeoffAnimationTicks;

    private Vec3 targetPosition;
    private int timeSinceTargetChange;
    private long lastLandingTime;
    private int decisionCooldown;

    protected DragonAutonomousFlightGoal(T dragon, DragonFlightBehaviorProfile profile,
                                         double cruiseSpeed, double landingSpeed, int takeoffAnimationTicks) {
        this.dragon = dragon;
        this.profile = profile;
        this.cruiseSpeed = cruiseSpeed;
        this.landingSpeed = landingSpeed;
        this.takeoffAnimationTicks = takeoffAnimationTicks;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!canUseAutonomousFlight()) {
            return false;
        }

        if (!dragon.isFlying() && dragon.level().getGameTime() - lastLandingTime < getLandingCooldownTicks()) {
            return false;
        }

        if (decisionCooldown > 0 && --decisionCooldown > 0) {
            return false;
        }

        boolean shouldFly = dragon.isOverStandardFlightDanger()
                || (dragon.isFlying() ? shouldKeepFlying() : dragon.hasStandardTakeoffClearance(getTakeoffClearanceHeight()) && shouldTakeOff());
        decisionCooldown = nextDecisionCooldown(getDecisionIntervalTicks());

        if (!shouldFly) {
            return false;
        }

        targetPosition = findCruiseTarget();
        return targetPosition != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (dragon.isLanding()) {
            return !dragon.onGround();
        }

        if (!canContinueAutonomousFlight()) {
            if (dragon.isFlying() && shouldLandWhenAutonomousFlightBlocked()) {
                beginLandingApproach();
                return true;
            }
            return false;
        }

        if (shouldLandNow()) {
            beginLandingApproach();
            return true;
        }

        return dragon.isFlying() && targetPosition != null && dragon.distanceToSqr(targetPosition) > 9.0D;
    }

    @Override
    public void start() {
        if (dragon.onGround() && !dragon.isFlying() && !dragon.isTakeoff() && !dragon.isLanding()) {
            beginAutonomousTakeoff();
        } else {
            dragon.beginAiFlight();
        }
        dragon.moveAiFlightTo(targetPosition, cruiseSpeed);
    }

    @Override
    public void tick() {
        timeSinceTargetChange++;

        if (dragon.isTakeoff() && dragon.isFlying() && !dragon.onGround()) {
            dragon.beginAiFlight();
        }

        if (dragon.isLanding()) {
            if (targetPosition == null) {
                beginLandingApproach();
            } else if (!dragon.getNavigation().isInProgress()) {
                dragon.moveAiFlightTo(targetPosition, landingSpeed);
            }
            return;
        }

        if (needsNewCruiseTarget()) {
            targetPosition = findCruiseTarget();
            timeSinceTargetChange = 0;
            dragon.moveAiFlightTo(targetPosition, cruiseSpeed);
        }
    }

    @Override
    public void stop() {
        targetPosition = null;
        timeSinceTargetChange = 0;
        dragon.getNavigation().stop();
        if (!dragon.isFlying()) {
            lastLandingTime = dragon.level().getGameTime();
        }
    }

    protected boolean canUseAutonomousFlight() {
        return !dragon.isBaby()
                && !dragon.isLanding()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleeping()
                && !dragon.isSleepingExiting();
    }

    protected boolean canContinueAutonomousFlight() {
        LivingEntity target = dragon.getTarget();
        return !dragon.isOrderedToSit()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isSleeping()
                && !dragon.isSleepingExiting()
                && (target == null || !target.isAlive());
    }

    protected boolean shouldLandWhenAutonomousFlightBlocked() {
        return false;
    }

    protected void beginAutonomousTakeoff() {
        dragon.beginAiTakeoff(takeoffAnimationTicks);
    }

    protected boolean shouldLandNow() {
        return dragon.isFlying() && !shouldKeepFlying() && !dragon.isOverStandardFlightDanger();
    }

    protected boolean shouldTakeOff() {
        return dragon.isOverStandardFlightDanger()
                || dragon.getRandom().nextInt(Math.max(1, getTakeoffRoll())) == 0;
    }

    protected boolean shouldKeepFlying() {
        return dragon.isOverStandardFlightDanger()
                || dragon.getRandom().nextInt(Math.max(1, getKeepFlyingRoll())) != 0;
    }

    protected Vec3 findCruiseTarget() {
        return dragon.findStandardAiFlightTarget(
                getCruiseTurnDegrees(),
                getCruiseMinRange(),
                getCruiseExtraRange(),
                getMaxHeightAboveGround(),
                dragon.isFlightControllerStuck()
        );
    }

    protected void beginLandingApproach() {
        Vec3 landingTarget = dragon.findStandardAiLandingTarget(null);
        if (landingTarget == null) {
            return;
        }
        targetPosition = landingTarget;
        dragon.beginAiLanding();
        dragon.moveAiFlightTo(landingTarget, landingSpeed);
    }

    private boolean needsNewCruiseTarget() {
        if (targetPosition == null) {
            return true;
        }
        if (dragon.distanceToSqr(targetPosition) < profile.targetReachedDistanceSq()) {
            return true;
        }
        if (dragon.isFlightControllerStuck()) {
            return true;
        }
        if (timeSinceTargetChange > profile.maxTargetAgeTicks()) {
            return true;
        }
        return dragon.tickCount % 20 == 0 && !dragon.isValidStandardFlightTarget(targetPosition);
    }

    protected int nextDecisionCooldown(int baseInterval) {
        int jitter = Math.max(1, baseInterval / 2);
        return baseInterval + dragon.getRandom().nextInt(jitter);
    }

    protected int getLandingCooldownTicks() {
        return profile.landingCooldownTicks();
    }

    protected int getDecisionIntervalTicks() {
        return profile.decisionIntervalClear();
    }

    protected int getTakeoffRoll() {
        return profile.takeoffRollClear();
    }

    protected int getKeepFlyingRoll() {
        return profile.keepFlyingRollClear();
    }

    protected int getTakeoffClearanceHeight() {
        return 10;
    }

    protected double getCruiseTurnDegrees() {
        return 180.0D;
    }

    protected double getCruiseMinRange() {
        return 50.0D;
    }

    protected double getCruiseExtraRange() {
        return 80.0D;
    }

    protected double getMaxHeightAboveGround() {
        return 50.0D;
    }
}