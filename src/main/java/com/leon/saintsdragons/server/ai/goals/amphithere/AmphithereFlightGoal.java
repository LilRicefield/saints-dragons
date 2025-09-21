package com.leon.saintsdragons.server.ai.goals.amphithere;

import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import net.minecraft.core.BlockPos;
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
public class AmphithereFlightGoal extends Goal {
    private final AmphithereEntity dragon;
    private Vec3 targetPosition;
    private int stuckCounter = 0;
    private int timeSinceTargetChange = 0;

    // Landing cooldown to prevent immediate takeoff after landing
    private static final int LANDING_COOLDOWN_TICKS = 40; // 2 seconds minimum on ground (gliders want to fly!)
    private long lastLandingTime = 0;
    
    // Flight decision cooldown (slower than lightning dragon)
    private int flightDecisionCooldown = 0;
    
    // Weather state tracking for immediate response
    private boolean wasThundering = false;
    private boolean wasRaining = false;

    public AmphithereFlightGoal(AmphithereEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE));
        
        // Start with no offset
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

        // Weather state snapshot for this decision
        boolean thundering = dragon.level().isThundering();
        boolean raining = !thundering && dragon.level().isRaining();
        
        // Check for weather changes that should trigger immediate takeoff
        boolean weatherChangedToStorm = (thundering && !wasThundering) || (raining && !wasRaining);
        boolean weatherChangedToThunder = thundering && !wasThundering;
        
        // Update weather state tracking
        wasThundering = thundering;
        wasRaining = raining;

        // If tamed, gliders still want to soar but avoid storms
        if (dragon.isTame()) {
            var owner = dragon.getOwner();
            if (owner != null && dragon.distanceToSqr(owner) < 8.0 * 8.0) {
                // Only avoid takeoff in storms when over safe ground - gliders love soaring in clear weather
                if (isOverDanger() && (thundering || raining)) {
                    return false;
                }
            }
        }

        // Use server game time for landing cooldown checks
        long currentTime = dragon.level().getGameTime();
        int cooldown = LANDING_COOLDOWN_TICKS; // fixed
        if (thundering) cooldown = 0;            // no cooldown in thunder - gliders avoid storms
        else if (raining) cooldown = cooldown / 4; // shorter cooldown in rain - gliders prefer clear weather
        
        // Override cooldown if weather just changed to storm conditions
        if (weatherChangedToStorm) {
            cooldown = 0;
        }
        
        if (!dragon.isFlying() && (currentTime - lastLandingTime) < cooldown) {
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
            if (dragon.isFlying()) {
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
        if (dragon.isLanding()) {
            return false;
        }

        // Stop if ordered to sit or something important comes up
        if (dragon.isOrderedToSit() || dragon.isVehicle()) {
            return false;
        }

        // Stop if combat starts
        var target = dragon.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }

        // NEW: Check if dragon wants to land naturally
        boolean thundering = dragon.level().isThundering();
        boolean raining = !thundering && dragon.level().isRaining();
        if (dragon.isFlying() && !shouldKeepFlying(thundering, raining)) {
            // Dragon wants to land - trigger landing sequence
            dragon.setLanding(true);
            dragon.setFlying(false);
            dragon.setTakeoff(false);
            dragon.setHovering(false);
            return false;
        }

        // Continue if we're flying and have a target
        return dragon.isFlying() && targetPosition != null && dragon.distanceToSqr(targetPosition) > 9.0;
    }

    @Override
    public void start() {
        dragon.setFlying(true);
        dragon.setTakeoff(false);
        dragon.setLanding(false);
        dragon.setHovering(false);
        if (targetPosition != null) {
            dragon.getMoveControl().setWantedPosition(targetPosition.x, targetPosition.y, targetPosition.z, dragon.getFlightSpeed());
        }
    }

    @Override
    public void tick() {
        timeSinceTargetChange++;

        // If dragon wants to land, let it handle that
        if (dragon.isLanding()) {
            return;
        }

        // Check if we need a new target
        boolean needNewTarget = false;

        if (targetPosition == null) {
            needNewTarget = true;
        } else {
            double distanceToTarget = dragon.distanceToSqr(targetPosition);

            // Reached target - large completion distance for glider soaring
            if (distanceToTarget < 100.0) { // 100 blocks for long glider flights
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
            if (timeSinceTargetChange > 300) {
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
        stuckCounter = 0;
        timeSinceTargetChange = 0;
        dragon.getNavigation().stop();

        // NEW: Record landing time for cooldown
        if (!dragon.isFlying()) {
            lastLandingTime = dragon.level().getGameTime();
        }
    }

    // ===== FLIGHT TARGET FINDING =====

    private Vec3 findFlightTarget() {
        Vec3 dragonPos = dragon.position();

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

    private Vec3 generateFlightCandidate(Vec3 dragonPos, int attempt) {
        boolean isStuck = dragon.horizontalCollision || stuckCounter > 0;

        float maxRot = isStuck ? 360 : 180;
        // Large range for high-soaring glider behavior
        float range = isStuck ? 40.0f + dragon.getRandom().nextFloat() * 60.0f :
                80.0f + dragon.getRandom().nextFloat() * 120.0f; // 80-200 blocks for glider soaring

        float yRotOffset;
        if (isStuck && attempt < 8) {
            yRotOffset = (float) Math.toRadians(180 + dragon.getRandom().nextFloat() * 120 - 60);
        } else {
            yRotOffset = (float) Math.toRadians(dragon.getRandom().nextFloat() * maxRot - (maxRot / 2));
        }

        float xRotOffset = (float) Math.toRadians((dragon.getRandom().nextFloat() - 0.5f) * 20);

        Vec3 lookVec = dragon.getLookAngle();
        Vec3 targetVec = lookVec.scale(range).yRot(yRotOffset).xRot(xRotOffset);
        Vec3 candidate = dragonPos.add(targetVec);

        double targetY = findSafeFlightHeight(candidate.x, candidate.z);
        candidate = new Vec3(candidate.x, targetY, candidate.z);

        if (!dragon.level().isLoaded(BlockPos.containing(candidate))) {
            return null;
        }

        return candidate;
    }

    private double findSafeFlightHeight(double x, double z) {
        int ix = (int) x;
        int iz = (int) z;
        int groundY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);

        // High soaring altitude for glider behavior
        double base = 25.0 + dragon.getRandom().nextDouble() * 35.0; // 25..60 above surface for high soaring

        // Weather-based cap above ground - gliders avoid storms
        boolean thundering = dragon.level().isThundering();
        boolean raining = !thundering && dragon.level().isRaining();
        double capAboveGround = thundering ? 20.0 : (raining ? 30.0 : 80.0); // Much lower in storms, very high in clear weather

        double target = groundY + base;
        double cap = groundY + capAboveGround;
        double worldCap = dragon.level().getMaxBuildHeight() - 10.0;

        return Math.min(Math.min(target, cap), worldCap);
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
        return baseInterval + dragon.getRandom().nextInt(jitter);
    }

    private boolean shouldTakeOff(boolean thundering, boolean raining) {
        if (isOverDanger()) {
            return true;
        }

        if (thundering) {
            // Gliders avoid thunderstorms - very rare takeoff
            return dragon.getRandom().nextInt(200) == 0; // 0.5% chance - gliders avoid storms
        } else if (raining) {
            // Gliders avoid rain - rare takeoff
            return dragon.getRandom().nextInt(100) == 0; // 1% chance - gliders prefer clear weather
        } else {
            // Clear weather - gliders love to soar
            return dragon.getRandom().nextInt(40) == 0; // 2.5% chance - frequent soaring in clear weather
        }
    }

    private boolean shouldKeepFlying(boolean thundering, boolean raining) {
        if (isOverDanger()) {
            return true;
        }

        // Weather-weighted patrol durations - gliders avoid storms
        if (thundering) {
            // Thunder: gliders land quickly in storms (~10 sec average)
            return dragon.getRandom().nextInt(200) != 0;
        } else if (raining) {
            // Rain: gliders land quickly in rain (~20 sec average)
            return dragon.getRandom().nextInt(400) != 0;
        } else {
            // Clear: gliders soar for long periods (~3 min average)
            return dragon.getRandom().nextInt(3600) != 0;
        }
    }

    // ===== UTILITY METHODS =====

    private boolean isOverDanger() {
        BlockPos dragonPos = dragon.blockPosition();
        boolean foundSolid = false;
        boolean nearFluid = false;

        for (int i = 1; i <= 25; i++) {
            BlockPos checkPos = dragonPos.below(i);

            var state = dragon.level().getBlockState(checkPos);
            // Treat as solid ground if the block has a collision shape or sturdy top face
            if (!state.getCollisionShape(dragon.level(), checkPos).isEmpty() ||
                    state.isFaceSturdy(dragon.level(), checkPos, net.minecraft.core.Direction.UP)) {
                foundSolid = true;
                break;
            }

            // Consider fluids within 10 blocks below as dangerous (avoid landing in water/lava)
            if (i <= 10 && !dragon.level().getFluidState(checkPos).isEmpty()) {
                nearFluid = true;
                // No break: still continue to see if solid exists even closer
            }
        }

        // Dangerous if over fluid nearby, or no solid ground found and we're near world bottom (void-like)
        if (nearFluid) return true;
        return !foundSolid && dragonPos.getY() < dragon.level().getMinBuildHeight() + 20;
    }
}