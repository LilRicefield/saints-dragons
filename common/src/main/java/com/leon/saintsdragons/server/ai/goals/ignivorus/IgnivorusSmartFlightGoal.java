package com.leon.saintsdragons.server.ai.goals.ignivorus;

import com.leon.saintsdragons.server.ai.navigation.pathfinding.AsyncPathfindingHelper;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
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
 * Enhanced Ignivorus flight goal using async A* pathfinding.
 *
 * Ignivorus is a BOLD, aggressive fire dragon - this pathfinder is tuned for CONTINUOUS flight:
 * - Smoother transitions between waypoints (no stop-start)
 * - Aggressive patrol behavior
 * - Extended flight duration
 *
 * Based on IgnivorusFlightGoal but with:
 * - Pathfinding integration
 * - Continuous waypoint following (doesn't stop when reaching target)
 * - Better stuck handling
 */
public class IgnivorusSmartFlightGoal extends Goal {
    private final Ignivorus dragon;
    private Vec3 finalTarget;
    private List<Vec3> currentPath;
    private int currentWaypointIndex;

    private int stuckCounter = 0;
    private int timeSinceTargetChange = 0;
    private boolean pathfindingInProgress = false;

    private static final int LANDING_COOLDOWN_TICKS = 60; // 3 seconds
    private long lastLandingTime = 0;

    private int flightDecisionCooldown = 0;

    public IgnivorusSmartFlightGoal(Ignivorus dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.flightDecisionCooldown = 0;
    }

    @Override
    public boolean canUse() {
        if (dragon.isLanding()) {
            return false;
        }

        if (dragon.isVehicle() || dragon.isPassenger() || dragon.isOrderedToSit()) {
            return false;
        }

        // Tamed dragons: only fly when owner flying or over danger
        if (dragon.isTame() && dragon.getOwner() != null) {
            LivingEntity owner = dragon.getOwner();
            boolean ownerFlying = !owner.onGround() && owner.isAlive();

            if (!isOverDanger() && !ownerFlying) {
                return false;
            }
        }

        long currentTime = dragon.level().getGameTime();
        int cooldown = LANDING_COOLDOWN_TICKS;

        if (!dragon.isFlying() && (currentTime - lastLandingTime) < cooldown) {
            return false;
        }

        int decisionInterval = flightDecisionInterval();
        if (flightDecisionCooldown > 0) {
            flightDecisionCooldown--;
            if (flightDecisionCooldown > 0) {
                return false;
            }
        }

        boolean shouldFly;
        if (isOverDanger()) {
            shouldFly = true;
        } else {
            if (dragon.isFlying()) {
                shouldFly = shouldKeepFlying();
            } else {
                shouldFly = shouldTakeOff();
            }
        }

        if (shouldFly) {
            requestNewFlightPath();
            this.flightDecisionCooldown = nextDecisionCooldown(decisionInterval);
            return true;
        }

        this.flightDecisionCooldown = nextDecisionCooldown(decisionInterval);
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (dragon.isLanding()) {
            return false;
        }

        if (dragon.isOrderedToSit() || dragon.isVehicle()) {
            return false;
        }

        // Tamed: land if owner not flying and not over danger
        if (dragon.isTame() && dragon.getOwner() != null) {
            LivingEntity owner = dragon.getOwner();
            boolean ownerFlying = owner.isAlive() && !owner.onGround();

            if (!isOverDanger() && !ownerFlying) {
                dragon.setGoingUp(false);
                dragon.setGoingDown(false);
                dragon.setLanding(true);
                dragon.setFlying(false);
                dragon.setHovering(false);
                dragon.setTakeoff(false);
                return false;
            }
        }

        // Stop if combat starts
        var target = dragon.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }

        // Natural landing decision
        if (dragon.isFlying() && !shouldKeepFlying() && !isOverDanger()) {
            dragon.setLanding(true);
            dragon.setFlying(false);
            dragon.setTakeoff(false);
            dragon.setHovering(false);
            return false;
        }

        // Handle stuck on ground while trying to fly
        if (dragon.isFlying() && dragon.onGround()) {
            if (timeSinceTargetChange > 5) {
                dragon.setLanding(true);
                dragon.setFlying(false);
                dragon.setTakeoff(false);
                dragon.setHovering(false);
                dragon.markLandedNow();
                return false;
            }
        }

        // CONTINUOUS FLIGHT FIX: Keep flying even when close to finalTarget
        // We'll just pick a new path when waypoints run out
        return dragon.isFlying() &&
               (hasWaypointsRemaining() || finalTarget != null);
    }

    @Override
    public void start() {
        dragon.setFlying(true);
        dragon.setLanding(false);
        dragon.setHovering(false);
        dragon.setTakeoff(false);

        if (hasWaypointsRemaining()) {
            moveToNextWaypoint();
        }
    }

    @Override
    public void tick() {
        timeSinceTargetChange++;

        if (dragon.isLanding()) {
            return;
        }

        // Handle stuck on ground
        if (dragon.isFlying() && dragon.onGround()) {
            if (timeSinceTargetChange > 5) {
                dragon.setLanding(true);
                dragon.setFlying(false);
                dragon.setTakeoff(false);
                dragon.setHovering(false);
                dragon.markLandedNow();
                return;
            }
        }

        // Tamed: check owner still flying
        if (dragon.isTame() && dragon.getOwner() != null) {
            LivingEntity owner = dragon.getOwner();
            boolean ownerFlying = owner.isAlive() && !owner.onGround();

            if (!isOverDanger() && !ownerFlying) {
                dragon.setLanding(true);
                dragon.setFlying(false);
                dragon.setHovering(false);
                dragon.setTakeoff(false);
                return;
            }
        }

        // Follow path waypoints
        if (hasWaypointsRemaining()) {
            Vec3 currentWaypoint = currentPath.get(currentWaypointIndex);
            double distToWaypoint = dragon.distanceToSqr(currentWaypoint);

            // Waypoint acceptance radius (4 blocks)
            if (distToWaypoint < 16.0) {
                currentWaypointIndex++;
                if (hasWaypointsRemaining()) {
                    moveToNextWaypoint();
                } else {
                    // Reached end of path - get new path for CONTINUOUS flight
                    requestNewFlightPath();
                }
            }

            // Stuck detection
            if (dragon.horizontalCollision) {
                stuckCounter++;
                if (stuckCounter > 3) {
                    requestNewFlightPath();
                    stuckCounter = 0;
                }
            } else {
                stuckCounter = Math.max(0, stuckCounter - 1);
            }
        } else if (finalTarget != null) {
            double distToTarget = dragon.distanceToSqr(finalTarget);

            // Close to target OR taking too long - get new path
            if (distToTarget < 64.0 || timeSinceTargetChange > 400) {
                requestNewFlightPath();
            }
        } else {
            // No path at all - request one
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
        dragon.getNavigation().stop();

        if (!dragon.isFlying()) {
            lastLandingTime = dragon.level().getGameTime();
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

        if (dragon.level() instanceof ServerLevel serverLevel) {
            pathfindingInProgress = true;

            // Adaptive grid resolution based on distance to prevent timeout on long paths
            double distance = dragon.position().distanceTo(targetPos);
            int gridResolution;
            if (distance < 30) {
                gridResolution = 4; // Fine-grained for short distances
            } else if (distance < 80) {
                gridResolution = 10; // Medium for medium distances
            } else {
                gridResolution = 16; // Coarse for long distances
            }

            AsyncPathfindingHelper.requestPath(
                serverLevel,
                dragon.position(),
                targetPos,
                gridResolution,
                dragon.getBoundingBox(), // Use actual dragon size
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
                            dragon.getMoveControl().setWantedPosition(
                                targetPos.x, targetPos.y, targetPos.z, dragon.getFlightSpeed()
                            );
                        });
                    }
                }
            );
        } else {
            dragon.getMoveControl().setWantedPosition(
                targetPos.x, targetPos.y, targetPos.z, dragon.getFlightSpeed()
            );
        }
    }

    private void moveToNextWaypoint() {
        if (!hasWaypointsRemaining()) {
            return;
        }

        Vec3 waypoint = currentPath.get(currentWaypointIndex);
        dragon.getMoveControl().setWantedPosition(waypoint.x, waypoint.y, waypoint.z, dragon.getFlightSpeed());
    }

    private boolean hasWaypointsRemaining() {
        return currentPath != null && currentWaypointIndex < currentPath.size();
    }

    // ===== IGNIVORUS FLIGHT LOGIC =====

    private Vec3 findFlightTarget() {
        Vec3 dragonPos = dragon.position();
        Vec3 anchor = getFlightAnchor();

        for (int attempts = 0; attempts < 16; attempts++) {
            Vec3 candidate = generateFlightCandidate(anchor, dragonPos, attempts);

            if (isValidFlightTarget(candidate)) {
                return candidate;
            }
        }

        return new Vec3(anchor.x, findSafeFlightHeight(anchor.x, anchor.z, false), anchor.z);
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
        int groundY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);

        double base;
        if (tethered) {
            // Tamed: moderate altitude around owner
            base = 15.0 + dragon.getRandom().nextDouble() * 15.0;
        } else {
            // Wild: aggressive patrol at medium-high altitude
            base = 20.0 + dragon.getRandom().nextDouble() * 25.0;
        }

        double capAboveGround = tethered ? 40.0 : 60.0;

        double target = groundY + base;
        double cap = groundY + capAboveGround;
        double worldCap = dragon.level().getMaxBuildHeight() - 10.0;

        return Math.min(Math.min(target, cap), worldCap);
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

        // Reject targets over water - check ground below target
        BlockPos targetPos = BlockPos.containing(target);
        int groundY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetPos.getX(), targetPos.getZ());

        // If ground is at or below water level (Y=62) and target is low, reject it
        // This prevents landing in water or on small islands at water level
        if (groundY <= 63 && target.y < 75) {
            BlockPos groundPos = new BlockPos(targetPos.getX(), groundY, targetPos.getZ());
            net.minecraft.world.level.block.state.BlockState groundState = dragon.level().getBlockState(groundPos);

            // Reject if ground is water or the target is too close to sea level
            if (groundState.getFluidState().is(net.minecraft.tags.FluidTags.WATER) || groundY < 63) {
                return false;
            }
        }

        // Line-of-sight check
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
        return 10; // Check every ~0.5 seconds
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
        return dragon.getRandom().nextInt(30) == 0; // ~3.3%
    }

    private boolean shouldKeepFlying() {
        if (isOverDanger()) {
            return true;
        }

        // Fire dragons patrol for extended periods (~2-3 minutes)
        return dragon.getRandom().nextInt(3000) != 0;
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

            if (i <= 10 && !dragon.level().getFluidState(checkPos).isEmpty()) {
                nearFluid = true;
            }
        }

        if (nearFluid) return true;
        return !foundSolid && dragonPos.getY() < dragon.level().getMinBuildHeight() + 20;
    }
}
