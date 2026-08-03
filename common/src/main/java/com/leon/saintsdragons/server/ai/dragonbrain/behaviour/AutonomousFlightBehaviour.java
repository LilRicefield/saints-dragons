package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.DragonAirCombatHelper;
import com.leon.saintsdragons.server.ai.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class AutonomousFlightBehaviour<T extends RideableFlyingDragon> extends DragonBehaviour<T> {
    private static final int STUCK_RETARGET_TICKS = 20;
    protected final DragonFlightBehaviorProfile profile;
    private final double cruiseSpeed;
    private final double landingSpeed;
    private final int takeoffAnimationTicks;

    private Vec3 targetPosition;
    private int timeSinceTargetChange;
    private long lastLandingTime;
    private int decisionCooldown;
    private boolean currentCruiseDive;

    public AutonomousFlightBehaviour(DragonFlightBehaviorProfile profile,
                                     double cruiseSpeed,
                                     double landingSpeed,
                                     int takeoffAnimationTicks) {
        super(Map.of(DragonMemories.MOVEMENT_INTENT, MemoryStatus.REGISTERED));
        this.profile = profile;
        this.cruiseSpeed = cruiseSpeed;
        this.landingSpeed = landingSpeed;
        this.takeoffAnimationTicks = takeoffAnimationTicks;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!canUseAutonomousFlight(dragon)) {
            return false;
        }
        boolean recoveringGroundPath = shouldRecoverFromGroundPathFailure(dragon);
        if (!recoveringGroundPath
                && !dragon.isFlying()
                && dragon.level().getGameTime() - lastLandingTime < getLandingCooldownTicks(dragon)) {
            return false;
        }
        if (!recoveringGroundPath && decisionCooldown > 0 && --decisionCooldown > 0) {
            return false;
        }

        boolean shouldFly = recoveringGroundPath
                || dragon.isOverStandardFlightDanger()
                || (dragon.isFlying()
                ? shouldKeepFlying(dragon)
                : dragon.hasStandardTakeoffClearance(getTakeoffClearanceHeight(dragon)) && shouldTakeOff(dragon));
        decisionCooldown = nextDecisionCooldown(dragon, getDecisionIntervalTicks(dragon));

        if (!shouldFly) {
            return false;
        }

        targetPosition = findCruiseTarget(dragon);
        return targetPosition != null;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (dragon.isVehicle() || dragon.isPassenger()) {
            return false;
        }
        if (dragon.isLanding()) {
            return !dragon.onGround();
        }
        if (!canContinueAutonomousFlight(dragon)) {
            if (dragon.isFlying() && shouldLandWhenAutonomousFlightBlocked(dragon)) {
                beginLandingApproach(context);
                return true;
            }
            return false;
        }
        if (shouldLandNow(dragon)) {
            beginLandingApproach(context);
            return true;
        }
        return dragon.isFlying() && targetPosition != null && dragon.distanceToSqr(targetPosition) > 9.0D;
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        dragon.getAIMovement().clearGroundPathFailureHistory();
        if (dragon.onGround() && !dragon.isFlying() && !dragon.isTakeoff() && !dragon.isLanding()) {
            beginAutonomousTakeoff(dragon);
        } else {
            dragon.beginAiFlight();
        }
        setMoveIntent(context, targetPosition, getCruiseSpeed(targetPosition));
        dragon.setAccelerating(currentCruiseDive);
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        timeSinceTargetChange++;

        if (dragon.isTakeoff() && dragon.isFlying() && !dragon.onGround()) {
            dragon.beginAiFlight();
        }

        if (dragon.isLanding()) {
            dragon.setAccelerating(false);
            if (targetPosition == null) {
                beginLandingApproach(context);
            } else if (!dragon.getAIMovement().isPathing()) {
                context.memories().set(DragonMemories.MOVEMENT_INTENT,
                        DragonMovementIntent.transitionToGround(targetPosition, landingSpeed));
            }
            return;
        }

        if (needsNewCruiseTarget(dragon)) {
            targetPosition = findCruiseTarget(dragon);
            timeSinceTargetChange = 0;
            setMoveIntent(context, targetPosition, getCruiseSpeed(targetPosition));
            dragon.setAccelerating(currentCruiseDive);
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        targetPosition = null;
        timeSinceTargetChange = 0;
        currentCruiseDive = false;
        dragon.setAccelerating(false);
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        if (!dragon.isFlying()) {
            lastLandingTime = dragon.level().getGameTime();
        }
    }

    protected boolean canUseAutonomousFlight(T dragon) {
        LivingEntity target = dragon.getTarget();
        return !dragon.isBaby()
                && !dragon.isInLove()
                && !dragon.isLanding()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleeping()
                && !dragon.isSleepingExiting()
                && (target == null || !target.isAlive());
    }

    protected boolean canContinueAutonomousFlight(T dragon) {
        LivingEntity target = dragon.getTarget();
        return !dragon.isInLove()
                && !dragon.isOrderedToSit()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isSleeping()
                && !dragon.isSleepingExiting()
                && (target == null || !target.isAlive());
    }

    protected boolean shouldLandWhenAutonomousFlightBlocked(T dragon) {
        return false;
    }

    protected void beginAutonomousTakeoff(T dragon) {
        dragon.beginAiTakeoff(takeoffAnimationTicks);
    }

    protected boolean shouldLandNow(T dragon) {
        return dragon.isFlying() && !shouldKeepFlying(dragon) && !dragon.isOverStandardFlightDanger();
    }

    protected boolean shouldTakeOff(T dragon) {
        return dragon.isOverStandardFlightDanger()
                || dragon.getRandom().nextInt(Math.max(1, getTakeoffRoll(dragon))) == 0;
    }

    protected boolean shouldRecoverFromGroundPathFailure(T dragon) {
        return !dragon.isAerial()
                && dragon.getAIMovement().hasRepeatedGroundPathFailures()
                && DragonAirCombatHelper.canTriggerAiFlight(dragon);
    }

    protected boolean shouldKeepFlying(T dragon) {
        return dragon.isOverStandardFlightDanger()
                || dragon.getRandom().nextInt(Math.max(1, getKeepFlyingRoll(dragon))) != 0;
    }

    protected Vec3 findCruiseTarget(T dragon) {
        currentCruiseDive = false;
        Vec3 cruiseTarget = dragon.findStandardAiFlightTarget(
                getCruiseTurnDegrees(dragon),
                getCruiseMinRange(dragon),
                getCruiseExtraRange(dragon),
                getMaxHeightAboveGround(dragon),
                dragon.isFlightControllerStuck()
        );
        if (cruiseTarget == null || !shouldUseAutonomousDiveTarget(dragon, cruiseTarget)) {
            return adjustCruiseTarget(dragon, cruiseTarget);
        }

        Vec3 diveTarget = buildAutonomousDiveTarget(dragon, cruiseTarget);
        currentCruiseDive = diveTarget != cruiseTarget;
        return adjustCruiseTarget(dragon, diveTarget);
    }

    protected Vec3 adjustCruiseTarget(T dragon, Vec3 cruiseTarget) {
        return cruiseTarget;
    }

    protected double getCruiseSpeed(Vec3 targetPosition) {
        return currentCruiseDive ? getAutonomousDiveSpeed() : cruiseSpeed;
    }

    protected void beginLandingApproach(DragonBrainContext<T> context) {
        Vec3 landingTarget = context.dragon().getAIMovement().findGroundTransitionTarget(null);
        if (landingTarget == null) {
            return;
        }
        targetPosition = landingTarget;
        context.memories().set(DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.transitionToGround(landingTarget, landingSpeed));
    }

    protected boolean isCurrentCruiseDive() {
        return currentCruiseDive;
    }

    private boolean needsNewCruiseTarget(T dragon) {
        if (targetPosition == null) {
            return true;
        }
        if (dragon.distanceToSqr(targetPosition) < profile.targetReachedDistanceSq()) {
            return true;
        }
        if (dragon.isFlightControllerRetrying()) {
            return timeSinceTargetChange >= STUCK_RETARGET_TICKS;
        }
        if (dragon.isFlightControllerFailed()) {
            return true;
        }
        if (timeSinceTargetChange > profile.maxTargetAgeTicks()) {
            return true;
        }
        return dragon.tickCount % 20 == 0 && !dragon.isValidStandardFlightTarget(targetPosition);
    }

    protected void setMoveIntent(DragonBrainContext<T> context, Vec3 target, double speed) {
        if (target != null) {
            context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.auto(target, speed));
        }
    }

    protected int nextDecisionCooldown(T dragon, int baseInterval) {
        int jitter = Math.max(1, baseInterval / 2);
        return baseInterval + dragon.getRandom().nextInt(jitter);
    }

    protected int getLandingCooldownTicks(T dragon) {
        return profile.landingCooldownTicks();
    }

    protected int getDecisionIntervalTicks(T dragon) {
        return profile.decisionIntervalClear();
    }

    protected int getTakeoffRoll(T dragon) {
        return profile.takeoffRollClear();
    }

    protected int getKeepFlyingRoll(T dragon) {
        return profile.keepFlyingRollClear();
    }

    protected int getTakeoffClearanceHeight(T dragon) {
        return 10;
    }

    protected double getCruiseTurnDegrees(T dragon) {
        return 180.0D;
    }

    protected double getCruiseMinRange(T dragon) {
        return 50.0D;
    }

    protected double getCruiseExtraRange(T dragon) {
        return 80.0D;
    }

    protected double getMaxHeightAboveGround(T dragon) {
        return 50.0D;
    }

    protected boolean shouldUseAutonomousDiveTarget(T dragon, Vec3 cruiseTarget) {
        if (!dragon.isFlying() || dragon.isLanding() || dragon.getTarget() != null) {
            return false;
        }
        if (dragon.isInWater() || dragon.isInWaterOrBubble() || dragon.isInLava()) {
            return false;
        }
        if (!canAutonomousDiveInWeather(dragon) && (dragon.level().isThundering() || dragon.level().isRaining())) {
            return false;
        }
        if (dragon.getRandom().nextInt(Math.max(1, getAutonomousDiveRoll(dragon))) != 0) {
            return false;
        }
        return altitudeAboveTerrain(dragon, dragon.position()) >= getAutonomousDiveMinAltitude(dragon);
    }

    protected boolean canAutonomousDiveInWeather(T dragon) {
        return false;
    }

    protected int getAutonomousDiveRoll(T dragon) {
        return 3;
    }

    protected double getAutonomousDiveMinAltitude(T dragon) {
        return 28.0D;
    }

    protected double getAutonomousDiveAngleDegrees(T dragon) {
        return 34.0D;
    }

    protected double getAutonomousDivePullOutAltitude(T dragon) {
        return 10.0D;
    }

    protected double getAutonomousDiveSpeed() {
        return cruiseSpeed * 1.6D;
    }

    protected Vec3 buildAutonomousDiveTarget(T dragon, Vec3 cruiseTarget) {
        Vec3 current = dragon.position();
        double dx = cruiseTarget.x - current.x;
        double dz = cruiseTarget.z - current.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance < 8.0D) {
            return cruiseTarget;
        }

        double groundY = dragon.level().getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(cruiseTarget.x),
                Mth.floor(cruiseTarget.z)
        );
        double pullOutY = groundY + getAutonomousDivePullOutAltitude(dragon);
        double angledY = current.y - horizontalDistance * Math.tan(Math.toRadians(getAutonomousDiveAngleDegrees(dragon)));
        double targetY = Mth.clamp(angledY, pullOutY, current.y - 2.0D);
        return new Vec3(cruiseTarget.x, targetY, cruiseTarget.z);
    }

    protected double altitudeAboveTerrain(T dragon, Vec3 position) {
        BlockPos pos = BlockPos.containing(position);
        int groundY = dragon.level().getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                pos.getX(),
                pos.getZ()
        );
        return position.y - groundY;
    }
}
