package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.ai.navigation.pathfinding.AsyncPathfindingHelper;
import com.leon.saintsdragons.server.ai.navigation.pathfinding.PathfindingResult;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Enhanced Raevyx flight goal using async A* pathfinding.
 *
 * Raevyx is a FAST, aggressive storm-lover - this pathfinder is tuned for speed:
 * - Slightly coarser grid (2 blocks) for faster computation
 * - Aggressive re-pathing when stuck
 * - Handles high-speed flight smoothly
 *
 * Based on RaevyxFlightGoal but with pathfinding integration.
 */
public class RaevyxSmartFlightGoal extends Goal {
    private final Raevyx raevyx;
    private Vec3 finalTarget;
    private List<Vec3> currentPath;
    private int currentWaypointIndex;

    private int stuckCounter = 0;
    private int timeSinceTargetChange = 0;
    private boolean pathfindingInProgress = false;

    private static final int LANDING_COOLDOWN_TICKS = 100;
    private long lastLandingTime = 0;

    private int flightDecisionCooldown = 0;
    private boolean wasThundering = false;
    private boolean wasRaining = false;

    public RaevyxSmartFlightGoal(Raevyx raevyx) {
        this.raevyx = raevyx;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.flightDecisionCooldown = 0;
    }

    @Override
    public boolean canUse() {
        if (raevyx.isLanding() || raevyx.isVehicle() ||
            raevyx.isPassenger() || raevyx.isOrderedToSit()) {
            return false;
        }

        // Parents shouldn't fly away and abandon babies
        if (!raevyx.isBaby() && hasNearbyBabies() && !isOverDanger()) {
            return false;
        }

        boolean thundering = raevyx.level().isThundering();
        boolean raining = !thundering && raevyx.level().isRaining();

        boolean weatherChangedToStorm = (thundering && !wasThundering) || (raining && !wasRaining);
        boolean weatherChangedToThunder = thundering && !wasThundering;

        wasThundering = thundering;
        wasRaining = raining;

        // Tamed Raevyx stay near owner
        if (raevyx.isTame()) {
            var owner = raevyx.getOwner();
            if (owner != null && raevyx.distanceToSqr(owner) < 15.0 * 15.0) {
                if (!isOverDanger()) {
                    return false;
                }
            }
        }

        long currentTime = raevyx.level().getGameTime();
        int cooldown = LANDING_COOLDOWN_TICKS;
        if (thundering) cooldown = 0;
        else if (raining) cooldown = cooldown / 4;

        if (weatherChangedToStorm) cooldown = 0;

        if (!raevyx.isFlying() && (currentTime - lastLandingTime) < cooldown) {
            return false;
        }

        int decisionInterval = flightDecisionInterval(thundering, raining);
        if (flightDecisionCooldown > 0) {
            flightDecisionCooldown--;
            if (flightDecisionCooldown > 0) {
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

        boolean isFlying;
        if (isOverDanger()) {
            isFlying = true;
        } else {
            if (raevyx.isFlying()) {
                isFlying = shouldKeepFlying(thundering, raining);
            } else {
                isFlying = shouldTakeOff(thundering, raining);
            }
        }

        if (isFlying) {
            requestNewFlightPath();
            this.flightDecisionCooldown = nextDecisionCooldown(decisionInterval);
            return true;
        }

        this.flightDecisionCooldown = nextDecisionCooldown(decisionInterval);
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (raevyx.isLanding() || raevyx.isOrderedToSit() || raevyx.isVehicle()) {
            return false;
        }

        if (raevyx.getTarget() != null && raevyx.getTarget().isAlive()) {
            return false;
        }

        boolean thundering = raevyx.level().isThundering();
        boolean raining = !thundering && raevyx.level().isRaining();
        if (raevyx.isFlying() && !shouldKeepFlying(thundering, raining)) {
            raevyx.setLanding(true);
            raevyx.setFlying(false);
            raevyx.setTakeoff(false);
            raevyx.setHovering(false);
            return false;
        }

        return raevyx.isFlying() &&
               (hasWaypointsRemaining() || (finalTarget != null && raevyx.distanceToSqr(finalTarget) > 9.0));
    }

    @Override
    public void start() {
        raevyx.setFlying(true);
        raevyx.setLanding(false);
        raevyx.setHovering(false);
        raevyx.setTakeoff(true);

        if (hasWaypointsRemaining()) {
            moveToNextWaypoint();
        }
    }

    @Override
    public void tick() {
        timeSinceTargetChange++;

        if (raevyx.isLanding()) {
            return;
        }

        // Follow path waypoints
        if (hasWaypointsRemaining()) {
            Vec3 currentWaypoint = currentPath.get(currentWaypointIndex);
            double distToWaypoint = raevyx.distanceToSqr(currentWaypoint);

            // Raevyx is FAST - larger waypoint acceptance radius
            if (distToWaypoint < 25.0) { // 5 blocks (vs Cindervane's 4)
                currentWaypointIndex++;
                if (hasWaypointsRemaining()) {
                    moveToNextWaypoint();
                }
            }

            // Aggressive stuck detection for fast dragon
            if (raevyx.horizontalCollision) {
                stuckCounter++;
                // Re-path faster than Cindervane (2 vs 3)
                if (stuckCounter > 2) {
                    requestNewFlightPath();
                    stuckCounter = 0;
                }
            } else {
                stuckCounter = Math.max(0, stuckCounter - 1);
            }
        } else if (finalTarget != null) {
            double distToTarget = raevyx.distanceToSqr(finalTarget);

            // Raevyx reaches targets faster - re-path when close or after timeout
            if (distToTarget < 64.0 || timeSinceTargetChange > 300) {
                requestNewFlightPath();
            }
        } else {
            requestNewFlightPath();
        }
    }

    @Override
    public void stop() {
        finalTarget = null;
        currentPath = null;
        currentWaypointIndex = 0;
        pathfindingInProgress = false;
        stuckCounter = 0;
        timeSinceTargetChange = 0;
        raevyx.getNavigation().stop();

        if (!raevyx.isFlying()) {
            lastLandingTime = raevyx.level().getGameTime();
        }
    }

    // ===== PATHFINDING INTEGRATION =====

    private void requestNewFlightPath() {
        if (pathfindingInProgress) {
            return;
        }

        Vec3 targetPos = findFlightTarget();
        if (targetPos == null) {
            return;
        }

        this.finalTarget = targetPos;
        timeSinceTargetChange = 0;

        if (raevyx.level() instanceof ServerLevel serverLevel) {
            pathfindingInProgress = true;

            // Raevyx is FAST - use 2-block grid for quicker computation
            AsyncPathfindingHelper.requestPath(
                serverLevel,
                raevyx.position(),
                targetPos,
                2, // Grid resolution (same as Cindervane)
                raevyx.getBoundingBox(), // Use actual dragon size
                result -> {
                    pathfindingInProgress = false;

                    if (result.isSuccess()) {
                        List<Vec3> path = result.getPath();

                        AsyncPathfindingHelper.scheduleOnMainThread(serverLevel, () -> {
                            currentPath = new ArrayList<>(path);
                            currentWaypointIndex = 0;
                            moveToNextWaypoint();
                        });
                    } else {
                        // Fallback: fly direct
                        AsyncPathfindingHelper.scheduleOnMainThread(serverLevel, () -> {
                            currentPath = null;
                            raevyx.getMoveControl().setWantedPosition(
                                targetPos.x, targetPos.y, targetPos.z, 1.0
                            );
                        });
                    }
                }
            );
        } else {
            raevyx.getMoveControl().setWantedPosition(
                targetPos.x, targetPos.y, targetPos.z, 1.0
            );
        }
    }

    private void moveToNextWaypoint() {
        if (!hasWaypointsRemaining()) {
            return;
        }

        Vec3 waypoint = currentPath.get(currentWaypointIndex);
        raevyx.getMoveControl().setWantedPosition(waypoint.x, waypoint.y, waypoint.z, 1.0);
    }

    private boolean hasWaypointsRemaining() {
        return currentPath != null && currentWaypointIndex < currentPath.size();
    }

    // ===== RAEVYX FLIGHT LOGIC =====

    private Vec3 findFlightTarget() {
        Vec3 dragonPos = raevyx.position();

        // Normal flight target generation
        for (int attempts = 0; attempts < 16; attempts++) {
            Vec3 candidate = generateFlightCandidate(dragonPos, attempts);

            if (isValidFlightTarget(candidate)) {
                return candidate;
            }
        }

        return new Vec3(dragonPos.x, findSafeFlightHeight(dragonPos.x, dragonPos.z), dragonPos.z);
    }

    private Vec3 generateFlightCandidate(Vec3 dragonPos, int attempt) {
        boolean isStuck = raevyx.horizontalCollision || stuckCounter > 0;

        float maxRot = isStuck ? 360 : 180;
        float range = isStuck ? 30.0f + raevyx.getRandom().nextFloat() * 40.0f :
                50.0f + raevyx.getRandom().nextFloat() * 80.0f; // Raevyx's ranges

        float yRotOffset;
        if (isStuck && attempt < 8) {
            yRotOffset = (float) Math.toRadians(180 + raevyx.getRandom().nextFloat() * 120 - 60);
        } else {
            yRotOffset = (float) Math.toRadians(raevyx.getRandom().nextFloat() * maxRot - (maxRot / 2));
        }

        float xRotOffset = (float) Math.toRadians((raevyx.getRandom().nextFloat() - 0.5f) * 20);

        Vec3 lookVec = raevyx.getLookAngle();
        Vec3 targetVec = lookVec.scale(range).yRot(yRotOffset).xRot(xRotOffset);
        Vec3 candidate = dragonPos.add(targetVec);

        double targetY = findSafeFlightHeight(candidate.x, candidate.z);
        candidate = new Vec3(candidate.x, targetY, candidate.z);

        if (!raevyx.level().isLoaded(BlockPos.containing(candidate))) {
            return null;
        }

        return candidate;
    }

    private double findSafeFlightHeight(double x, double z) {
        int ix = (int) x;
        int iz = (int) z;
        int groundY = raevyx.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);

        double base = 15.0 + raevyx.getRandom().nextDouble() * 20.0; // 15..35 above surface

        // Raevyx LOVES storms - flies HIGHER in bad weather (opposite of Cindervane!)
        boolean thundering = raevyx.level().isThundering();
        boolean raining = !thundering && raevyx.level().isRaining();
        double capAboveGround = thundering ? 90.0 : (raining ? 70.0 : 50.0);

        double target = groundY + base;
        double cap = groundY + capAboveGround;
        double worldCap = raevyx.level().getMaxBuildHeight() - 10.0;

        return Math.min(Math.min(target, cap), worldCap);
    }

    private boolean isValidFlightTarget(Vec3 target) {
        if (target == null) return false;

        BlockHitResult result = raevyx.level().clip(new ClipContext(
                raevyx.getEyePosition(),
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                raevyx
        ));

        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }

        double distanceToHit = result.getLocation().distanceTo(raevyx.position());
        double distanceToTarget = target.distanceTo(raevyx.position());

        return distanceToHit > distanceToTarget * 0.95;
    }

    private int flightDecisionInterval(boolean thundering, boolean raining) {
        if (thundering) return 2;
        if (raining) return 8;
        return 25;
    }

    private int nextDecisionCooldown(int baseInterval) {
        int jitter = Math.max(1, baseInterval / 2);
        return baseInterval + raevyx.getRandom().nextInt(jitter);
    }

    private boolean shouldTakeOff(boolean thundering, boolean raining) {
        if (isOverDanger()) return true;

        if (thundering) {
            // Raevyx LOVES thunder - very aggressive takeoff
            return raevyx.getRandom().nextInt(4) == 0; // 25%
        } else if (raining) {
            return raevyx.getRandom().nextInt(8) == 0; // 12.5%
        } else {
            return raevyx.getRandom().nextInt(80) == 0; // 1.25%
        }
    }

    private boolean shouldKeepFlying(boolean thundering, boolean raining) {
        if (isOverDanger()) return true;

        // Raevyx LOVES storms - flies LONGER in bad weather
        if (thundering) {
            return raevyx.getRandom().nextInt(3000) != 0; // ~2.5 min
        } else if (raining) {
            return raevyx.getRandom().nextInt(1800) != 0; // ~90 sec
        } else {
            return raevyx.getRandom().nextInt(200) != 0; // ~10 sec
        }
    }

    private boolean hasNearbyBabies() {
        return !raevyx.level().getEntitiesOfClass(
                Raevyx.class,
                raevyx.getBoundingBox().inflate(16.0D),
                baby -> baby != null && baby.isBaby() && baby.isAlive()
        ).isEmpty();
    }

    private boolean isOverDanger() {
        BlockPos dragonPos = raevyx.blockPosition();
        boolean foundSolid = false;
        boolean nearFluid = false;

        for (int i = 1; i <= 25; i++) {
            BlockPos checkPos = dragonPos.below(i);

            var state = raevyx.level().getBlockState(checkPos);
            if (!state.getCollisionShape(raevyx.level(), checkPos).isEmpty() ||
                    state.isFaceSturdy(raevyx.level(), checkPos, net.minecraft.core.Direction.UP)) {
                foundSolid = true;
                break;
            }

            if (i <= 10 && !raevyx.level().getFluidState(checkPos).isEmpty()) {
                nearFluid = true;
            }
        }

        if (nearFluid) return true;
        return !foundSolid && dragonPos.getY() < raevyx.level().getMinBuildHeight() + 20;
    }
}
