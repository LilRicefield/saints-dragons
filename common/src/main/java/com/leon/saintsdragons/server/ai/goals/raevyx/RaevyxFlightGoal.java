package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.ai.goals.base.DragonAggroLandingHelper;
import com.leon.saintsdragons.server.ai.goals.base.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RaevyxFlightGoal extends Goal {
    private static final DragonFlightBehaviorProfile PROFILE = DragonFlightBehaviorProfile.raevyx();
    private static final double CRUISE_SPEED = 2.0D;
    private static final double LANDING_SPEED = 1.6D;
    private static final double MIN_AIRBORNE_LANDING_HORIZONTAL = 6.0D;

    private final Raevyx wyvern;
    private Vec3 targetPosition;
    private int stuckCounter = 0;
    private int timeSinceTargetChange = 0;

    // NEW: Landing cooldown to prevent immediate takeoff after landing
    private long lastLandingTime = 0;
    // Flight decision cooldown
    private int flightDecisionCooldown = 0;
    
    // Weather state tracking for immediate response
    private boolean wasThundering = false;
    private boolean wasRaining = false;

    public RaevyxFlightGoal(Raevyx wyvern) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.MOVE));
        
        // Start with no offset
        this.flightDecisionCooldown = 0;
    }

    @Override
    public boolean canUse() {
        // Babies cannot fly
        if (wyvern.isBaby()) {
            return false;
        }
        // Tamed Raevyx flight should be command/rider driven, not autonomous storm patrol.
        if (wyvern.isTame()) {
            return false;
        }

        // Don't interfere with landing sequence
        if (wyvern.isLanding()) {
            return false;
        }

        // Don't interfere with important behaviors
        if (wyvern.isVehicle() || wyvern.isPassenger() || wyvern.isOrderedToSit()) {
            return false;
        }

        // Don't take off while sleeping or waking up
        if (wyvern.isSleeping() || wyvern.isSleepingExiting()) {
            return false;
        }

        // Parents shouldn't fly away and abandon their babies (unless in danger)
        if (!wyvern.isBaby() && hasNearbyBabies() && !isOverDanger()) {
            return false;
        }

        // Weather state snapshot for this decision
        boolean thundering = wyvern.level().isThundering();
        boolean raining = !thundering && wyvern.level().isRaining();
        boolean stormy = thundering || raining;
        
        // Check for weather changes that should trigger immediate takeoff
        boolean weatherChangedToStorm = (thundering && !wasThundering) || (raining && !wasRaining);
        boolean weatherChangedToThunder = thundering && !wasThundering;
        
        // Update weather state tracking
        wasThundering = thundering;
        wasRaining = raining;

        // Use server game time for landing cooldown checks
        long currentTime = wyvern.level().getGameTime();
        int cooldown = PROFILE.landingCooldownTicks();
        if (thundering) cooldown = 0;            // no cooldown in thunder
        else if (raining) cooldown = cooldown / 4; // shorter cooldown in rain
        
        // Override cooldown if weather just changed to storm conditions
        if (weatherChangedToStorm) {
            cooldown = 0;
        }
        
        if (!wyvern.isFlying() && (currentTime - lastLandingTime) < cooldown) {
            return false;
        }

        // Use desynced cooldown to prevent all dragons making flight decisions same tick
        int decisionInterval = flightDecisionInterval(thundering, raining);
        if (flightDecisionCooldown > 0) {
            flightDecisionCooldown--;
            if (flightDecisionCooldown > 0) {
                // Override cooldown if weather just changed to thunder for immediate response
                if (weatherChangedToThunder) {
                    flightDecisionCooldown = 0;
                } else if ((thundering || raining) && flightDecisionCooldown > decisionInterval) {
                    flightDecisionCooldown = decisionInterval;
                }
                if (flightDecisionCooldown > 0) {
                    return false;
                }
            }
        }

        // Must fly if over danger
        boolean isFlying;
        if (isOverDanger()) {
            isFlying = true;
        } else {
            // Weather-based flight decisions
            if (wyvern.isFlying()) {
                isFlying = shouldKeepFlying(thundering, raining);
            } else {
                // Check for clearance before takeoff
                if (!hasTakeoffClearance()) {
                    isFlying = false;
                } else {
                    isFlying = shouldTakeOff(thundering, raining);
                }
            }
        }

        if (isFlying) {
            this.targetPosition = findFlightTarget();
            // Reset cooldown for next decision
            this.flightDecisionCooldown = nextDecisionCooldown(decisionInterval);
            return true;
        }

        // Reset cooldown even when not flying
        this.flightDecisionCooldown = nextDecisionCooldown(decisionInterval);
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (wyvern.isTame()) {
            if (wyvern.isFlying()) {
                beginLandingApproach();
                return true;
            }
            return false;
        }

        if (wyvern.isLanding()) {
            return !wyvern.onGround();
        }

        // Stop if ordered to sit or something important comes up
        if (wyvern.isOrderedToSit() || wyvern.isVehicle()) {
            return false;
        }

        // Stop if combat starts
        if (wyvern.getTarget() != null && wyvern.getTarget().isAlive()) {
            return false;
        }

        // NEW: Check if wyvern wants to land naturally
        boolean thundering = wyvern.level().isThundering();
        boolean raining = !thundering && wyvern.level().isRaining();
        if (wyvern.isFlying() && !shouldKeepFlying(thundering, raining)) {
            beginLandingApproach();
            return true;
        }

        // Continue if we're flying and have a target
        return wyvern.isFlying() && targetPosition != null && wyvern.distanceToSqr(targetPosition) > 9.0;
    }

    @Override
    public void start() {
        if (wyvern.onGround() && !wyvern.isFlying() && !wyvern.isTakeoff() && !wyvern.isLanding()) {
            wyvern.startTakeoffSequence(0.12D, Raevyx.TAKEOFF_ANIMATION_TICKS);
        } else {
            wyvern.setTakeoff(false);
            wyvern.setFlying(true);
            wyvern.setLanding(false);
            wyvern.setHovering(false);
        }

        if (targetPosition != null) {
            moveToTarget(targetPosition, CRUISE_SPEED);
        }
    }

    @Override
    public void tick() {
        timeSinceTargetChange++;

        // Clear takeoff flag once airborne
        if (wyvern.isTakeoff() && wyvern.isFlying() && !wyvern.onGround()) {
            wyvern.setTakeoff(false);
        }

        // If wyvern wants to land, let it handle that
        if (wyvern.isLanding()) {
            if (targetPosition == null) {
                beginLandingApproach();
            } else if (!wyvern.getNavigation().isInProgress()) {
                moveToTarget(targetPosition, LANDING_SPEED);
            }
            return;
        }

        // Check if we need a new target
        boolean needNewTarget = false;

        if (targetPosition == null) {
            needNewTarget = true;
        } else {
            double distanceToTarget = wyvern.distanceToSqr(targetPosition);

            // Reached target - much larger completion distance
            if (distanceToTarget < PROFILE.targetReachedDistanceSq()) {
                needNewTarget = true;
            }

            // Check if move controller gave up (collision handling)
            if (wyvern.isFlightControllerStuck() && distanceToTarget > 25.0) {
                needNewTarget = true;
                stuckCounter = 0;
            }

            // Better stuck detection
            if (wyvern.horizontalCollision && timeSinceTargetChange % 5 == 0) {
                stuckCounter++;
                if (stuckCounter > 2) {
                    needNewTarget = true;
                    stuckCounter = 0;
                }
            } else if (!wyvern.horizontalCollision) {
                stuckCounter = Math.max(0, stuckCounter - 1);
            }

            // Periodic path validation
            if (wyvern.tickCount % 20 == 0) {
                if (!isValidFlightTarget(targetPosition)) {
                    needNewTarget = true;
                }
            }

            // Been going to same target for too long
            if (timeSinceTargetChange > PROFILE.maxTargetAgeTicks()) {
                needNewTarget = true;
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
        wyvern.getNavigation().stop();

        // NEW: Record landing time for cooldown
        if (!wyvern.isFlying()) {
            lastLandingTime = wyvern.level().getGameTime();
        }
    }

    // ===== FLIGHT TARGET FINDING =====

    private Vec3 findFlightTarget() {
        Vec3 dragonPos = wyvern.position();

        // Try multiple attempts with progressively more desperate searching
        for (int attempts = 0; attempts < 16; attempts++) {
            Vec3 candidate = generateFlightCandidate(dragonPos, attempts);

            if (isValidFlightTarget(candidate)) {
                return candidate;
            }
        }

        // Fallback: safe position above current location
        return new Vec3(dragonPos.x, findSafeFlightHeight(dragonPos.x, dragonPos.z), dragonPos.z);
    }

    private void beginLandingApproach() {
        Vec3 landingTarget = DragonAggroLandingHelper.findLandingTarget(wyvern, null);
        if (landingTarget == null) {
            return;
        }

        targetPosition = landingTarget;
        wyvern.setHovering(false);
        wyvern.setTakeoff(false);
        wyvern.setLanding(true);
        moveToTarget(landingTarget, LANDING_SPEED);
    }

    private void finishLanding() {
        targetPosition = null;
        if (wyvern.onGround()) {
            wyvern.handleAiLandingComplete();
        } else {
            wyvern.setLanding(false);
            wyvern.setFlying(false);
            wyvern.setHovering(false);
            wyvern.setTakeoff(false);
        }
        wyvern.getNavigation().stop();
    }

    private void moveToTarget(Vec3 target, double speed) {
        if (target != null) {
            wyvern.getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }

    private Vec3 findLandingTarget() {
        BlockPos origin = wyvern.blockPosition();
        double currentAltitude = Math.max(0.0D, wyvern.getY()
                - wyvern.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ()));
        double minHorizontalDistance = currentAltitude > 6.0D ? MIN_AIRBORNE_LANDING_HORIZONTAL : 0.0D;

        for (int radius = 8; radius <= 24; radius += 8) {
            for (int attempt = 0; attempt < 10; attempt++) {
                int dx = wyvern.getRandom().nextInt(radius * 2 + 1) - radius;
                int dz = wyvern.getRandom().nextInt(radius * 2 + 1) - radius;
                BlockPos column = origin.offset(dx, 0, dz);
                double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
                if (horizontalDistance < minHorizontalDistance) {
                    continue;
                }
                if (!wyvern.level().hasChunkAt(column)) {
                    continue;
                }

                int surfaceY = wyvern.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
                BlockPos ground = new BlockPos(column.getX(), surfaceY - 1, column.getZ());
                if (isValidLandingSurface(ground)) {
                    return new Vec3(column.getX() + 0.5D, ground.getY() + 1.0D, column.getZ() + 0.5D);
                }
            }
        }

        if (minHorizontalDistance > 0.0D) {
            for (int radius = 0; radius <= 24; radius += 8) {
                for (int attempt = 0; attempt < 10; attempt++) {
                    int dx = radius == 0 ? 0 : wyvern.getRandom().nextInt(radius * 2 + 1) - radius;
                    int dz = radius == 0 ? 0 : wyvern.getRandom().nextInt(radius * 2 + 1) - radius;
                    BlockPos column = origin.offset(dx, 0, dz);
                    if (!wyvern.level().hasChunkAt(column)) {
                        continue;
                    }

                    int surfaceY = wyvern.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
                    BlockPos ground = new BlockPos(column.getX(), surfaceY - 1, column.getZ());
                    if (isValidLandingSurface(ground)) {
                        return new Vec3(column.getX() + 0.5D, ground.getY() + 1.0D, column.getZ() + 0.5D);
                    }
                }
            }
        }

        return null;
    }

    private boolean isValidLandingSurface(BlockPos ground) {
        if (!wyvern.level().hasChunkAt(ground)) {
            return false;
        }

        var state = wyvern.level().getBlockState(ground);
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (!state.isFaceSturdy(wyvern.level(), ground, Direction.UP)) {
            return false;
        }

        BlockPos above = ground.above();
        BlockPos aboveTwo = above.above();
        var aboveState = wyvern.level().getBlockState(above);
        var aboveTwoState = wyvern.level().getBlockState(aboveTwo);
        return aboveState.getCollisionShape(wyvern.level(), above).isEmpty()
                && aboveState.getFluidState().isEmpty()
                && aboveTwoState.getCollisionShape(wyvern.level(), aboveTwo).isEmpty()
                && aboveTwoState.getFluidState().isEmpty();
    }

    private Vec3 generateFlightCandidate(Vec3 dragonPos, int attempt) {
        boolean isStuck = wyvern.horizontalCollision || stuckCounter > 0 || wyvern.isFlightControllerStuck();

        float maxRot = isStuck ? 360 : 180;
        float range = isStuck ? 30.0f + wyvern.getRandom().nextFloat() * 40.0f :
                50.0f + wyvern.getRandom().nextFloat() * 80.0f; // Much larger range for exploration

        float yRotOffset;
        if (isStuck && attempt < 8) {
            yRotOffset = (float) Math.toRadians(180 + wyvern.getRandom().nextFloat() * 120 - 60);
        } else {
            yRotOffset = (float) Math.toRadians(wyvern.getRandom().nextFloat() * maxRot - (maxRot / 2));
        }

        float xRotOffset = (float) Math.toRadians((wyvern.getRandom().nextFloat() - 0.5f) * 20);

        Vec3 lookVec = wyvern.getLookAngle();
        Vec3 targetVec = lookVec.scale(range).yRot(yRotOffset).xRot(xRotOffset);
        Vec3 candidate = dragonPos.add(targetVec);

        double targetY = findSafeFlightHeight(candidate.x, candidate.z);
        candidate = new Vec3(candidate.x, targetY, candidate.z);

        if (!wyvern.level().isLoaded(BlockPos.containing(candidate))) {
            return null;
        }

        return candidate;
    }

    private double findSafeFlightHeight(double x, double z) {
        int ix = (int) x;
        int iz = (int) z;

        // Weather snapshot
        boolean thundering = wyvern.level().isThundering();
        boolean raining = !thundering && wyvern.level().isRaining();
        int groundY = wyvern.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);
        double capAboveGround = thundering ? 90.0 : (raining ? 70.0 : 50.0);
        double base = 15.0 + wyvern.getRandom().nextDouble() * 20.0;

        double target = groundY + base;
        double cap = groundY + capAboveGround;
        double worldCap = wyvern.level().getMaxBuildHeight() - 10.0;

        return Math.min(Math.min(target, cap), worldCap);
    }

    private boolean isValidFlightTarget(Vec3 target) {
        if (target == null) return false;

        BlockHitResult result = wyvern.level().clip(new ClipContext(
                wyvern.getEyePosition(),
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                wyvern
        ));

        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }

        double distanceToHit = result.getLocation().distanceTo(wyvern.position());
        double distanceToTarget = target.distanceTo(wyvern.position());

        return distanceToHit > distanceToTarget * 0.95;
    }

    // ===== DECISION MAKING (FIXED) =====

    private int flightDecisionInterval(boolean thundering, boolean raining) {
        if (thundering) {
            return PROFILE.decisionIntervalThunder();
        }
        if (raining) {
            return PROFILE.decisionIntervalRain();
        }
        return PROFILE.decisionIntervalClear();
    }

    private int nextDecisionCooldown(int baseInterval) {
        int jitter = Math.max(1, baseInterval / 2);
        return baseInterval + wyvern.getRandom().nextInt(jitter);
    }

    private boolean shouldTakeOff(boolean thundering, boolean raining) {
        if (isOverDanger()) {
            return true;
        }

        if (thundering) {
            return wyvern.getRandom().nextInt(PROFILE.takeoffRollThunder()) == 0;
        } else if (raining) {
            return wyvern.getRandom().nextInt(PROFILE.takeoffRollRain()) == 0;
        } else {
            return wyvern.getRandom().nextInt(PROFILE.takeoffRollClear()) == 0;
        }
    }

    private boolean shouldKeepFlying(boolean thundering, boolean raining) {
        if (isOverDanger()) {
            return true;
        }

        // Weather-weighted patrol durations
        if (thundering) {
            return wyvern.getRandom().nextInt(PROFILE.keepFlyingRollThunder()) != 0;
        } else if (raining) {
            return wyvern.getRandom().nextInt(PROFILE.keepFlyingRollRain()) != 0;
        } else {
            return wyvern.getRandom().nextInt(PROFILE.keepFlyingRollClear()) != 0;
        }
    }

    // ===== UTILITY METHODS =====

    private boolean hasTakeoffClearance() {
        BlockPos dragonPos = wyvern.blockPosition();
        double dragonWidth = wyvern.getBbWidth();
        int checkRadius = (int) Math.ceil(dragonWidth / 2.0);
        int checkHeight = 10; // Check 10 blocks up

        // Check a cylinder above the dragon
        for (int dy = 1; dy <= checkHeight; dy++) {
            for (int dx = -checkRadius; dx <= checkRadius; dx++) {
                for (int dz = -checkRadius; dz <= checkRadius; dz++) {
                    // Skip corners for more natural cylinder shape
                    if (Math.abs(dx) + Math.abs(dz) > checkRadius + 1) {
                        continue;
                    }

                    BlockPos checkPos = dragonPos.offset(dx, dy, dz);
                    var state = wyvern.level().getBlockState(checkPos);
                    if (state.isAir()) {
                        continue;
                    }

                    if (!state.getCollisionShape(wyvern.level(), checkPos).isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }


    private boolean hasNearbyBabies() {
        return wyvern.hasNearbyAssignedBabies(Raevyx.class);
    }

    private boolean isOverDanger() {
        BlockPos dragonPos = wyvern.blockPosition();
        boolean foundSolid = false;
        boolean nearFluid = false;

        for (int i = 1; i <= 25; i++) {
            BlockPos checkPos = dragonPos.below(i);

            var state = wyvern.level().getBlockState(checkPos);
            // Treat as solid ground if the block has a collision shape or sturdy top face
            if (!state.getCollisionShape(wyvern.level(), checkPos).isEmpty() ||
                    state.isFaceSturdy(wyvern.level(), checkPos, net.minecraft.core.Direction.UP)) {
                foundSolid = true;
                break;
            }

            // Consider fluids within 10 blocks below as dangerous (avoid landing in water/lava)
            if (i <= 10 && !wyvern.level().getFluidState(checkPos).isEmpty()) {
                nearFluid = true;
                // No break: still continue to see if solid exists even closer
            }
        }

        // Dangerous if over fluid nearby, or no solid ground found and we're near world bottom (void-like)
        if (nearFluid) return true;
        return !foundSolid && dragonPos.getY() < wyvern.level().getMinBuildHeight() + 20;
    }
}
