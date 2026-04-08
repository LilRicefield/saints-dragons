package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.server.ai.goals.base.DragonAggroLandingHelper;
import com.leon.saintsdragons.server.ai.goals.base.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
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

public class CindervaneFlightGoal extends Goal {
    private static final DragonFlightBehaviorProfile PROFILE = DragonFlightBehaviorProfile.cindervane();
    private static final double CRUISE_SPEED = 1.25D;
    private static final double LANDING_SPEED = 1.0D;
    private static final double MIN_AIRBORNE_LANDING_HORIZONTAL = 6.0D;
    private final Cindervane amphithere;
    private Vec3 targetPosition;
    private int stuckCounter = 0;
    private int timeSinceTargetChange = 0;

    // Landing cooldown to prevent immediate takeoff after landing
    private long lastLandingTime = 0;
    // Flight decision cooldown (slower than lightning amphithere)
    private int flightDecisionCooldown = 0;
    
    // Weather state tracking for immediate response
    private boolean wasThundering = false;
    private boolean wasRaining = false;

    public CindervaneFlightGoal(Cindervane amphithere) {
        this.amphithere = amphithere;
        this.setFlags(EnumSet.of(Flag.MOVE));
        
        // Start with no offset
        this.flightDecisionCooldown = 0;
    }

    @Override
    public boolean canUse() {
        if (isFollowingPackLeader()) {
            return false;
        }

        // In Follow command, owner-follow goal should own movement entirely.
        // Otherwise this autonomous flight goal can take off and drift away from the owner.
        if (isInOwnerFollowMode()) {
            return false;
        }

        // Babies cannot fly
        if (amphithere.isBaby()) {
            return false;
        }

        // Never start autonomous flight while submerged.
        // Water behavior is handled by swim goals/combat steering instead.
        if (amphithere.isInWater() || amphithere.isInWaterOrBubble() || amphithere.isInLava()) {
            return false;
        }

        // Don't interfere with landing sequence
        if (amphithere.isLanding()) {
            return false;
        }

        // Don't interfere with important behaviors
        if (amphithere.isVehicle() || amphithere.isPassenger() || amphithere.isOrderedToSit()) {
            return false;
        }

        // Never start ambient/autonomous flight during combat.
        // Combat goals handle movement (including water steering) directly.
        LivingEntity combatTarget = amphithere.getTarget();
        if (combatTarget != null && combatTarget.isAlive()) {
            return false;
        }

        // Don't take off while sleeping or waking up
        if (amphithere.isSleeping() || amphithere.isSleepingExiting()) {
            return false;
        }

        // Parents should stay grounded with nearby babies unless they are over danger.
        if (hasNearbyBabies() && !isOverDanger()) {
            return false;
        }

        // In Wander command, tamed flyers should stay grounded and roam on foot.
        if (amphithere.isTame() && amphithere.getCommand() == 2) {
            return false;
        }

        // Weather state snapshot for this decision
        boolean thundering = amphithere.level().isThundering();
        boolean raining = !thundering && amphithere.level().isRaining();
        
        // Check for weather changes that should trigger immediate takeoff
        boolean weatherChangedToStorm = (thundering && !wasThundering) || (raining && !wasRaining);
        boolean weatherChangedToThunder = thundering && !wasThundering;
        
        // Update weather state tracking
        wasThundering = thundering;
        wasRaining = raining;

        // Tamed amphitheres stay grounded (already handled above)
        // This check is redundant but kept for clarity

        // Use server game time for landing cooldown checks
        long currentTime = amphithere.level().getGameTime();
        int cooldown = PROFILE.landingCooldownTicks();
        if (thundering) cooldown = 0;            // no cooldown in thunder - gliders avoid storms
        else if (raining) cooldown = cooldown / 4; // shorter cooldown in rain - gliders prefer clear weather
        
        // Override cooldown if weather just changed to storm conditions
        if (weatherChangedToStorm) {
            cooldown = 0;
        }
        
        if (!amphithere.isFlying() && (currentTime - lastLandingTime) < cooldown) {
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
            if (amphithere.isFlying()) {
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
        if (isFollowingPackLeader()) {
            return false;
        }

        // Stop autonomous flight immediately when follow mode is active.
        if (isInOwnerFollowMode()) {
            return false;
        }

        // If we end up in fluid, stop autonomous flight behavior immediately.
        if (amphithere.isInWater() || amphithere.isInWaterOrBubble() || amphithere.isInLava()) {
            if (amphithere.isFlying()) {
                beginLandingApproach();
                return true;
            }
            return false;
        }

        if (amphithere.isLanding()) {
            return !amphithere.onGround();
        }

        // Stop if ordered to sit or something important comes up
        if (amphithere.isOrderedToSit() || amphithere.isVehicle()) {
            return false;
        }

        // In Wander command, tamed flyers should not remain in autonomous flight.
        if (amphithere.isTame() && amphithere.getCommand() == 2) {
            if (amphithere.isFlying()) {
                beginLandingApproach();
                return true;
            }
            return false;
        }

        // If a baby comes nearby while airborne, abort the patrol and land.
        if (hasNearbyBabies() && !isOverDanger()) {
            if (amphithere.isFlying()) {
                beginLandingApproach();
                return true;
            }
            return false;
        }

        // Stop if combat starts
        var target = amphithere.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }

        // Check if amphithere wants to land naturally (only for wild/untamed dragons)
        if (!amphithere.isTame()) {
            boolean thundering = amphithere.level().isThundering();
            boolean raining = !thundering && amphithere.level().isRaining();
            if (amphithere.isFlying() && !shouldKeepFlying(thundering, raining)) {
                beginLandingApproach();
                return true;
            }
        }

        // Continue if we're flying and have a target
        // CRITICAL: Only continue if actually airborne (not on ground)
        // Allow brief grace period for takeoff (5 ticks = 0.25 seconds)
        if (amphithere.isFlying() && amphithere.onGround()) {
            if (timeSinceTargetChange > 5) { // Grace period for takeoff
                finishLanding();
                return false;
            }
        }
        
        return amphithere.isFlying() && targetPosition != null && amphithere.distanceToSqr(targetPosition) > 9.0;
    }

    @Override
    public void start() {
        if (amphithere.isInWater() || amphithere.isInWaterOrBubble() || amphithere.isInLava()) {
            return;
        }
        if (amphithere.onGround() && !amphithere.isFlying() && !amphithere.isTakeoff() && !amphithere.isLanding()) {
            amphithere.beginAiTakeoff(Cindervane.TAKEOFF_ANIMATION_TICKS);
        } else {
            amphithere.beginAiFlight();
        }
        if (targetPosition != null) {
            moveToTarget(targetPosition, CRUISE_SPEED);
        }
    }

    @Override
    public void tick() {
        timeSinceTargetChange++;

        // If amphithere wants to land, let it handle that
        if (amphithere.isLanding()) {
            if (targetPosition == null) {
                beginLandingApproach();
            } else if (!amphithere.getNavigation().isInProgress()) {
                moveToTarget(targetPosition, LANDING_SPEED);
            }
            return;
        }

        // CRITICAL: Handle stuck state where isFlying=true but onGround=true
        // Allow brief grace period for takeoff (5 ticks = 0.25 seconds)
        if (amphithere.isFlying() && amphithere.onGround()) {
            if (timeSinceTargetChange > 5) { // Grace period for takeoff
                finishLanding();
                return;
            }
        }

        if (amphithere.isTame() && amphithere.getCommand() == 2) {
            beginLandingApproach();
            return;
        }

        // Check if we need a new target
        boolean needNewTarget = false;

        if (targetPosition == null) {
            needNewTarget = true;
        } else {
            double distanceToTarget = amphithere.distanceToSqr(targetPosition);

            // Reached target - large completion distance for glider soaring
            if (distanceToTarget < PROFILE.targetReachedDistanceSq()) {
                needNewTarget = true;
            }

            // Check if move controller gave up (collision handling)
            if (amphithere.horizontalCollision && distanceToTarget > 25.0) {
                needNewTarget = true;
                stuckCounter = 0;
            }

            // Better stuck detection
            if (amphithere.horizontalCollision && timeSinceTargetChange % 5 == 0) {
                stuckCounter++;
                if (stuckCounter > 2) {
                    needNewTarget = true;
                    stuckCounter = 0;
                }
            } else if (!amphithere.horizontalCollision) {
                stuckCounter = Math.max(0, stuckCounter - 1);
            }

            // Periodic path validation
            if (amphithere.tickCount % 20 == 0) {
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
        amphithere.getNavigation().stop();

        // NEW: Record landing time for cooldown
        if (!amphithere.isFlying()) {
            lastLandingTime = amphithere.level().getGameTime();
        }
    }

    // ===== FLIGHT TARGET FINDING =====

    private Vec3 findFlightTarget() {
        Vec3 dragonPos = amphithere.position();
        Vec3 anchor = getFlightAnchor();

        // Try multiple attempts with progressively more desperate searching
        for (int attempts = 0; attempts < 16; attempts++) {
            Vec3 candidate = generateFlightCandidate(anchor, dragonPos, attempts);

            if (isValidFlightTarget(candidate)) {
                return candidate;
            }
        }

        // Fallback: safe position above anchor
        return new Vec3(anchor.x, findSafeFlightHeight(anchor.x, anchor.z, true), anchor.z);
    }

    private void beginLandingApproach() {
        Vec3 landingTarget = DragonAggroLandingHelper.findLandingTarget(amphithere, null);
        if (landingTarget == null) {
            return;
        }

        targetPosition = landingTarget;
        amphithere.beginAiLanding();
        moveToTarget(landingTarget, LANDING_SPEED);
    }

    private void finishLanding() {
        targetPosition = null;
        if (amphithere.onGround()) {
            amphithere.handleAiLandingComplete();
        } else {
            amphithere.setLanding(false);
            amphithere.setFlying(false);
            amphithere.setHovering(false);
            amphithere.setTakeoff(false);
        }
        amphithere.getNavigation().stop();
    }

    private void moveToTarget(Vec3 target, double speed) {
        if (target != null) {
            amphithere.getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }

    private Vec3 findLandingTarget() {
        BlockPos origin = amphithere.blockPosition();
        double currentAltitude = Math.max(0.0D, amphithere.getY()
                - amphithere.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ()));
        double minHorizontalDistance = currentAltitude > 6.0D ? MIN_AIRBORNE_LANDING_HORIZONTAL : 0.0D;

        for (int radius = 8; radius <= 40; radius += 8) {
            for (int attempt = 0; attempt < 16; attempt++) {
                int dx = amphithere.getRandom().nextInt(radius * 2 + 1) - radius;
                int dz = amphithere.getRandom().nextInt(radius * 2 + 1) - radius;
                BlockPos column = origin.offset(dx, 0, dz);
                double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
                if (horizontalDistance < minHorizontalDistance) {
                    continue;
                }
                if (!amphithere.level().hasChunkAt(column)) {
                    continue;
                }

                int surfaceY = amphithere.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
                BlockPos ground = new BlockPos(column.getX(), surfaceY - 1, column.getZ());
                if (isValidLandingSurface(ground)) {
                    return new Vec3(column.getX() + 0.5D, ground.getY() + 1.0D, column.getZ() + 0.5D);
                }
            }
        }

        if (minHorizontalDistance > 0.0D) {
            double relaxedMinHorizontal = Math.max(3.0D, minHorizontalDistance * 0.5D);
            for (int radius = 8; radius <= 40; radius += 8) {
                for (int attempt = 0; attempt < 16; attempt++) {
                    int dx = amphithere.getRandom().nextInt(radius * 2 + 1) - radius;
                    int dz = amphithere.getRandom().nextInt(radius * 2 + 1) - radius;
                    BlockPos column = origin.offset(dx, 0, dz);
                    double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
                    if (horizontalDistance < relaxedMinHorizontal) {
                        continue;
                    }
                    if (!amphithere.level().hasChunkAt(column)) {
                        continue;
                    }

                    int surfaceY = amphithere.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
                    BlockPos ground = new BlockPos(column.getX(), surfaceY - 1, column.getZ());
                    if (isValidLandingSurface(ground)) {
                        return new Vec3(column.getX() + 0.5D, ground.getY() + 1.0D, column.getZ() + 0.5D);
                    }
                }
            }
        }

        for (int radius = 8; radius <= 40; radius += 8) {
            for (int attempt = 0; attempt < 12; attempt++) {
                int dx = amphithere.getRandom().nextInt(radius * 2 + 1) - radius;
                int dz = amphithere.getRandom().nextInt(radius * 2 + 1) - radius;
                BlockPos column = origin.offset(dx, 0, dz);
                if (!amphithere.level().hasChunkAt(column)) {
                    continue;
                }

                int surfaceY = amphithere.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
                BlockPos ground = new BlockPos(column.getX(), surfaceY - 1, column.getZ());
                if (isValidLandingSurface(ground)) {
                    return new Vec3(column.getX() + 0.5D, ground.getY() + 1.0D, column.getZ() + 0.5D);
                }
            }
        }

        return null;
    }

    private boolean isValidLandingSurface(BlockPos ground) {
        if (!amphithere.level().hasChunkAt(ground)) {
            return false;
        }

        var state = amphithere.level().getBlockState(ground);
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (!state.isFaceSturdy(amphithere.level(), ground, Direction.UP)) {
            return false;
        }

        BlockPos above = ground.above();
        BlockPos aboveTwo = above.above();
        var aboveState = amphithere.level().getBlockState(above);
        var aboveTwoState = amphithere.level().getBlockState(aboveTwo);
        return aboveState.getCollisionShape(amphithere.level(), above).isEmpty()
                && aboveState.getFluidState().isEmpty()
                && aboveTwoState.getCollisionShape(amphithere.level(), aboveTwo).isEmpty()
                && aboveTwoState.getFluidState().isEmpty();
    }

    private Vec3 generateFlightCandidate(Vec3 anchor, Vec3 dragonPos, int attempt) {
        boolean isStuck = amphithere.horizontalCollision || stuckCounter > 0;

        boolean tethered = isTamedWander();
        float range;
        Vec3 candidate;

        if (tethered) {
            double min = 10.0 + amphithere.getRandom().nextDouble() * 6.0;
            double max = 24.0 + amphithere.getRandom().nextDouble() * 6.0;
            double angle = amphithere.getRandom().nextDouble() * Math.PI * 2.0;
            double radius = min + amphithere.getRandom().nextDouble() * (max - min);
            double cx = anchor.x + Math.cos(angle) * radius;
            double cz = anchor.z + Math.sin(angle) * radius;
            double targetY = findSafeFlightHeight(cx, cz, true);
            candidate = new Vec3(cx, targetY, cz);
        } else {
            float maxRot = isStuck ? 360 : 180;
            range = isStuck ? 40.0f + amphithere.getRandom().nextFloat() * 60.0f :
                    80.0f + amphithere.getRandom().nextFloat() * 120.0f;

            float yRotOffset;
            if (isStuck && attempt < 8) {
                yRotOffset = (float) Math.toRadians(180 + amphithere.getRandom().nextFloat() * 120 - 60);
            } else {
                yRotOffset = (float) Math.toRadians(amphithere.getRandom().nextFloat() * maxRot - (maxRot / 2));
            }

            float xRotOffset = (float) Math.toRadians((amphithere.getRandom().nextFloat() - 0.5f) * 20);

            Vec3 lookVec = amphithere.getLookAngle();
            Vec3 targetVec = lookVec.scale(range).yRot(yRotOffset).xRot(xRotOffset);
            Vec3 raw = dragonPos.add(targetVec);
            double targetY = findSafeFlightHeight(raw.x, raw.z, false);
            candidate = new Vec3(raw.x, targetY, raw.z);
        }

        if (!amphithere.level().isLoaded(BlockPos.containing(candidate))) {
            return null;
        }

        return candidate;
    }

    private double findSafeFlightHeight(double x, double z, boolean tethered) {
        int ix = (int) x;
        int iz = (int) z;

        int groundY = amphithere.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);
        boolean thundering = amphithere.level().isThundering();
        boolean raining = !thundering && amphithere.level().isRaining();

        double capAboveGround;
        if (tethered) {
            capAboveGround = thundering ? 12.0 : (raining ? 18.0 : 32.0);
        } else {
            capAboveGround = thundering ? 20.0 : (raining ? 30.0 : 80.0);
        }

        double base;
        if (tethered) {
            base = 12.0 + amphithere.getRandom().nextDouble() * 12.0;
        } else {
            base = 25.0 + amphithere.getRandom().nextDouble() * 35.0;
        }

        double target = groundY + base;
        double cap = groundY + capAboveGround;
        double worldCap = amphithere.level().getMaxBuildHeight() - 10.0;

        return Math.min(Math.min(target, cap), worldCap);
    }

    private Vec3 getFlightAnchor() {
        if (isTamedWander()) {
            LivingEntity owner = amphithere.getOwner();
            if (owner != null) {
                return owner.position();
            }
        }
        return amphithere.position();
    }

    private boolean isTamedWander() {
        return amphithere.isTame() && amphithere.getCommand() == 2 && amphithere.getOwner() != null;
    }

    private boolean isInOwnerFollowMode() {
        LivingEntity owner = amphithere.getOwner();
        return amphithere.isTame()
                && amphithere.getCommand() == 0
                && owner != null
                && owner.isAlive()
                && owner.level() == amphithere.level();
    }

    private boolean isFollowingPackLeader() {
        if (!amphithere.canParticipateInPack()) {
            return false;
        }
        java.util.UUID leaderUuid = amphithere.getPackLeaderUuid();
        return leaderUuid != null && !leaderUuid.equals(amphithere.getUUID());
    }

    private boolean isValidFlightTarget(Vec3 target) {
        if (target == null) return false;

        BlockHitResult result = amphithere.level().clip(new ClipContext(
                amphithere.getEyePosition(),
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                amphithere
        ));

        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }

        double distanceToHit = result.getLocation().distanceTo(amphithere.position());
        double distanceToTarget = target.distanceTo(amphithere.position());

        return distanceToHit > distanceToTarget * 0.95;
    }

    // ===== DECISION MAKING (SLOWER THAN LIGHTNING DRAGON) =====

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
        return baseInterval + amphithere.getRandom().nextInt(jitter);
    }

    private boolean shouldTakeOff(boolean thundering, boolean raining) {
        if (isOverDanger()) {
            return true;
        }

        // NIGHT-TIME: Wild Cindervanes don't take off at night (they sleep)
        // Tamed dragons can still fly at night with owner
        if (!amphithere.isTame()) {
            long dayTime = amphithere.level().getDayTime() % 24000;
            boolean isNight = dayTime >= 13000 && dayTime < 23000;
            if (isNight) {
                return false; // Stay grounded at night for RestGoal to activate
            }
        }

        if (thundering) {
            return amphithere.getRandom().nextInt(PROFILE.takeoffRollThunder()) == 0;
        } else if (raining) {
            return amphithere.getRandom().nextInt(PROFILE.takeoffRollRain()) == 0;
        } else {
            return amphithere.getRandom().nextInt(PROFILE.takeoffRollClear()) == 0;
        }
    }

    private boolean shouldKeepFlying(boolean thundering, boolean raining) {
        if (isOverDanger()) {
            return true;
        }

        // NIGHT-TIME: Wild Cindervanes land quickly at night (they sleep)
        // Tamed dragons can still fly at night with owner
        if (!amphithere.isTame()) {
            long dayTime = amphithere.level().getDayTime() % 24000;
            boolean isNight = dayTime >= 13000 && dayTime < 23000;
            if (isNight) {
                // Land quickly at night (~5 sec average) to find a safe spot to sleep
                return amphithere.getRandom().nextInt(100) != 0;
            }
        }

        // Weather-weighted patrol durations - gliders avoid storms
        if (thundering) {
            return amphithere.getRandom().nextInt(PROFILE.keepFlyingRollThunder()) != 0;
        } else if (raining) {
            return amphithere.getRandom().nextInt(PROFILE.keepFlyingRollRain()) != 0;
        } else {
            return amphithere.getRandom().nextInt(PROFILE.keepFlyingRollClear()) != 0;
        }
    }

    // ===== UTILITY METHODS =====

    private boolean hasTakeoffClearance() {
        BlockPos dragonPos = amphithere.blockPosition();
        double dragonWidth = amphithere.getBbWidth();
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
                    var state = amphithere.level().getBlockState(checkPos);
                    if (state.isAir()) {
                        continue;
                    }

                    if (!state.getCollisionShape(amphithere.level(), checkPos).isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean hasNearbyBabies() {
        return amphithere.hasNearbyAssignedBabies(Cindervane.class);
    }

    private boolean isOverDanger() {
        BlockPos dragonPos = amphithere.blockPosition();
        boolean foundSolid = false;
        boolean nearFluid = false;

        for (int i = 1; i <= 25; i++) {
            BlockPos checkPos = dragonPos.below(i);

            var state = amphithere.level().getBlockState(checkPos);
            // Treat as solid ground if the block has a collision shape or sturdy top face
            if (!state.getCollisionShape(amphithere.level(), checkPos).isEmpty() ||
                    state.isFaceSturdy(amphithere.level(), checkPos, net.minecraft.core.Direction.UP)) {
                foundSolid = true;
                break;
            }

            // Consider fluids within 10 blocks below as dangerous (avoid landing in water/lava)
            if (i <= 10 && !amphithere.level().getFluidState(checkPos).isEmpty()) {
                nearFluid = true;
                // No break: still continue to see if solid exists even closer
            }
        }

        // Dangerous if over fluid nearby, or no solid ground found and we're near world bottom (void-like)
        if (nearFluid) return true;
        return !foundSolid && dragonPos.getY() < amphithere.level().getMinBuildHeight() + 20;
    }

}
