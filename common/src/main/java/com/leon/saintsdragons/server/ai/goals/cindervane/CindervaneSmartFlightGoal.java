package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.server.ai.navigation.pathfinding.AsyncPathfindingHelper;
import com.leon.saintsdragons.server.ai.navigation.pathfinding.PathfindingResult;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
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
 * Enhanced Cindervane flight goal using async A* pathfinding.
 *
 * Improvements over basic flight:
 * - Uses 3D pathfinding for smarter obstacle avoidance
 * - Smooth paths via line-of-sight shortcuts and curve interpolation
 * - Async computation (no lag from pathfinding)
 * - Falls back to direct flight if pathfinding fails
 *
 * Based on CindervaneFlightGoal but with pathfinding integration.
 */
public class CindervaneSmartFlightGoal extends Goal {
    private final Cindervane cindervane;
    private Vec3 finalTarget; // Ultimate destination
    private List<Vec3> currentPath; // Path waypoints to follow
    private int currentWaypointIndex;

    private int stuckCounter = 0;
    private int timeSinceTargetChange = 0;
    private boolean pathfindingInProgress = false;

    // Landing cooldown
    private static final int LANDING_COOLDOWN_TICKS = 40;
    private long lastLandingTime = 0;

    private int flightDecisionCooldown = 0;
    private boolean wasThundering = false;
    private boolean wasRaining = false;

    public CindervaneSmartFlightGoal(Cindervane cindervane) {
        this.cindervane = cindervane;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.flightDecisionCooldown = 0;
    }

    @Override
    public boolean canUse() {
        // Same logic as original CindervaneFlightGoal
        if (cindervane.isLanding() || cindervane.isVehicle() ||
            cindervane.isPassenger() || cindervane.isOrderedToSit()) {
            return false;
        }

        if (cindervane.isTame() && cindervane.getOwner() != null && !isOverDanger()) {
            return false;
        }

        boolean thundering = cindervane.level().isThundering();
        boolean raining = !thundering && cindervane.level().isRaining();

        boolean weatherChangedToStorm = (thundering && !wasThundering) || (raining && !wasRaining);
        boolean weatherChangedToThunder = thundering && !wasThundering;

        wasThundering = thundering;
        wasRaining = raining;

        long currentTime = cindervane.level().getGameTime();
        int cooldown = LANDING_COOLDOWN_TICKS;
        if (thundering) cooldown = 0;
        else if (raining) cooldown = cooldown / 4;

        if (weatherChangedToStorm) cooldown = 0;

        if (!cindervane.isFlying() && (currentTime - lastLandingTime) < cooldown) {
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
            if (cindervane.isFlying()) {
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
        // Same as original
        if (cindervane.isLanding() || cindervane.isOrderedToSit() || cindervane.isVehicle()) {
            return false;
        }

        if (cindervane.isTame() && cindervane.getOwner() != null && !isOverDanger()) {
            cindervane.setGoingUp(false);
            cindervane.setGoingDown(false);
            cindervane.setLanding(true);
            cindervane.setFlying(false);
            cindervane.setHovering(false);
            cindervane.setTakeoff(false);
            return false;
        }

        var target = cindervane.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }

        if (!cindervane.isTame()) {
            boolean thundering = cindervane.level().isThundering();
            boolean raining = !thundering && cindervane.level().isRaining();
            if (cindervane.isFlying() && !shouldKeepFlying(thundering, raining)) {
                cindervane.setLanding(true);
                cindervane.setFlying(false);
                cindervane.setTakeoff(false);
                cindervane.setHovering(false);
                return false;
            }
        }

        if (cindervane.isFlying() && cindervane.onGround()) {
            if (timeSinceTargetChange > 5) {
                return false;
            }
        }

        // Continue if we have waypoints to follow or a final target
        return cindervane.isFlying() &&
               (hasWaypointsRemaining() || (finalTarget != null && cindervane.distanceToSqr(finalTarget) > 9.0));
    }

    @Override
    public void start() {
        cindervane.setFlying(true);
        cindervane.setLanding(false);
        cindervane.setHovering(false);

        // If we already have a path, start following it
        if (hasWaypointsRemaining()) {
            moveToNextWaypoint();
        }
    }

    @Override
    public void tick() {
        timeSinceTargetChange++;

        if (cindervane.isLanding()) {
            return;
        }

        if (cindervane.isFlying() && cindervane.onGround() && timeSinceTargetChange > 5) {
            cindervane.setLanding(true);
            cindervane.setFlying(false);
            cindervane.setTakeoff(false);
            cindervane.setHovering(false);
            cindervane.markLandedNow();
            return;
        }

        if (cindervane.isTame() && cindervane.getOwner() != null && !isOverDanger()) {
            cindervane.setLanding(true);
            cindervane.setFlying(false);
            cindervane.setHovering(false);
            cindervane.setTakeoff(false);
            return;
        }

        // Follow path waypoints
        if (hasWaypointsRemaining()) {
            Vec3 currentWaypoint = currentPath.get(currentWaypointIndex);
            double distToWaypoint = cindervane.distanceToSqr(currentWaypoint);

            // Reached current waypoint
            if (distToWaypoint < 16.0) { // 4 blocks
                currentWaypointIndex++;
                if (hasWaypointsRemaining()) {
                    moveToNextWaypoint();
                }
            }

            // Stuck detection
            if (cindervane.horizontalCollision) {
                stuckCounter++;
                if (stuckCounter > 3) {
                    requestNewFlightPath(); // Re-path around obstacle
                    stuckCounter = 0;
                }
            } else {
                stuckCounter = Math.max(0, stuckCounter - 1);
            }
        } else if (finalTarget != null) {
            // No path but have final target - fly direct (fallback)
            double distToTarget = cindervane.distanceToSqr(finalTarget);

            if (distToTarget < 100.0 || timeSinceTargetChange > 300) {
                requestNewFlightPath();
            }
        } else {
            // No path and no target - get new one
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
        cindervane.getNavigation().stop();

        if (!cindervane.isFlying()) {
            lastLandingTime = cindervane.level().getGameTime();
        }
    }

    // ===== PATHFINDING INTEGRATION =====

    /**
     * Request a new flight path using async A* pathfinding.
     */
    private void requestNewFlightPath() {
        if (pathfindingInProgress) {
            return; // Already computing a path
        }

        // Pick a random target (same logic as original)
        Vec3 targetPos = findFlightTarget();
        if (targetPos == null) {
            return;
        }

        this.finalTarget = targetPos;
        timeSinceTargetChange = 0;

        // Try async pathfinding if on server
        if (cindervane.level() instanceof ServerLevel serverLevel) {
            pathfindingInProgress = true;

            // Adaptive grid resolution based on distance to prevent timeout on long paths
            double distance = cindervane.position().distanceTo(targetPos);
            int gridResolution;
            if (distance < 30) {
                gridResolution = 2; // Fine-grained for short distances
            } else if (distance < 80) {
                gridResolution = 4; // Medium for medium distances
            } else {
                gridResolution = 8; // Coarse for long distances
            }

            AsyncPathfindingHelper.requestPath(
                serverLevel,
                cindervane.position(),
                targetPos,
                gridResolution,
                cindervane.getBoundingBox(), // Use actual dragon size
                result -> {
                    // Callback runs on background thread!
                    pathfindingInProgress = false;

                    if (result.isSuccess()) {
                        List<Vec3> path = result.getPath();

                        // Schedule path update on main thread
                        AsyncPathfindingHelper.scheduleOnMainThread(serverLevel, () -> {
                            // Path found - use it!
                            currentPath = new ArrayList<>(path);
                            currentWaypointIndex = 0;
                            moveToNextWaypoint();
                        });
                    } else {
                        // Pathfinding failed - fly direct as fallback
                        AsyncPathfindingHelper.scheduleOnMainThread(serverLevel, () -> {
                            currentPath = null;
                            cindervane.getMoveControl().setWantedPosition(
                                targetPos.x, targetPos.y, targetPos.z,
                                cindervane.getFlightSpeed()
                            );
                        });
                    }
                }
            );
        } else {
            // Client side or fallback - fly direct
            cindervane.getMoveControl().setWantedPosition(
                targetPos.x, targetPos.y, targetPos.z,
                cindervane.getFlightSpeed()
            );
        }
    }

    private void moveToNextWaypoint() {
        if (!hasWaypointsRemaining()) {
            return;
        }

        Vec3 waypoint = currentPath.get(currentWaypointIndex);
        cindervane.getMoveControl().setWantedPosition(
            waypoint.x, waypoint.y, waypoint.z,
            cindervane.getFlightSpeed()
        );
    }

    private boolean hasWaypointsRemaining() {
        return currentPath != null && currentWaypointIndex < currentPath.size();
    }

    // ===== ORIGINAL CINDERVANE FLIGHT LOGIC (REUSED) =====

    private Vec3 findFlightTarget() {
        Vec3 dragonPos = cindervane.position();
        Vec3 anchor = getFlightAnchor();

        for (int attempts = 0; attempts < 16; attempts++) {
            Vec3 candidate = generateFlightCandidate(anchor, dragonPos, attempts);

            if (isValidFlightTarget(candidate)) {
                return candidate;
            }
        }

        return new Vec3(anchor.x, findSafeFlightHeight(anchor.x, anchor.z, true), anchor.z);
    }

    private Vec3 generateFlightCandidate(Vec3 anchor, Vec3 dragonPos, int attempt) {
        boolean isStuck = cindervane.horizontalCollision || stuckCounter > 0;
        boolean tethered = isTamedWander();

        Vec3 candidate;

        if (tethered) {
            double min = 10.0 + cindervane.getRandom().nextDouble() * 6.0;
            double max = 24.0 + cindervane.getRandom().nextDouble() * 6.0;
            double angle = cindervane.getRandom().nextDouble() * Math.PI * 2.0;
            double radius = min + cindervane.getRandom().nextDouble() * (max - min);
            double cx = anchor.x + Math.cos(angle) * radius;
            double cz = anchor.z + Math.sin(angle) * radius;
            double targetY = findSafeFlightHeight(cx, cz, true);
            candidate = new Vec3(cx, targetY, cz);
        } else {
            float maxRot = isStuck ? 360 : 180;
            float range = isStuck ? 40.0f + cindervane.getRandom().nextFloat() * 60.0f :
                    80.0f + cindervane.getRandom().nextFloat() * 120.0f;

            float yRotOffset;
            if (isStuck && attempt < 8) {
                yRotOffset = (float) Math.toRadians(180 + cindervane.getRandom().nextFloat() * 120 - 60);
            } else {
                yRotOffset = (float) Math.toRadians(cindervane.getRandom().nextFloat() * maxRot - (maxRot / 2));
            }

            float xRotOffset = (float) Math.toRadians((cindervane.getRandom().nextFloat() - 0.5f) * 20);

            Vec3 lookVec = cindervane.getLookAngle();
            Vec3 targetVec = lookVec.scale(range).yRot(yRotOffset).xRot(xRotOffset);
            Vec3 raw = dragonPos.add(targetVec);
            double targetY = findSafeFlightHeight(raw.x, raw.z, false);
            candidate = new Vec3(raw.x, targetY, raw.z);
        }

        if (!cindervane.level().isLoaded(BlockPos.containing(candidate))) {
            return null;
        }

        return candidate;
    }

    private double findSafeFlightHeight(double x, double z, boolean tethered) {
        int ix = (int) x;
        int iz = (int) z;
        int groundY = cindervane.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);

        double base = tethered ? 12.0 + cindervane.getRandom().nextDouble() * 12.0 :
                                 25.0 + cindervane.getRandom().nextDouble() * 35.0;

        boolean thundering = cindervane.level().isThundering();
        boolean raining = !thundering && cindervane.level().isRaining();
        double capAboveGround;
        if (tethered) {
            capAboveGround = thundering ? 12.0 : (raining ? 18.0 : 32.0);
        } else {
            capAboveGround = thundering ? 20.0 : (raining ? 30.0 : 80.0);
        }

        double target = groundY + base;
        double cap = groundY + capAboveGround;
        double worldCap = cindervane.level().getMaxBuildHeight() - 10.0;

        return Math.min(Math.min(target, cap), worldCap);
    }

    private Vec3 getFlightAnchor() {
        if (isTamedWander()) {
            LivingEntity owner = cindervane.getOwner();
            if (owner != null) {
                return owner.position();
            }
        }
        return cindervane.position();
    }

    private boolean isTamedWander() {
        return cindervane.isTame() && cindervane.getCommand() == 2 && cindervane.getOwner() != null;
    }

    private boolean isValidFlightTarget(Vec3 target) {
        if (target == null) return false;

        // Reject targets over water - check ground below target
        BlockPos targetPos = BlockPos.containing(target);
        int groundY = cindervane.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetPos.getX(), targetPos.getZ());

        // If ground is at or below water level (Y=62) and target is low, reject it
        // This prevents landing in water or on small islands at water level
        if (groundY <= 63 && target.y < 75) {
            BlockPos groundPos = new BlockPos(targetPos.getX(), groundY, targetPos.getZ());
            net.minecraft.world.level.block.state.BlockState groundState = cindervane.level().getBlockState(groundPos);

            // Reject if ground is water or the target is too close to sea level
            if (groundState.getFluidState().is(net.minecraft.tags.FluidTags.WATER) || groundY < 63) {
                return false;
            }
        }

        // Line-of-sight check
        BlockHitResult result = cindervane.level().clip(new ClipContext(
                cindervane.getEyePosition(),
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                cindervane
        ));

        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }

        double distanceToHit = result.getLocation().distanceTo(cindervane.position());
        double distanceToTarget = target.distanceTo(cindervane.position());

        return distanceToHit > distanceToTarget * 0.95;
    }

    private int flightDecisionInterval(boolean thundering, boolean raining) {
        if (thundering) return 2;
        if (raining) return 5;
        return 8;
    }

    private int nextDecisionCooldown(int baseInterval) {
        int jitter = Math.max(1, baseInterval / 2);
        return baseInterval + cindervane.getRandom().nextInt(jitter);
    }

    private boolean shouldTakeOff(boolean thundering, boolean raining) {
        if (isOverDanger()) return true;

        if (!cindervane.isTame()) {
            long dayTime = cindervane.level().getDayTime() % 24000;
            boolean isNight = dayTime >= 13000 && dayTime < 23000;
            if (isNight) return false;
        }

        if (thundering) return cindervane.getRandom().nextInt(200) == 0;
        else if (raining) return cindervane.getRandom().nextInt(100) == 0;
        else return cindervane.getRandom().nextInt(40) == 0;
    }

    private boolean shouldKeepFlying(boolean thundering, boolean raining) {
        if (isOverDanger()) return true;

        if (!cindervane.isTame()) {
            long dayTime = cindervane.level().getDayTime() % 24000;
            boolean isNight = dayTime >= 13000 && dayTime < 23000;
            if (isNight) return cindervane.getRandom().nextInt(100) != 0;
        }

        if (thundering) return cindervane.getRandom().nextInt(200) != 0;
        else if (raining) return cindervane.getRandom().nextInt(400) != 0;
        else return cindervane.getRandom().nextInt(3600) != 0;
    }

    private boolean isOverDanger() {
        BlockPos dragonPos = cindervane.blockPosition();
        boolean foundSolid = false;
        boolean nearFluid = false;

        for (int i = 1; i <= 25; i++) {
            BlockPos checkPos = dragonPos.below(i);

            var state = cindervane.level().getBlockState(checkPos);
            if (!state.getCollisionShape(cindervane.level(), checkPos).isEmpty() ||
                    state.isFaceSturdy(cindervane.level(), checkPos, net.minecraft.core.Direction.UP)) {
                foundSolid = true;
                break;
            }

            if (i <= 10 && !cindervane.level().getFluidState(checkPos).isEmpty()) {
                nearFluid = true;
            }
        }

        if (nearFluid) return true;
        return !foundSolid && dragonPos.getY() < cindervane.level().getMinBuildHeight() + 20;
    }
}
