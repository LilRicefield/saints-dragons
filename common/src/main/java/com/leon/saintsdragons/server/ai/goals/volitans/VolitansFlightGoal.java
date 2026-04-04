package com.leon.saintsdragons.server.ai.goals.volitans;

import com.leon.saintsdragons.server.ai.goals.base.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class VolitansFlightGoal extends Goal {
    private static final DragonFlightBehaviorProfile PROFILE = DragonFlightBehaviorProfile.volitans();
    private static final double CRUISE_SPEED = 1.25D;
    private static final double LANDING_SPEED = 1.0D;
    private static final double MIN_AIRBORNE_LANDING_HORIZONTAL = 6.0D;

    private final Volitans dragon;
    private Vec3 targetPosition;
    private int stuckCounter;
    private int timeSinceTargetChange;
    private long lastLandingTime;
    private int flightDecisionCooldown;

    public VolitansFlightGoal(Volitans dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (dragon.isBaby() || dragon.isTame()) {
            return false;
        }
        if (dragon.isLanding() || dragon.isVehicle() || dragon.isPassenger() || dragon.isOrderedToSit()) {
            return false;
        }
        if (dragon.isSleeping() || dragon.isSleepingExiting()) {
            return false;
        }
        if (dragon.isBurrowing() || dragon.isInWater() || dragon.isInWaterOrBubble() || dragon.isInLava()) {
            return false;
        }
        if (dragon.isAiSpecialCombatActive() || dragon.isAiSpecialCombatReserved()) {
            return false;
        }

        LivingEntity target = dragon.getTarget();
        if (target != null && dragon.isTargetValid(target)) {
            return false;
        }

        long gameTime = dragon.level().getGameTime();
        if (!dragon.isFlying() && gameTime - lastLandingTime < PROFILE.landingCooldownTicks()) {
            return false;
        }

        if (flightDecisionCooldown > 0 && --flightDecisionCooldown > 0) {
            return false;
        }

        boolean shouldFly = isOverDanger()
                || (dragon.isFlying() ? shouldKeepFlying() : hasTakeoffClearance() && shouldTakeOff());

        flightDecisionCooldown = nextDecisionCooldown(PROFILE.decisionIntervalClear());
        if (!shouldFly) {
            return false;
        }

        targetPosition = findFlightTarget();
        return targetPosition != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (dragon.isBaby() || dragon.isVehicle() || dragon.isPassenger() || dragon.isOrderedToSit()) {
            return false;
        }
        if (dragon.isBurrowing() || dragon.isInWater() || dragon.isInWaterOrBubble() || dragon.isInLava()) {
            if (dragon.isFlying()) {
                beginLandingApproach();
                return true;
            }
            return false;
        }
        if (dragon.isAiSpecialCombatActive() || dragon.isAiSpecialCombatReserved()) {
            return false;
        }

        LivingEntity target = dragon.getTarget();
        if (target != null && dragon.isTargetValid(target)) {
            return false;
        }

        if (dragon.isLanding()) {
            return !dragon.onGround();
        }

        if (dragon.isFlying() && !shouldKeepFlying() && !isOverDanger()) {
            beginLandingApproach();
            return true;
        }

        if (dragon.isFlying() && dragon.onGround() && timeSinceTargetChange > 5) {
            finishLanding();
            return false;
        }

        return dragon.isFlying() && targetPosition != null && dragon.distanceToSqr(targetPosition) > 9.0D;
    }

    @Override
    public void start() {
        if (dragon.isFlying()) {
            dragon.beginAiFlight();
        } else {
            dragon.beginAiTakeoff();
        }
        if (targetPosition != null) {
            moveToTarget(targetPosition, CRUISE_SPEED);
        }
    }

    @Override
    public void tick() {
        timeSinceTargetChange++;

        if (dragon.isTakeoff() && !dragon.onGround() && dragon.getDeltaMovement().y > 0.02D) {
            dragon.beginAiFlight();
        }

        if (dragon.isLanding()) {
            if (targetPosition == null) {
                beginLandingApproach();
            } else if (!dragon.getNavigation().isInProgress()) {
                moveToTarget(targetPosition, LANDING_SPEED);
            }
            return;
        }

        if (dragon.isFlying() && dragon.onGround() && timeSinceTargetChange > 5) {
            finishLanding();
            return;
        }

        boolean needNewTarget = targetPosition == null;
        if (targetPosition != null) {
            double distanceToTarget = dragon.distanceToSqr(targetPosition);
            if (distanceToTarget < PROFILE.targetReachedDistanceSq()
                    || (dragon.isFlightControllerStuck() && distanceToTarget > 25.0D)
                    || timeSinceTargetChange > PROFILE.maxTargetAgeTicks()
                    || (dragon.tickCount % 20 == 0 && !isValidFlightTarget(targetPosition))) {
                needNewTarget = true;
                stuckCounter = 0;
            }

            if (!needNewTarget && dragon.horizontalCollision && timeSinceTargetChange % 5 == 0) {
                stuckCounter++;
                if (stuckCounter > 2) {
                    needNewTarget = true;
                    stuckCounter = 0;
                }
            } else if (!dragon.horizontalCollision) {
                stuckCounter = Math.max(0, stuckCounter - 1);
            }
        }

        if (needNewTarget) {
            targetPosition = findFlightTarget();
            timeSinceTargetChange = 0;
            moveToTarget(targetPosition, CRUISE_SPEED);
        }
    }

    @Override
    public void stop() {
        targetPosition = null;
        stuckCounter = 0;
        timeSinceTargetChange = 0;
        dragon.getNavigation().stop();
        if (!dragon.isFlying()) {
            lastLandingTime = dragon.level().getGameTime();
        }
    }

    private Vec3 findFlightTarget() {
        Vec3 origin = dragon.position();
        for (int attempt = 0; attempt < 16; attempt++) {
            Vec3 candidate = generateFlightCandidate(origin, attempt);
            if (isValidFlightTarget(candidate)) {
                return candidate;
            }
        }
        return new Vec3(origin.x, findSafeFlightHeight(origin.x, origin.z), origin.z);
    }

    private Vec3 generateFlightCandidate(Vec3 origin, int attempt) {
        boolean isStuck = dragon.horizontalCollision || stuckCounter > 0 || dragon.isFlightControllerStuck();
        float maxRot = isStuck ? 360.0F : 180.0F;
        float range = isStuck
                ? 24.0F + dragon.getRandom().nextFloat() * 32.0F
                : 32.0F + dragon.getRandom().nextFloat() * 48.0F;

        float yawOffset = isStuck && attempt < 8
                ? (float) Math.toRadians(180.0D + dragon.getRandom().nextFloat() * 120.0D - 60.0D)
                : (float) Math.toRadians(dragon.getRandom().nextFloat() * maxRot - maxRot * 0.5F);
        float pitchOffset = (float) Math.toRadians((dragon.getRandom().nextFloat() - 0.5F) * 20.0F);

        Vec3 raw = origin.add(dragon.getLookAngle().scale(range).yRot(yawOffset).xRot(pitchOffset));
        Vec3 candidate = new Vec3(raw.x, findSafeFlightHeight(raw.x, raw.z), raw.z);
        return dragon.level().isLoaded(BlockPos.containing(candidate)) ? candidate : null;
    }

    private double findSafeFlightHeight(double x, double z) {
        int groundY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
        double target = groundY + 12.0D + dragon.getRandom().nextDouble() * 14.0D;
        double cap = groundY + 34.0D;
        double worldCap = dragon.level().getMaxBuildHeight() - 10.0D;
        return Math.min(Math.min(target, cap), worldCap);
    }

    private void beginLandingApproach() {
        Vec3 landingTarget = findLandingTarget();
        if (landingTarget == null) {
            return;
        }
        targetPosition = landingTarget;
        dragon.beginAiLanding();
        moveToTarget(landingTarget, LANDING_SPEED);
    }

    private Vec3 findLandingTarget() {
        BlockPos origin = dragon.blockPosition();
        double altitude = Math.max(0.0D, dragon.getY()
                - dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ()));
        double minHorizontalDistanceSqr = altitude > 6.0D
                ? MIN_AIRBORNE_LANDING_HORIZONTAL * MIN_AIRBORNE_LANDING_HORIZONTAL
                : 0.0D;

        for (int radius = 8; radius <= 32; radius += 8) {
            for (int attempt = 0; attempt < 12; attempt++) {
                int dx = dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                int dz = dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                if (dx * dx + dz * dz < minHorizontalDistanceSqr) {
                    continue;
                }

                BlockPos column = origin.offset(dx, 0, dz);
                if (!dragon.level().hasChunkAt(column)) {
                    continue;
                }

                int surfaceY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
                BlockPos ground = new BlockPos(column.getX(), surfaceY - 1, column.getZ());
                if (isValidLandingSurface(ground)) {
                    return new Vec3(column.getX() + 0.5D, ground.getY() + 1.0D, column.getZ() + 0.5D);
                }
            }
        }
        return null;
    }

    private boolean isValidLandingSurface(BlockPos ground) {
        if (!dragon.level().hasChunkAt(ground)) {
            return false;
        }

        var state = dragon.level().getBlockState(ground);
        if (state.isAir() || !state.getFluidState().isEmpty() || !state.isFaceSturdy(dragon.level(), ground, Direction.UP)) {
            return false;
        }

        BlockPos above = ground.above();
        BlockPos aboveTwo = above.above();
        var aboveState = dragon.level().getBlockState(above);
        var aboveTwoState = dragon.level().getBlockState(aboveTwo);
        return aboveState.getCollisionShape(dragon.level(), above).isEmpty()
                && aboveState.getFluidState().isEmpty()
                && aboveTwoState.getCollisionShape(dragon.level(), aboveTwo).isEmpty()
                && aboveTwoState.getFluidState().isEmpty();
    }

    private void finishLanding() {
        targetPosition = null;
        if (dragon.onGround()) {
            dragon.handleAiLandingComplete();
        } else {
            dragon.markLandedNow();
        }
        dragon.getNavigation().stop();
    }

    private void moveToTarget(Vec3 target, double speed) {
        if (target != null) {
            dragon.getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }

    private boolean shouldTakeOff() {
        return dragon.getRandom().nextInt(PROFILE.takeoffRollClear()) == 0;
    }

    private boolean shouldKeepFlying() {
        return isOverDanger() || dragon.getRandom().nextInt(PROFILE.keepFlyingRollClear()) != 0;
    }

    private int nextDecisionCooldown(int baseInterval) {
        int jitter = Math.max(1, baseInterval / 2);
        return baseInterval + dragon.getRandom().nextInt(jitter);
    }

    private boolean hasTakeoffClearance() {
        BlockPos dragonPos = dragon.blockPosition();
        int checkRadius = (int) Math.ceil(dragon.getBbWidth() * 0.5D);
        for (int dy = 1; dy <= 10; dy++) {
            for (int dx = -checkRadius; dx <= checkRadius; dx++) {
                for (int dz = -checkRadius; dz <= checkRadius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > checkRadius + 1) {
                        continue;
                    }
                    BlockPos checkPos = dragonPos.offset(dx, dy, dz);
                    if (!dragon.level().getBlockState(checkPos).getCollisionShape(dragon.level(), checkPos).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isOverDanger() {
        BlockPos dragonPos = dragon.blockPosition();
        boolean foundSolid = false;
        boolean nearFluid = false;

        for (int i = 1; i <= 25; i++) {
            BlockPos checkPos = dragonPos.below(i);
            var state = dragon.level().getBlockState(checkPos);
            if (!state.getCollisionShape(dragon.level(), checkPos).isEmpty()
                    || state.isFaceSturdy(dragon.level(), checkPos, Direction.UP)) {
                foundSolid = true;
                break;
            }
            if (i <= 10 && !dragon.level().getFluidState(checkPos).isEmpty()) {
                nearFluid = true;
            }
        }

        return nearFluid || (!foundSolid && dragonPos.getY() < dragon.level().getMinBuildHeight() + 20);
    }

    private boolean isValidFlightTarget(Vec3 target) {
        if (target == null) {
            return false;
        }

        BlockHitResult result = dragon.level().clip(new ClipContext(
                dragon.getEyePosition(),
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                dragon
        ));

        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }

        double distanceToHit = result.getLocation().distanceTo(dragon.position());
        double distanceToTarget = target.distanceTo(dragon.position());
        return distanceToHit > distanceToTarget * 0.95D;
    }
}
