package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.core.BlockPos;
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
    private int stuckCounter = 0;
    private int timeSinceTargetChange = 0;

    // Landing cooldown to prevent immediate takeoff after landing
    private static final int LANDING_COOLDOWN_TICKS = 40; // 2 seconds minimum on ground (gliders want to fly!)
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
        // Don't interfere with landing sequence
        if (amphithere.isLanding()) {
            return false;
        }

        // Don't interfere with important behaviors
        if (amphithere.isVehicle() || amphithere.isPassenger() || amphithere.isOrderedToSit()) {
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
                isFlying = shouldTakeOff(thundering, raining);
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
                amphithere.setGoingUp(false);
                amphithere.setGoingDown(false);
                amphithere.setLanding(true);
                amphithere.setFlying(false);
                amphithere.setHovering(false);
                amphithere.setTakeoff(false);
                return false;
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
                // Dragon wants to land - trigger landing sequence
                amphithere.setLanding(true);
                amphithere.setFlying(false);
                amphithere.setTakeoff(false);
                amphithere.setHovering(false);
                return false;
            }
        }

        // Continue if we're flying and have a target
        // CRITICAL: Only continue if actually airborne (not on ground)
        // Allow brief grace period for takeoff (5 ticks = 0.25 seconds)
        if (amphithere.isFlying() && amphithere.onGround()) {
            if (timeSinceTargetChange > 5) { // Grace period for takeoff
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
        if (targetPosition != null) {
            amphithere.getMoveControl().setWantedPosition(targetPosition.x, targetPosition.y, targetPosition.z, amphithere.getFlightSpeed());
        }
    }

    @Override
    public void tick() {
        timeSinceTargetChange++;
        
        // If amphithere wants to land, let it handle that
        if (amphithere.isLanding()) {
            return;
        }

        // CRITICAL: Handle stuck state where isFlying=true but onGround=true
        // Allow brief grace period for takeoff (5 ticks = 0.25 seconds)
        if (amphithere.isFlying() && amphithere.onGround()) {
            if (timeSinceTargetChange > 5) { // Grace period for takeoff
                // Properly land the amphithere instead of just resetting states
                amphithere.setLanding(true);
                amphithere.setFlying(false);
                amphithere.setTakeoff(false);
                amphithere.setHovering(false);
                amphithere.markLandedNow();
                return;
            }
        }

        if (amphithere.isTame() && amphithere.getOwner() != null && !isOverDanger()) {
            amphithere.setLanding(true);
            amphithere.setFlying(false);
            amphithere.setHovering(false);
            amphithere.setTakeoff(false);
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

        double base;
        if (tethered) {
            base = 12.0 + amphithere.getRandom().nextDouble() * 12.0;
        } else {
            base = 25.0 + amphithere.getRandom().nextDouble() * 35.0;
        }

        // Weather-based cap above ground - gliders avoid storms
        boolean thundering = amphithere.level().isThundering();
        boolean raining = !thundering && amphithere.level().isRaining();
        double capAboveGround;
        if (tethered) {
            capAboveGround = thundering ? 12.0 : (raining ? 18.0 : 32.0);
        } else {
            capAboveGround = thundering ? 20.0 : (raining ? 30.0 : 80.0);
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
