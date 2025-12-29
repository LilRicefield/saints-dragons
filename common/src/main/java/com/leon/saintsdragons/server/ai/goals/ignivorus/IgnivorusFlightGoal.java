package com.leon.saintsdragons.server.ai.goals.ignivorus;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
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
 * Flight goal for Ignivorus - provides aggressive patrol behavior.
 * Fire dragons are bold and active, preferring to patrol their territory from the air.
 */
public class IgnivorusFlightGoal extends Goal {
    private final Ignivorus dragon;
    private Vec3 targetPosition;
    private Vec3 landingPosition;
    private boolean landingApproach;
    private int landingApproachTicks = 0;
    private boolean landingForceDrop = false;
    private int stuckCounter = 0;
    private int timeSinceTargetChange = 0;

    // Landing cooldown to prevent immediate takeoff after landing
    private static final int LANDING_COOLDOWN_TICKS = 60; // 3 seconds minimum on ground
    private long lastLandingTime = 0;
    private static final int LANDING_FORCE_DROP_TICKS = 80;
    private static final int LANDING_EMERGENCY_GROUNDING_TICKS = 100; // 5 seconds - force ground if stuck

    // Flight decision cooldown
    private int flightDecisionCooldown = 0;

    public IgnivorusFlightGoal(Ignivorus dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.flightDecisionCooldown = 0;
    }

    @Override
    public boolean canUse() {
        // Don't interfere with landing sequence
        if (dragon.isLanding()) {
            return false;
        }

        // Don't interfere with important behaviors
        if (dragon.isVehicle() || dragon.isPassenger() || dragon.isOrderedToSit()) {
            return false;
        }

        // Don't take off while sleeping or waking up
        if (dragon.isSleeping() || dragon.isSleepingExiting()) {
            return false;
        }

        // Tamed dragons should NEVER randomly patrol - only fly for danger escape
        if (dragon.isTame()) {
            // Only allow this goal if over immediate danger (water/lava/void)
            if (!isOverDanger()) {
                return false;
            }
        }

        // Use server game time for landing cooldown checks
        long currentTime = dragon.level().getGameTime();
        int cooldown = LANDING_COOLDOWN_TICKS;

        if (!dragon.isFlying() && (currentTime - lastLandingTime) < cooldown) {
            return false;
        }

        // Use desynced cooldown to prevent all dragons making flight decisions same tick
        int decisionInterval = flightDecisionInterval();
        if (flightDecisionCooldown > 0) {
            flightDecisionCooldown--;
            if (flightDecisionCooldown > 0) {
                return false;
            }
        }

        // Must fly if over danger
        boolean shouldFly;
        if (isOverDanger()) {
            shouldFly = true;
        } else {
            // Normal flight decisions
            if (dragon.isFlying()) {
                shouldFly = shouldKeepFlying();
            } else {
                // Check for clearance before takeoff
                if (!hasTakeoffClearance()) {
                    shouldFly = false;
                } else {
                    shouldFly = shouldTakeOff();
                }
            }
        }

        if (shouldFly) {
            landingApproach = false;
            landingPosition = null;
            this.targetPosition = findFlightTarget();
            this.flightDecisionCooldown = nextDecisionCooldown(decisionInterval);
            return true;
        }

        this.flightDecisionCooldown = nextDecisionCooldown(decisionInterval);
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (landingApproach) {
            if (dragon.onGround()) {
                finishLanding();
                return false;
            }
            return true;
        }

        // Let landing system take over
        if (dragon.isLanding()) {
            return false;
        }

        // Stop if ordered to sit or something important comes up
        if (dragon.isOrderedToSit() || dragon.isVehicle()) {
            return false;
        }

        // Tamed dragons should land immediately once danger is cleared
        if (dragon.isTame() && !isOverDanger()) {
            beginLandingApproach();
            return true;
        }

        // Stop if combat starts
        var target = dragon.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }

        // Check if dragon wants to land naturally
        if (dragon.isFlying() && !shouldKeepFlying() && !isOverDanger()) {
            beginLandingApproach();
            return true;
        }

        // Handle stuck state where isFlying=true but onGround=true
        if (dragon.isFlying() && dragon.onGround()) {
            if (timeSinceTargetChange > 5) { // Grace period for takeoff
                finishLanding();
                return false;
            }
        }

        return dragon.isFlying() && targetPosition != null && dragon.distanceToSqr(targetPosition) > 9.0;
    }

    @Override
    public void start() {
        dragon.setFlying(true);
        dragon.setLanding(false);
        dragon.setHovering(false);
        landingApproach = false;
        landingPosition = null;
        if (targetPosition != null) {
            dragon.getMoveControl().setWantedPosition(targetPosition.x, targetPosition.y, targetPosition.z, dragon.getFlightSpeed());
        }
    }

    @Override
    public void tick() {
        timeSinceTargetChange++;

        if (landingApproach) {
            if (dragon.isInWaterOrBubble()) {
                landingApproach = false;
                landingApproachTicks = 0;
                landingForceDrop = false;
                targetPosition = null;
                landingPosition = null;
                dragon.setLanding(false);
                dragon.setHovering(false);
                dragon.setTakeoff(false);
                dragon.setFlying(false);
                return;
            }
            landingApproachTicks++;

            // Emergency grounding: if stuck floating for too long, abort landing and let entity fall
            if (landingApproachTicks > LANDING_EMERGENCY_GROUNDING_TICKS && !dragon.onGround()) {
                landingApproach = false;
                landingApproachTicks = 0;
                landingForceDrop = false;
                targetPosition = null;
                landingPosition = null;
                dragon.setNoGravity(false);
                dragon.setFlying(false);
                dragon.setLanding(true);
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
                    dragon.setLanding(false);
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
                        dragon.setLanding(false);
                        return;
                    }
                }
                double altitude = dragon.getY() - landingPosition.y;

                // Apply downward velocity throughout descent, not just when far away
                if (!dragon.isInWaterOrBubble() && !dragon.onGround()) {
                    Vec3 motion = dragon.getDeltaMovement();
                    // Stronger descent when high, gentler when close
                    double descentRate = altitude > Ignivorus.LANDING_BLEND_ALTITUDE ? 0.18 : 0.08;
                    double newY = Math.max(motion.y - descentRate, -1.6);
                    dragon.setDeltaMovement(motion.x, newY, motion.z);
                }
                dragon.getMoveControl().setWantedPosition(landingPosition.x, landingPosition.y, landingPosition.z, 1.6);
                if (!dragon.isLanding()
                        && altitude >= -0.25D
                        && altitude <= Ignivorus.LANDING_BLEND_ALTITUDE) {
                    dragon.setLanding(true);
                }
            }
            return;
        }

        // If dragon wants to land, let it handle that
        if (dragon.isLanding()) {
            return;
        }

        // Handle stuck state where isFlying=true but onGround=true
        if (dragon.isFlying() && dragon.onGround()) {
            if (timeSinceTargetChange > 5) {
                finishLanding();
                return;
            }
        }

        // Tamed dragons should land immediately once danger is cleared
        if (dragon.isTame() && !isOverDanger()) {
            beginLandingApproach();
            return;
        }

        // Check if we need a new target
        boolean needNewTarget = false;

        if (targetPosition == null) {
            needNewTarget = true;
        } else {
            double distanceToTarget = dragon.distanceToSqr(targetPosition);

            // Reached target - larger acceptance radius for smooth flight
            if (distanceToTarget < 64.0) { // 8 blocks - matches Raevyx
                needNewTarget = true;
            }

            // Check if move controller gave up (collision handling)
            if (dragon.horizontalCollision && distanceToTarget > 25.0) {
                needNewTarget = true;
                stuckCounter = 0;
            }

            // Better stuck detection
            if (dragon.horizontalCollision && timeSinceTargetChange % 5 == 0) {
                stuckCounter++;
                if (stuckCounter > 2) {
                    needNewTarget = true;
                    stuckCounter = 0;
                }
            } else if (!dragon.horizontalCollision) {
                stuckCounter = Math.max(0, stuckCounter - 1);
            }

            // Periodic path validation
            if (dragon.tickCount % 20 == 0) {
                if (!isValidFlightTarget(targetPosition)) {
                    needNewTarget = true;
                }
            }

            // Been going to same target for too long
            if (timeSinceTargetChange > 400) { // ~20 seconds
                needNewTarget = true;
            }
        }

        if (needNewTarget) {
            targetPosition = findFlightTarget();
            timeSinceTargetChange = 0;
            dragon.getMoveControl().setWantedPosition(targetPosition.x, targetPosition.y, targetPosition.z, dragon.getFlightSpeed());
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
        dragon.getNavigation().stop();

        // Record landing time for cooldown
        if (!dragon.isFlying()) {
            lastLandingTime = dragon.level().getGameTime();
        }
    }

    // ===== FLIGHT TARGET FINDING =====

    private Vec3 findFlightTarget() {
        Vec3 dragonPos = dragon.position();
        Vec3 anchor = getFlightAnchor();

        // Try multiple attempts to find valid target
        for (int attempts = 0; attempts < 16; attempts++) {
            Vec3 candidate = generateFlightCandidate(anchor, dragonPos, attempts);

            if (isValidFlightTarget(candidate)) {
                return candidate;
            }
        }

        // Fallback: safe position above anchor
        return new Vec3(anchor.x, findSafeFlightHeight(anchor.x, anchor.z, false), anchor.z);
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
        dragon.setHovering(false);
        dragon.setTakeoff(false);
    }

    private void finishLanding() {
        landingApproach = false;
        landingApproachTicks = 0;
        landingForceDrop = false;
        targetPosition = null;
        landingPosition = null;
        dragon.handleAiLandingComplete();
        dragon.setHovering(false);
        dragon.setFlying(false);
    }

    private Vec3 findLandingTarget() {
        BlockPos origin = dragon.blockPosition();
        int radius = 16; // Increased search radius

        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = dragon.getRandom().nextInt(radius * 2 + 1) - radius;
            int dz = dragon.getRandom().nextInt(radius * 2 + 1) - radius;
            BlockPos column = origin.offset(dx, 0, dz);
            if (!dragon.level().hasChunkAt(column)) {
                continue;
            }

            // Use WORLD_SURFACE to get actual ground, not tree trunks
            int surfaceY = dragon.level().getHeight(Heightmap.Types.WORLD_SURFACE,
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
        BlockPos origin = dragon.blockPosition();

        // Search in expanding radius for solid ground
        for (int radius = 0; radius <= 32; radius += 8) {
            for (int attempt = 0; attempt < 12; attempt++) {
                int dx = radius == 0 ? 0 : dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                int dz = radius == 0 ? 0 : dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                BlockPos checkPos = origin.offset(dx, 0, dz);

                if (!dragon.level().hasChunkAt(checkPos)) {
                    continue;
                }

                // Get surface level
                int surfaceY = dragon.level().getHeight(Heightmap.Types.WORLD_SURFACE,
                        checkPos.getX(), checkPos.getZ());
                BlockPos groundPos = new BlockPos(checkPos.getX(), surfaceY - 1, checkPos.getZ());

                var state = dragon.level().getBlockState(groundPos);

                // Must be solid and not fluid
                if (!state.isAir() && state.getFluidState().isEmpty() &&
                    state.isFaceSturdy(dragon.level(), groundPos, Direction.UP)) {
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
        if (!dragon.level().hasChunkAt(ground)) {
            return false;
        }

        var state = dragon.level().getBlockState(ground);
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (!state.isFaceSturdy(dragon.level(), ground, Direction.UP)) {
            return false;
        }
        return isLandingSpaceClear(ground);
    }

    private boolean isLandingSpaceClear(BlockPos ground) {
        BlockPos above = ground.above();
        BlockPos aboveTwo = above.above();
        var aboveState = dragon.level().getBlockState(above);
        if (!aboveState.getCollisionShape(dragon.level(), above).isEmpty()
                || !aboveState.getFluidState().isEmpty()) {
            return false;
        }
        var aboveTwoState = dragon.level().getBlockState(aboveTwo);
        return aboveTwoState.getCollisionShape(dragon.level(), aboveTwo).isEmpty()
                && aboveTwoState.getFluidState().isEmpty();
    }

    private Vec3 generateFlightCandidate(Vec3 anchor, Vec3 dragonPos, int attempt) {
        boolean isStuck = dragon.horizontalCollision || stuckCounter > 0;

        boolean tethered = isTamedWander();

        Vec3 candidate;

        if (tethered) {
            // Tamed wander mode: patrol around owner
            double min = 15.0 + dragon.getRandom().nextDouble() * 10.0;
            double max = 35.0 + dragon.getRandom().nextDouble() * 15.0;
            double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0;
            double radius = min + dragon.getRandom().nextDouble() * (max - min);
            double cx = anchor.x + Math.cos(angle) * radius;
            double cz = anchor.z + Math.sin(angle) * radius;
            double targetY = findSafeFlightHeight(cx, cz, true);
            candidate = new Vec3(cx, targetY, cz);
        } else {
            // Wild/untamed: aggressive patrol behavior
            float maxRot = isStuck ? 360 : 180;
            float range = isStuck ? 30.0f + dragon.getRandom().nextFloat() * 40.0f :
                    50.0f + dragon.getRandom().nextFloat() * 70.0f;

            float yRotOffset;
            if (isStuck && attempt < 8) {
                yRotOffset = (float) Math.toRadians(180 + dragon.getRandom().nextFloat() * 120 - 60);
            } else {
                yRotOffset = (float) Math.toRadians(dragon.getRandom().nextFloat() * maxRot - (maxRot / 2));
            }

            float xRotOffset = (float) Math.toRadians((dragon.getRandom().nextFloat() - 0.5f) * 30);

            Vec3 lookVec = dragon.getLookAngle();
            Vec3 targetVec = lookVec.scale(range).yRot(yRotOffset).xRot(xRotOffset);
            Vec3 raw = dragonPos.add(targetVec);
            double targetY = findSafeFlightHeight(raw.x, raw.z, false);
            candidate = new Vec3(raw.x, targetY, raw.z);
        }

        if (!dragon.level().isLoaded(BlockPos.containing(candidate))) {
            return null;
        }

        return candidate;
    }

    private double findSafeFlightHeight(double x, double z, boolean tethered) {
        int ix = (int) x;
        int iz = (int) z;

        // Check if dragon is currently in a cave/enclosed space
        BlockPos dragonPos = dragon.blockPosition();
        boolean canSeeSky = dragon.level().canSeeSky(dragonPos);

        int groundY;
        double capAboveGround;

        if (canSeeSky) {
            // OUTDOOR: Use heightmap for normal flight
            groundY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);
            capAboveGround = tethered ? 40.0 : 60.0;
        } else {
            // CAVE/INDOOR: Find actual floor and ceiling, fly between them
            int surfaceY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);
            groundY = findGroundInCave(x, surfaceY, z);
            int ceilingY = findCeilingInCave(x, groundY, z);

            // Fly between 50-75% of the distance from floor to ceiling (fire dragons more aggressive)
            double caveFactor = tethered ? (0.45 + dragon.getRandom().nextDouble() * 0.2) : // 45-65% for tethered
                                          (0.55 + dragon.getRandom().nextDouble() * 0.2);  // 55-75% for free
            capAboveGround = (ceilingY - groundY) * caveFactor;

            // Ensure minimum clearance
            capAboveGround = Math.max(capAboveGround, tethered ? 10.0 : 15.0);
        }

        double base;
        if (tethered) {
            // Tamed: moderate altitude around owner
            base = canSeeSky ? (15.0 + dragon.getRandom().nextDouble() * 15.0) :
                               (8.0 + dragon.getRandom().nextDouble() * 12.0); // Lower base in caves
        } else {
            // Wild: aggressive patrol at medium-high altitude
            base = canSeeSky ? (20.0 + dragon.getRandom().nextDouble() * 25.0) :
                               (10.0 + dragon.getRandom().nextDouble() * 18.0); // Lower base in caves
        }

        double target = groundY + base;
        double cap = groundY + capAboveGround;
        double worldCap = dragon.level().getMaxBuildHeight() - 10.0;

        return Math.min(Math.min(target, cap), worldCap);
    }

    /**
     * Finds the actual ground level in a cave by searching downward
     */
    private int findGroundInCave(double x, double currentY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, currentY, z);

        // Search down to find solid ground
        while (pos.getY() > dragon.level().getMinBuildHeight() &&
               !dragon.level().getBlockState(pos).isSolid() &&
               dragon.level().getFluidState(pos).isEmpty()) {
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
        while (pos.getY() < dragon.level().getMaxBuildHeight() &&
               !dragon.level().getBlockState(pos).isSolid()) {
            pos.move(0, 1, 0);
        }

        // Return ceiling position (subtract 1 to get the air block just below the solid ceiling)
        return Math.max((int) floorY + 10, pos.getY() - 1);
    }

    private Vec3 getFlightAnchor() {
        if (isTamedWander()) {
            LivingEntity owner = dragon.getOwner();
            if (owner != null) {
                return owner.position();
            }
        }
        return dragon.position();
    }

    private boolean isTamedWander() {
        return dragon.isTame() && dragon.getCommand() == 2 && dragon.getOwner() != null;
    }

    private boolean isValidFlightTarget(Vec3 target) {
        if (target == null) return false;

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

        return distanceToHit > distanceToTarget * 0.95;
    }

    // ===== DECISION MAKING =====

    private int flightDecisionInterval() {
        return 10; // Check every ~0.5 seconds - fire dragons are active
    }

    private int nextDecisionCooldown(int baseInterval) {
        int jitter = Math.max(1, baseInterval / 2);
        return baseInterval + dragon.getRandom().nextInt(jitter);
    }

    private boolean shouldTakeOff() {
        if (isOverDanger()) {
            return true;
        }

        // Fire dragons are bold - higher chance to take off
        return dragon.getRandom().nextInt(30) == 0; // ~3.3% chance
    }

    private boolean shouldKeepFlying() {
        if (isOverDanger()) {
            return true;
        }

        // Fire dragons patrol for extended periods (~2-3 minutes average)
        return dragon.getRandom().nextInt(3000) != 0;
    }

    // ===== UTILITY METHODS =====

    /**
     * Check if there's enough vertical clearance above the dragon to safely take off
     * Prevents takeoff when surrounded by trees/blocks
     */
    private boolean hasTakeoffClearance() {
        BlockPos dragonPos = dragon.blockPosition();
        double dragonWidth = dragon.getBbWidth();
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
                    var state = dragon.level().getBlockState(checkPos);

                    // Allow takeoff through leaves and other breakable vegetation
                    if (state.isAir() || isBreakableVegetation(state)) {
                        continue;
                    }

                    // Blocked by solid block
                    if (!state.getCollisionShape(dragon.level(), checkPos).isEmpty()) {
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
        BlockPos dragonPos = dragon.blockPosition();
        boolean foundSolid = false;
        boolean nearFluid = false;

        for (int i = 1; i <= 25; i++) {
            BlockPos checkPos = dragonPos.below(i);

            var state = dragon.level().getBlockState(checkPos);
            if (!state.getCollisionShape(dragon.level(), checkPos).isEmpty() ||
                    state.isFaceSturdy(dragon.level(), checkPos, net.minecraft.core.Direction.UP)) {
                foundSolid = true;
                break;
            }

            // Consider fluids within 10 blocks below as dangerous
            if (i <= 10 && !dragon.level().getFluidState(checkPos).isEmpty()) {
                nearFluid = true;
            }
        }

        // Dangerous if over fluid nearby, or no solid ground found and we're near world bottom
        if (nearFluid) return true;
        return !foundSolid && dragonPos.getY() < dragon.level().getMinBuildHeight() + 20;
    }
}
