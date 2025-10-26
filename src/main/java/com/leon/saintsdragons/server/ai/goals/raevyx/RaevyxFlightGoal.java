package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * CLEANED UP flight system that works with the new landing logic
 * Fixed the stupid infinite fly-land-fly loop
 */
public class RaevyxFlightGoal extends Goal {

    private final Raevyx wyvern;
    private Vec3 targetPosition;
    private int stuckCounter = 0;
    private int timeSinceTargetChange = 0;

    // NEW: Landing cooldown to prevent immediate takeoff after landing
    private static final int LANDING_COOLDOWN_TICKS = 100; // 5 seconds minimum on ground
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
        // Don't interfere with landing sequence
        if (wyvern.isLanding()) {
            return false;
        }

        // Don't interfere with important behaviors
        if (wyvern.isVehicle() || wyvern.isPassenger() || wyvern.isOrderedToSit()) {
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

        // If tamed, don't take off due to weather - only take off if in danger
        if (wyvern.isTame()) {
            var owner = wyvern.getOwner();
            if (owner != null && wyvern.distanceToSqr(owner) < 15.0 * 15.0) {
                // Only take off if over danger, not due to weather
                if (!isOverDanger()) {
                    return false;
                }
            }
        }

        // Use server game time for landing cooldown checks
        long currentTime = wyvern.level().getGameTime();
        int cooldown = LANDING_COOLDOWN_TICKS; // fixed
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
        if (wyvern.isLanding()) {
            return false;
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
            // Dragon wants to land - trigger landing sequence
            wyvern.setLanding(true);
            wyvern.setFlying(false);
            wyvern.setTakeoff(false);
            wyvern.setHovering(false);
            return false;
        }

        // Continue if we're flying and have a target
        return wyvern.isFlying() && targetPosition != null && wyvern.distanceToSqr(targetPosition) > 9.0;
    }

    @Override
    public void start() {
        wyvern.setFlying(true);
        wyvern.setTakeoff(false);
        wyvern.setLanding(false);
        wyvern.setHovering(false);
        if (targetPosition != null) {
            wyvern.getMoveControl().setWantedPosition(targetPosition.x, targetPosition.y, targetPosition.z, 1.0);
        }
    }

    @Override
    public void tick() {
        timeSinceTargetChange++;

        // If wyvern wants to land, let it handle that
        if (wyvern.isLanding()) {
            return;
        }

        // Check if we need a new target
        boolean needNewTarget = false;

        if (targetPosition == null) {
            needNewTarget = true;
        } else {
            double distanceToTarget = wyvern.distanceToSqr(targetPosition);

            // Reached target - much larger completion distance
            if (distanceToTarget < 64.0) {
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
            if (timeSinceTargetChange > 300) {
                needNewTarget = true;
            }
        }

        if (needNewTarget) {
            targetPosition = findFlightTarget();
            timeSinceTargetChange = 0;
            wyvern.getMoveControl().setWantedPosition(targetPosition.x, targetPosition.y, targetPosition.z, 1.0);
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
        int groundY = wyvern.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);

        // Base hover altitude above ground
        double base = 15.0 + wyvern.getRandom().nextDouble() * 20.0; // 15..35 above surface

        // Weather-based cap above ground
        boolean thundering = wyvern.level().isThundering();
        boolean raining = !thundering && wyvern.level().isRaining();
        double capAboveGround = thundering ? 90.0 : (raining ? 70.0 : 50.0);

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
            return 2; // Very frequent decisions during thunder
        }
        if (raining) {
            return 8; // More frequent decisions during rain
        }
        return 25; // Normal frequency during clear weather
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
            // Much more aggressive takeoff during thunder - almost immediate
            return wyvern.getRandom().nextInt(4) == 0; // 25% chance per decision interval
        } else if (raining) {
            // More frequent takeoff during rain
            return wyvern.getRandom().nextInt(8) == 0; // 12.5% chance per decision interval
        } else {
            return wyvern.getRandom().nextInt(80) == 0; // 1.25% chance per decision interval
        }
    }

    private boolean shouldKeepFlying(boolean thundering, boolean raining) {
        if (isOverDanger()) {
            return true;
        }

        // Weather-weighted patrol durations
        if (thundering) {
            // Thunder: long aerial patrols (~2.5 min average)
            return wyvern.getRandom().nextInt(3000) != 0;
        } else if (raining) {
            // Rain: medium patrols (~90 sec average)
            return wyvern.getRandom().nextInt(1800) != 0;
        } else {
            // Clear: short patrols (~10 sec average), then land
            return wyvern.getRandom().nextInt(200) != 0;
        }
    }

    // ===== UTILITY METHODS =====

    /**
     * Check if there are baby Raevyx nearby that this parent should protect
     */
    private boolean hasNearbyBabies() {
        return !wyvern.level().getEntitiesOfClass(
                Raevyx.class,
                wyvern.getBoundingBox().inflate(16.0D),  // Check 16 block radius
                baby -> baby != null && baby.isBaby() && baby.isAlive()
        ).isEmpty();
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
