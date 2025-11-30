package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.ai.navigation.pathfinding.AsyncPathfindingHelper;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
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
 * Base flight goal using async A* pathfinding for all flying dragons.
 *
 * Consolidates shared flight logic:
 * - Async 3D pathfinding with adaptive grid resolution
 * - Waypoint navigation and stuck detection
 * - Landing cooldowns and weather-based decision making
 * - Danger avoidance (fluids, voids, etc.)
 *
 * Subclasses customize dragon-specific behavior via abstract methods.
 */
public abstract class DragonSmartFlightGoal<T extends DragonEntity & DragonFlightCapable> extends Goal {
    protected final T dragon;
    protected Vec3 finalTarget;
    protected List<Vec3> currentPath;
    protected int currentWaypointIndex;

    protected int stuckCounter = 0;
    protected int timeSinceTargetChange = 0;
    protected boolean pathfindingInProgress = false;

    protected long lastLandingTime = 0;
    protected int flightDecisionCooldown = 0;
    protected boolean wasThundering = false;
    protected boolean wasRaining = false;

    public DragonSmartFlightGoal(T dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.flightDecisionCooldown = 0;
    }

    // ===== ABSTRACT METHODS - Dragon-specific tuning =====

    /**
     * Waypoint acceptance radius (squared distance).
     * Fast dragons use larger values (25.0), slower dragons use smaller (16.0).
     */
    protected abstract double getWaypointAcceptanceRadiusSqr();

    /**
     * How many consecutive stuck ticks before re-pathing.
     * Aggressive dragons use lower values (2), patient dragons use higher (3).
     */
    protected abstract int getStuckThreshold();

    /**
     * Ticks to wait after landing before considering takeoff again.
     */
    protected abstract int getLandingCooldownTicks();

    /**
     * Should this dragon take off given current weather?
     * @return probability as chance (e.g., 4 = 1/4 = 25%)
     */
    protected abstract int getTakeoffChance(boolean thundering, boolean raining);

    /**
     * Should this dragon keep flying given current weather?
     * @return probability as chance (e.g., 3000 = 1/3000 per tick)
     */
    protected abstract int getKeepFlyingChance(boolean thundering, boolean raining);

    /**
     * Flight decision interval (ticks between flight/land decisions).
     * Storm-loving dragons use shorter intervals during storms.
     */
    protected abstract int getFlightDecisionInterval(boolean thundering, boolean raining);

    /**
     * Find safe flight height for this dragon at given position.
     * Weather-sensitive dragons adjust altitude based on conditions.
     */
    protected abstract double findSafeFlightHeight(double x, double z, boolean thundering, boolean raining);

    /**
     * Generate flight target range (min, max) based on dragon's flight style.
     * Returns [minRange, maxRange]
     */
    protected abstract float[] getFlightRange(boolean isStuck);

    /**
     * Should this dragon protect nearby babies by not flying away?
     */
    protected abstract boolean shouldProtectBabies();

    /**
     * Additional dragon-specific checks for canUse().
     * Return false to prevent flight.
     */
    protected boolean additionalCanUseChecks() {
        return true;
    }

    /**
     * Additional dragon-specific checks for canContinueToUse().
     * Return false to stop flight.
     */
    protected boolean additionalCanContinueChecks() {
        return true;
    }

    /**
     * Called when flight starts (after base initialization).
     */
    protected void onFlightStart() {
        // Override for custom behavior
    }

    /**
     * Called when flight stops (after base cleanup).
     */
    protected void onFlightStop() {
        // Override for custom behavior
    }

    // ===== GOAL LOGIC =====

    @Override
    public boolean canUse() {
        if (dragon.isLanding() || dragon.isVehicle() ||
            dragon.isPassenger() || dragon.isOrderedToSit()) {
            return false;
        }

        // Baby protection check
        if (shouldProtectBabies() && !dragon.isBaby() && hasNearbyBabies() && !isOverDanger()) {
            return false;
        }

        // Tamed dragons stay near owner (unless in danger)
        if (dragon.isTame()) {
            var owner = dragon.getOwner();
            if (owner != null && dragon.distanceToSqr(owner) < 15.0 * 15.0) {
                if (!isOverDanger()) {
                    return false;
                }
            }
        }

        // Dragon-specific checks
        if (!additionalCanUseChecks()) {
            return false;
        }

        boolean thundering = dragon.level().isThundering();
        boolean raining = !thundering && dragon.level().isRaining();

        boolean weatherChangedToStorm = (thundering && !wasThundering) || (raining && !wasRaining);
        boolean weatherChangedToThunder = thundering && !wasThundering;

        wasThundering = thundering;
        wasRaining = raining;

        // Landing cooldown check
        long currentTime = dragon.level().getGameTime();
        int cooldown = getLandingCooldownTicks();
        if (thundering) cooldown = 0;
        else if (raining) cooldown = cooldown / 4;
        if (weatherChangedToStorm) cooldown = 0;

        if (!dragon.isFlying() && (currentTime - lastLandingTime) < cooldown) {
            return false;
        }

        // Flight decision cooldown
        int decisionInterval = getFlightDecisionInterval(thundering, raining);
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

        // Decide whether to fly
        boolean isFlying;
        if (isOverDanger()) {
            isFlying = true;
        } else {
            if (dragon.isFlying()) {
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
        if (dragon.isLanding() || dragon.isOrderedToSit() || dragon.isVehicle()) {
            return false;
        }

        // Stop if dragon has a target (combat takes priority)
        if (dragon.getTarget() != null && dragon.getTarget().isAlive()) {
            return false;
        }

        // Tamed dragons return to owner
        if (dragon.isTame() && dragon.getOwner() != null && !isOverDanger()) {
            stopFlying();
            return false;
        }

        // Dragon-specific continue checks
        if (!additionalCanContinueChecks()) {
            stopFlying();
            return false;
        }

        // Weather-based landing for wild dragons
        if (!dragon.isTame()) {
            boolean thundering = dragon.level().isThundering();
            boolean raining = !thundering && dragon.level().isRaining();
            if (dragon.isFlying() && !shouldKeepFlying(thundering, raining)) {
                stopFlying();
                return false;
            }
        }

        // Stop if grounded for too long
        if (dragon.isFlying() && dragon.onGround() && timeSinceTargetChange > 5) {
            return false;
        }

        return dragon.isFlying() &&
               (hasWaypointsRemaining() || (finalTarget != null && dragon.distanceToSqr(finalTarget) > 9.0));
    }

    @Override
    public void start() {
        dragon.setFlying(true);
        dragon.setLanding(false);
        dragon.setHovering(false);
        dragon.setTakeoff(true);

        if (hasWaypointsRemaining()) {
            moveToNextWaypoint();
        }

        onFlightStart();
    }

    @Override
    public void tick() {
        timeSinceTargetChange++;

        if (dragon.isLanding()) {
            return;
        }

        // Follow path waypoints
        if (hasWaypointsRemaining()) {
            Vec3 currentWaypoint = currentPath.get(currentWaypointIndex);
            double distToWaypoint = dragon.distanceToSqr(currentWaypoint);

            if (distToWaypoint < getWaypointAcceptanceRadiusSqr()) {
                currentWaypointIndex++;
                if (hasWaypointsRemaining()) {
                    moveToNextWaypoint();
                }
            }

            // Stuck detection
            if (dragon.horizontalCollision) {
                stuckCounter++;
                if (stuckCounter > getStuckThreshold()) {
                    requestNewFlightPath();
                    stuckCounter = 0;
                }
            } else {
                stuckCounter = Math.max(0, stuckCounter - 1);
            }
        } else if (finalTarget != null) {
            double distToTarget = dragon.distanceToSqr(finalTarget);

            // Re-path when close or after timeout
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
        dragon.getNavigation().stop();

        if (!dragon.isFlying()) {
            lastLandingTime = dragon.level().getGameTime();
        }

        onFlightStop();
    }

    // ===== PATHFINDING INTEGRATION =====

    protected void requestNewFlightPath() {
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

            // Adaptive grid resolution based on distance
            double distance = dragon.position().distanceTo(targetPos);
            int gridResolution;
            if (distance < 30) {
                gridResolution = 2; // Fine for short distances
            } else if (distance < 80) {
                gridResolution = 4; // Medium for medium distances
            } else {
                gridResolution = 8; // Coarse for long distances
            }

            AsyncPathfindingHelper.requestPath(
                serverLevel,
                dragon.position(),
                targetPos,
                gridResolution,
                dragon.getBoundingBox(),
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
                                targetPos.x, targetPos.y, targetPos.z, 1.0
                            );
                        });
                    }
                }
            );
        } else {
            dragon.getMoveControl().setWantedPosition(
                targetPos.x, targetPos.y, targetPos.z, 1.0
            );
        }
    }

    protected void moveToNextWaypoint() {
        if (!hasWaypointsRemaining()) {
            return;
        }

        Vec3 waypoint = currentPath.get(currentWaypointIndex);
        dragon.getMoveControl().setWantedPosition(waypoint.x, waypoint.y, waypoint.z, 1.0);
    }

    protected boolean hasWaypointsRemaining() {
        return currentPath != null && currentWaypointIndex < currentPath.size();
    }

    // ===== FLIGHT TARGET GENERATION =====

    protected Vec3 findFlightTarget() {
        Vec3 dragonPos = dragon.position();

        for (int attempts = 0; attempts < 16; attempts++) {
            Vec3 candidate = generateFlightCandidate(dragonPos, attempts);

            if (isValidFlightTarget(candidate)) {
                return candidate;
            }
        }

        boolean thundering = dragon.level().isThundering();
        boolean raining = !thundering && dragon.level().isRaining();
        return new Vec3(dragonPos.x, findSafeFlightHeight(dragonPos.x, dragonPos.z, thundering, raining), dragonPos.z);
    }

    protected Vec3 generateFlightCandidate(Vec3 dragonPos, int attempt) {
        boolean isStuck = dragon.horizontalCollision || stuckCounter > 0;
        float[] range = getFlightRange(isStuck);
        float minRange = range[0];
        float maxRange = range[1];

        float maxRot = isStuck ? 360 : 180;
        float flightRange = minRange + dragon.getRandom().nextFloat() * (maxRange - minRange);

        float yRotOffset;
        if (isStuck && attempt < 8) {
            // Turn around when stuck
            yRotOffset = (float) Math.toRadians(180 + dragon.getRandom().nextFloat() * 120 - 60);
        } else {
            yRotOffset = (float) Math.toRadians(dragon.getRandom().nextFloat() * maxRot - (maxRot / 2));
        }

        float xRotOffset = (float) Math.toRadians((dragon.getRandom().nextFloat() - 0.5f) * 20);

        Vec3 lookVec = dragon.getLookAngle();
        Vec3 targetVec = lookVec.scale(flightRange).yRot(yRotOffset).xRot(xRotOffset);
        Vec3 candidate = dragonPos.add(targetVec);

        boolean thundering = dragon.level().isThundering();
        boolean raining = !thundering && dragon.level().isRaining();
        double targetY = findSafeFlightHeight(candidate.x, candidate.z, thundering, raining);
        candidate = new Vec3(candidate.x, targetY, candidate.z);

        if (!dragon.level().isLoaded(BlockPos.containing(candidate))) {
            return null;
        }

        return candidate;
    }

    protected boolean isValidFlightTarget(Vec3 target) {
        if (target == null) return false;

        // Reject targets over water
        BlockPos targetPos = BlockPos.containing(target);
        int groundY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetPos.getX(), targetPos.getZ());

        if (groundY <= 63 && target.y < 75) {
            BlockPos groundPos = new BlockPos(targetPos.getX(), groundY, targetPos.getZ());
            net.minecraft.world.level.block.state.BlockState groundState = dragon.level().getBlockState(groundPos);

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

    // ===== FLIGHT DECISION LOGIC =====

    protected int nextDecisionCooldown(int baseInterval) {
        int jitter = Math.max(1, baseInterval / 2);
        return baseInterval + dragon.getRandom().nextInt(jitter);
    }

    protected boolean shouldTakeOff(boolean thundering, boolean raining) {
        if (isOverDanger()) return true;

        int chance = getTakeoffChance(thundering, raining);
        return dragon.getRandom().nextInt(chance) == 0;
    }

    protected boolean shouldKeepFlying(boolean thundering, boolean raining) {
        if (isOverDanger()) return true;

        int chance = getKeepFlyingChance(thundering, raining);
        return dragon.getRandom().nextInt(chance) != 0;
    }

    protected boolean hasNearbyBabies() {
        return !dragon.level().getEntitiesOfClass(
                dragon.getClass(),
                dragon.getBoundingBox().inflate(16.0D),
                baby -> baby != null && baby.isBaby() && baby.isAlive()
        ).isEmpty();
    }

    protected boolean isOverDanger() {
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

    protected void stopFlying() {
        dragon.setLanding(true);
        dragon.setFlying(false);
        dragon.setTakeoff(false);
        dragon.setHovering(false);
    }
}
