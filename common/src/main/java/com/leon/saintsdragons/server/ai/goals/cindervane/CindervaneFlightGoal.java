package com.leon.saintsdragons.server.ai.goals.cindervane;

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

/**
 * Amphithere flight goal for high-soaring glider behavior
 * Gliders soar high in clear weather but avoid storms and rain
 * Features large flight ranges (80-200 blocks) and high altitudes (25-60 blocks above ground)
 */
public class CindervaneFlightGoal extends Goal {
    private final Cindervane amphithere;
    private Vec3 targetPosition;
    private Vec3 landingPosition;
    private boolean landingApproach;
    private int landingApproachTicks = 0;
    private boolean landingForceDrop = false;
    private int stuckCounter = 0;
    private int timeSinceTargetChange = 0;

    // Landing cooldown to prevent immediate takeoff after landing
    private static final int LANDING_COOLDOWN_TICKS = 40; // 2 seconds minimum on ground (gliders want to fly!)
    private long lastLandingTime = 0;
    private static final int LANDING_FORCE_DROP_TICKS = 80;
    private static final int LANDING_EMERGENCY_GROUNDING_TICKS = 100; // 5 seconds - force ground if stuck
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
        // Don't interfere with landing sequence
        if (amphithere.isLanding()) {
            return false;
        }

        // Don't interfere with important behaviors
        if (amphithere.isVehicle() || amphithere.isPassenger() || amphithere.isOrderedToSit()) {
            return false;
        }

        // Don't take off while sleeping or waking up
        if (amphithere.isSleeping() || amphithere.isSleepingExiting()) {
            return false;
        }

        // Prevent autonomous flight when tamed - amphitheres should stay grounded
        if (amphithere.isTame() && amphithere.getOwner() != null) {
            // Only allow flight when over danger (void, lava, water)
            if (!isOverDanger()) {
                return false;
            }
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
        int cooldown = LANDING_COOLDOWN_TICKS; // fixed
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
            landingApproach = false;
            landingPosition = null;
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
        if (landingApproach) {
            if (amphithere.onGround()) {
                finishLanding();
                return false;
            }
            return true;
        }

        // Let landing system take over
        if (amphithere.isLanding()) {
            return false;
        }

        // Stop if ordered to sit or something important comes up
        if (amphithere.isOrderedToSit() || amphithere.isVehicle()) {
            return false;
        }

        // Tamed amphitheres only fly autonomously when over danger
        if (amphithere.isTame() && amphithere.getOwner() != null) {
            if (!isOverDanger()) {
                beginLandingApproach();
                return true;
            }
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
        amphithere.setFlying(true);
        amphithere.setLanding(false);
        amphithere.setHovering(false);
        landingApproach = false;
        landingPosition = null;
        if (targetPosition != null) {
            amphithere.getMoveControl().setWantedPosition(targetPosition.x, targetPosition.y, targetPosition.z, amphithere.getFlightSpeed());
        }
    }

    @Override
    public void tick() {
        timeSinceTargetChange++;

        if (landingApproach) {
            if (amphithere.isInWaterOrBubble()) {
                landingApproach = false;
                landingApproachTicks = 0;
                landingForceDrop = false;
                targetPosition = null;
                landingPosition = null;
                amphithere.setLanding(false);
                amphithere.setHovering(false);
                amphithere.setTakeoff(false);
                amphithere.setFlying(false);
                return;
            }
            landingApproachTicks++;

            // Emergency grounding: if stuck floating for too long, abort landing and let entity fall
            if (landingApproachTicks > LANDING_EMERGENCY_GROUNDING_TICKS && !amphithere.onGround()) {
                landingApproach = false;
                landingApproachTicks = 0;
                landingForceDrop = false;
                targetPosition = null;
                landingPosition = null;
                amphithere.setNoGravity(false);
                amphithere.setFlying(false);
                amphithere.setLanding(true);
                // Goal will stop, let gravity take over
                return;
            }

            if (!landingForceDrop && landingApproachTicks > LANDING_FORCE_DROP_TICKS) {
                landingForceDrop = true;
                Vec3 dropTarget = findValidDropTarget();
                if (dropTarget != null) {
                    landingPosition = dropTarget;
                } else {
                    // Can't find anywhere to drop - abort landing
                    landingApproach = false;
                    landingApproachTicks = 0;
                    amphithere.setLanding(false);
                    return;
                }
            }
            if (landingPosition != null) {
                BlockPos landingGround = BlockPos.containing(landingPosition.x, landingPosition.y - 1.0, landingPosition.z);
                if (!landingForceDrop && !isWideLandingSurface(landingGround)) {
                    landingPosition = findLandingTarget();
                    if (landingPosition == null) {
                        // No valid surface - abort landing
                        landingApproach = false;
                        landingApproachTicks = 0;
                        amphithere.setLanding(false);
                        return;
                    }
                }
                double altitude = amphithere.getY() - landingPosition.y;

                // Apply downward velocity throughout descent, not just when far away
                if (!amphithere.isInWaterOrBubble() && !amphithere.onGround()) {
                    Vec3 motion = amphithere.getDeltaMovement();
                    // Stronger descent when high, gentler when close
                    double descentRate = altitude > Cindervane.LANDING_BLEND_ALTITUDE ? 0.18 : 0.08;
                    double newY = Math.max(motion.y - descentRate, -1.6);
                    amphithere.setDeltaMovement(motion.x, newY, motion.z);
                }
                amphithere.getMoveControl().setWantedPosition(landingPosition.x, landingPosition.y, landingPosition.z, 1.6);
                if (!amphithere.isLanding()
                        && altitude >= -0.25D
                        && altitude <= Cindervane.LANDING_BLEND_ALTITUDE) {
                    amphithere.setLanding(true);
                }
            }
            return;
        }
        // If amphithere wants to land, let it handle that
        if (amphithere.isLanding()) {
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

        if (amphithere.isTame() && amphithere.getOwner() != null && !isOverDanger()) {
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
            if (distanceToTarget < 100.0) { // 100 blocks for long glider flights
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
            if (timeSinceTargetChange > 300) {
                needNewTarget = true;
            }
        }

        if (needNewTarget) {
            targetPosition = findFlightTarget();
            timeSinceTargetChange = 0;
            amphithere.getMoveControl().setWantedPosition(targetPosition.x, targetPosition.y, targetPosition.z, amphithere.getFlightSpeed());
        }
    }

    @Override
    public void stop() {
        targetPosition = null;
        landingPosition = null;
        landingApproach = false;
        landingApproachTicks = 0;
        landingForceDrop = false;
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
        if (landingApproach) {
            return;
        }

        landingPosition = findLandingTarget();
        if (landingPosition == null) {
            // No valid landing spot found - abort landing and keep flying
            return;
        }

        landingApproach = true;
        landingApproachTicks = 0;
        landingForceDrop = false;
        targetPosition = landingPosition;
        amphithere.setHovering(false);
        amphithere.setTakeoff(false);
    }

    private void finishLanding() {
        landingApproach = false;
        landingApproachTicks = 0;
        landingForceDrop = false;
        targetPosition = null;
        landingPosition = null;
        amphithere.handleAiLandingComplete();
        amphithere.setHovering(false);
        amphithere.setFlying(false);
    }

    private Vec3 findLandingTarget() {
        BlockPos origin = amphithere.blockPosition();
        int radius = 16; // Increased search radius

        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = amphithere.getRandom().nextInt(radius * 2 + 1) - radius;
            int dz = amphithere.getRandom().nextInt(radius * 2 + 1) - radius;
            BlockPos column = origin.offset(dx, 0, dz);
            if (!amphithere.level().hasChunkAt(column)) {
                continue;
            }

            // Use WORLD_SURFACE to get actual ground, not tree trunks
            int surfaceY = amphithere.level().getHeight(Heightmap.Types.WORLD_SURFACE,
                    column.getX(), column.getZ());
            BlockPos ground = new BlockPos(column.getX(), surfaceY - 1, column.getZ());
            if (isWideLandingSurface(ground)) {
                return new Vec3(column.getX() + 0.5, ground.getY() + 1.0, column.getZ() + 0.5);
            }
        }

        return null;
    }

    /**
     * Finds a valid drop target that's actually solid ground, not water
     * Searches in expanding radius around current position
     */
    private Vec3 findValidDropTarget() {
        BlockPos origin = amphithere.blockPosition();

        // Search in expanding radius for solid ground
        for (int radius = 0; radius <= 32; radius += 8) {
            for (int attempt = 0; attempt < 12; attempt++) {
                int dx = radius == 0 ? 0 : amphithere.getRandom().nextInt(radius * 2 + 1) - radius;
                int dz = radius == 0 ? 0 : amphithere.getRandom().nextInt(radius * 2 + 1) - radius;
                BlockPos checkPos = origin.offset(dx, 0, dz);

                if (!amphithere.level().hasChunkAt(checkPos)) {
                    continue;
                }

                // Get surface level
                int surfaceY = amphithere.level().getHeight(Heightmap.Types.WORLD_SURFACE,
                        checkPos.getX(), checkPos.getZ());
                BlockPos groundPos = new BlockPos(checkPos.getX(), surfaceY - 1, checkPos.getZ());

                var state = amphithere.level().getBlockState(groundPos);

                // Must be solid and not fluid
                if (!state.isAir() && state.getFluidState().isEmpty() &&
                    state.isFaceSturdy(amphithere.level(), groundPos, Direction.UP)) {
                    return new Vec3(checkPos.getX() + 0.5, groundPos.getY() + 1.0, checkPos.getZ() + 0.5);
                }
            }
        }

        return null; // No valid drop target found
    }

    /**
     * Checks if the landing surface is wide enough for the dragon's bounding box
     * Dragons are large creatures, so we check a 3x3 area
     */
    private boolean isWideLandingSurface(BlockPos ground) {
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
        return isLandingSpaceClear(ground);
    }

    private boolean isLandingSpaceClear(BlockPos ground) {
        BlockPos above = ground.above();
        BlockPos aboveTwo = above.above();
        var aboveState = amphithere.level().getBlockState(above);
        if (!aboveState.getCollisionShape(amphithere.level(), above).isEmpty()
                || !aboveState.getFluidState().isEmpty()) {
            return false;
        }
        var aboveTwoState = amphithere.level().getBlockState(aboveTwo);
        return aboveTwoState.getCollisionShape(amphithere.level(), aboveTwo).isEmpty()
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

        // Check if dragon is currently in a cave/enclosed space
        BlockPos dragonPos = amphithere.blockPosition();
        boolean canSeeSky = amphithere.level().canSeeSky(dragonPos);

        int groundY;
        double capAboveGround;

        if (canSeeSky) {
            // OUTDOOR: Use heightmap for normal flight
            groundY = amphithere.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);

            // Weather-based cap above ground - gliders avoid storms
            boolean thundering = amphithere.level().isThundering();
            boolean raining = !thundering && amphithere.level().isRaining();

            if (tethered) {
                capAboveGround = thundering ? 12.0 : (raining ? 18.0 : 32.0);
            } else {
                capAboveGround = thundering ? 20.0 : (raining ? 30.0 : 80.0);
            }
        } else {
            // CAVE/INDOOR: Find actual floor and ceiling, fly between them
            int surfaceY = amphithere.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);
            groundY = findGroundInCave(x, surfaceY, z);
            int ceilingY = findCeilingInCave(x, groundY, z);

            // Fly between 40-70% of the distance from floor to ceiling
            double caveFactor = tethered ? (0.4 + amphithere.getRandom().nextDouble() * 0.2) : // 40-60% for tethered
                                          (0.5 + amphithere.getRandom().nextDouble() * 0.2);  // 50-70% for free
            capAboveGround = (ceilingY - groundY) * caveFactor;

            // Ensure minimum clearance
            capAboveGround = Math.max(capAboveGround, tethered ? 8.0 : 12.0);
        }

        double base;
        if (tethered) {
            base = canSeeSky ? (12.0 + amphithere.getRandom().nextDouble() * 12.0) :
                               (5.0 + amphithere.getRandom().nextDouble() * 8.0); // Lower base in caves
        } else {
            base = canSeeSky ? (25.0 + amphithere.getRandom().nextDouble() * 35.0) :
                               (8.0 + amphithere.getRandom().nextDouble() * 15.0); // Lower base in caves
        }

        double target = groundY + base;
        double cap = groundY + capAboveGround;
        double worldCap = amphithere.level().getMaxBuildHeight() - 10.0;

        return Math.min(Math.min(target, cap), worldCap);
    }

    /**
     * Finds the actual ground level in a cave by searching downward
     */
    private int findGroundInCave(double x, double currentY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, currentY, z);

        // Search down to find solid ground
        while (pos.getY() > amphithere.level().getMinBuildHeight() &&
               !amphithere.level().getBlockState(pos).isSolid() &&
               amphithere.level().getFluidState(pos).isEmpty()) {
            pos.move(0, -1, 0);
        }

        return pos.getY();
    }

    /**
     * Finds the ceiling in a cave by searching upward from the floor
     */
    private int findCeilingInCave(double x, double floorY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, floorY + 2, z);

        // Search up to find ceiling
        while (pos.getY() < amphithere.level().getMaxBuildHeight() &&
               !amphithere.level().getBlockState(pos).isSolid()) {
            pos.move(0, 1, 0);
        }

        // Return ceiling position (subtract 1 to get the air block just below the solid ceiling)
        return Math.max((int) floorY + 10, pos.getY() - 1);
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
            return 2; // Fast decisions in storms to land quickly
        }
        if (raining) {
            return 5; // Quick decisions in rain to land
        }
        return 8; // Frequent decisions in clear weather - gliders want to soar!
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
            // Gliders avoid thunderstorms - very rare takeoff
            return amphithere.getRandom().nextInt(200) == 0; // 0.5% chance - gliders avoid storms
        } else if (raining) {
            // Gliders avoid rain - rare takeoff
            return amphithere.getRandom().nextInt(100) == 0; // 1% chance - gliders prefer clear weather
        } else {
            // Clear weather - gliders love to soar
            return amphithere.getRandom().nextInt(40) == 0; // 2.5% chance - frequent soaring in clear weather
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
            // Thunder: gliders land quickly in storms (~10 sec average)
            return amphithere.getRandom().nextInt(200) != 0;
        } else if (raining) {
            // Rain: gliders land quickly in rain (~20 sec average)
            return amphithere.getRandom().nextInt(400) != 0;
        } else {
            // Clear: gliders soar for long periods (~3 min average)
            return amphithere.getRandom().nextInt(3600) != 0;
        }
    }

    // ===== UTILITY METHODS =====

    /**
     * Check if there's enough vertical clearance above the dragon to safely take off
     * Prevents takeoff when surrounded by trees/blocks
     */
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

                    // Allow takeoff through leaves and other breakable vegetation
                    if (state.isAir() || isBreakableVegetation(state)) {
                        continue;
                    }

                    // Blocked by solid block
                    if (!state.getCollisionShape(amphithere.level(), checkPos).isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true; // Clear path upward
    }

    /**
     * Check if a block is breakable vegetation that won't stop takeoff
     */
    private boolean isBreakableVegetation(net.minecraft.world.level.block.state.BlockState state) {
        var block = state.getBlock();
        return block instanceof net.minecraft.world.level.block.LeavesBlock ||
               block instanceof net.minecraft.world.level.block.VineBlock ||
               block instanceof net.minecraft.world.level.block.TallGrassBlock ||
               block instanceof net.minecraft.world.level.block.FlowerBlock ||
               block instanceof net.minecraft.world.level.block.DoublePlantBlock;
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
