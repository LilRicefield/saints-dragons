package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.ai.navigation.pathfinding.AsyncPathfindingHelper;
import com.leon.saintsdragons.server.ai.navigation.pathfinding.DragonPathfinder;
import com.leon.saintsdragons.server.ai.navigation.pathfinding.PathfindingResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal for flying dragons stuck in water to path to the nearest shore.
 * Uses async pathfinding with WATER_ESCAPE mode to avoid server lag.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Activates when dragon is in water and not flying
 *   <li>Searches for nearest shore asynchronously
 *   <li>Requests path to shore using WATER_ESCAPE pathfinding mode
 *   <li>Follows path waypoints until reaching land
 *   <li>Stops when dragon reaches shore or exits water
 * </ul>
 *
 * <p>Usage: Add to dragon's goal selector with high priority (above wandering).
 * <pre>
 * this.goalSelector.addGoal(2, new DragonWaterEscapeGoal(this, 32, 150));
 * </pre>
 */
public class DragonWaterEscapeGoal extends Goal {

    private final Mob mob;
    private final int maxShoreSearchRadius;
    private final long pathfindingTimeoutMs;

    @Nullable
    private Vec3 targetShorePos;
    @Nullable
    private List<Vec3> currentPath;
    private int currentWaypointIndex;

    private boolean isSearchingForShore;
    private boolean isPathfinding;
    private int recheckCooldown;
    private static final int RECHECK_TICKS = 40; // 2 seconds

    public DragonWaterEscapeGoal(Mob mob, int maxShoreSearchRadius, long pathfindingTimeoutMs) {
        this.mob = mob;
        this.maxShoreSearchRadius = maxShoreSearchRadius;
        this.pathfindingTimeoutMs = pathfindingTimeoutMs;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Convenience constructor with default parameters.
     */
    public DragonWaterEscapeGoal(Mob mob) {
        this(mob, 48, 2000); // 48 block search radius, 2000ms timeout (2 seconds for testing)
    }

    @Override
    public boolean canUse() {
        // Decrement cooldown
        if (recheckCooldown > 0) {
            recheckCooldown--;
            return false;
        }

        // Only activate if:
        // 1. Dragon is in water
        // 2. Not already flying (we want ground-based escape)
        // 3. Not currently riding anything
        // 4. Not being ridden
        if (!mob.isInWater() || mob.isPassenger() || mob.isVehicle()) {
            return false;
        }

        // Check if dragon has "flying" state - if it does and it's flying, don't activate
        // This allows the dragon to naturally fly out if it can
        if (mob instanceof net.minecraft.world.entity.animal.FlyingAnimal) {
            // If the dragon is more than 3 blocks above water, it's probably flying out already
            if (mob.getY() > mob.level().getSeaLevel() + 3) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Only stop if we're out of water AND on solid ground (successfully reached shore)
        if (!mob.isInWater() && mob.onGround()) {
            return false;
        }

        // Stop if we have a path and reached the end
        if (currentPath != null && currentWaypointIndex >= currentPath.size()) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        // Reset state
        this.targetShorePos = null;
        this.currentPath = null;
        this.currentWaypointIndex = 0;
        this.isSearchingForShore = true;
        this.isPathfinding = false;

        // Start searching for nearest shore asynchronously
        searchForShore();
    }

    @Override
    public void tick() {
        // If we're searching or pathfinding, just wait
        if (isSearchingForShore || isPathfinding) {
            return;
        }

        // If we have no path, we failed - stop
        if (currentPath == null || currentPath.isEmpty()) {
            this.stop();
            return;
        }

        // Follow the current path
        followPath();
    }

    @Override
    public void stop() {
        // Set cooldown before trying again
        this.recheckCooldown = RECHECK_TICKS;

        // Clean up state
        this.targetShorePos = null;
        this.currentPath = null;
        this.currentWaypointIndex = 0;
        this.isSearchingForShore = false;
        this.isPathfinding = false;

        // Let mob's AI take over
        mob.getNavigation().stop();
    }

    /**
     * Search for the nearest shore position asynchronously.
     */
    private void searchForShore() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            this.stop();
            return;
        }

        // Run shore search on async thread to avoid blocking
        AsyncPathfindingHelper.requestPath(
            serverLevel,
            mob.position(),
            mob.position(), // Dummy goal for now
            2,
            result -> {
                // This callback runs on background thread!
                // We'll do the shore search here manually
                Vec3 shorePos = DragonPathfinder.findNearestShore(
                    mob.level(),
                    mob.position(),
                    maxShoreSearchRadius
                );

                // Schedule result handling on main thread
                AsyncPathfindingHelper.scheduleOnMainThread(serverLevel, () -> {
                    handleShoreSearchResult(shorePos);
                });
            }
        );
    }

    /**
     * Handle shore search result (runs on main thread).
     */
    private void handleShoreSearchResult(@Nullable Vec3 shorePos) {
        this.isSearchingForShore = false;

        if (shorePos == null) {
            // No shore found within radius - give up
            this.stop();
            return;
        }

        this.targetShorePos = shorePos;

        // Now request pathfinding to that shore
        requestPathToShore();
    }

    /**
     * Request async pathfinding to the target shore position.
     */
    private void requestPathToShore() {
        if (targetShorePos == null || !(mob.level() instanceof ServerLevel serverLevel)) {
            this.stop();
            return;
        }

        this.isPathfinding = true;

        // Request path using WATER_ESCAPE mode
        com.leon.saintsdragons.server.ai.navigation.pathfinding.AsyncPathfindingManager.getInstance()
            .requestPath(
                serverLevel,
                mob.position(),
                targetShorePos,
                2, // Coarse grid for performance
                pathfindingTimeoutMs,
                true, // Enable path smoothing
                mob.getBoundingBox(), // Use mob's bounding box
                DragonPathfinder.PathMode.WATER_ESCAPE // WATER_ESCAPE mode!
            )
            .thenAccept(result -> {
                // Schedule handling on main thread
                AsyncPathfindingHelper.scheduleOnMainThread(serverLevel, () -> {
                    handlePathfindingResult(result);
                });
            });
    }

    /**
     * Handle pathfinding result (runs on main thread).
     */
    private void handlePathfindingResult(PathfindingResult result) {
        this.isPathfinding = false;

        if (!result.isSuccess() || result.getPath() == null || result.getPath().isEmpty()) {
            // Pathfinding failed - give up
            this.stop();
            return;
        }

        // Got a path! Start following it
        this.currentPath = result.getPath();
        this.currentWaypointIndex = 0;
    }

    /**
     * Follow the current path to shore.
     */
    private void followPath() {
        if (currentPath == null || currentWaypointIndex >= currentPath.size()) {
            return;
        }

        Vec3 targetWaypoint = currentPath.get(currentWaypointIndex);

        // Check if we're close enough to the current waypoint
        double distanceSqr = mob.position().distanceToSqr(targetWaypoint);
        double reachThreshold = Math.max(2.0, mob.getBbWidth() * 1.5);

        if (distanceSqr < reachThreshold * reachThreshold) {
            // Reached waypoint, move to next
            currentWaypointIndex++;

            if (currentWaypointIndex >= currentPath.size()) {
                // Reached the end of the path - clamp to last waypoint and keep moving until on land
                currentWaypointIndex = currentPath.size() - 1;
            }

            targetWaypoint = currentPath.get(currentWaypointIndex);
        }

        // Move towards the waypoint
        // Use direct movement since we're in water (not normal navigation)
        moveTowards(targetWaypoint);
    }

    /**
     * Direct movement towards a target position in water.
     * Based on TDE's amphibious movement system with constant upward buoyancy.
     */
    private void moveTowards(Vec3 target) {
        Vec3 currentPos = mob.position();
        Vec3 direction = target.subtract(currentPos).normalize();

        // Set movement speed
        double speed = 0.3; // Swimming speed

        // Add constant upward buoyancy (critical for floating!)
        // Based on TDE's AmphibiousMobMoveController
        Vec3 currentDelta = mob.getDeltaMovement();
        mob.setDeltaMovement(currentDelta.add(0.0, 0.005, 0.0));

        // If target is above us (climbing onto shore), add extra upward boost
        double verticalDistance = target.y - currentPos.y;
        if (verticalDistance > 0.5 && currentPos.distanceTo(target) < 3.0) {
            // Close to shore and need to climb up - add jump boost
            mob.setDeltaMovement(mob.getDeltaMovement().add(0.0, 0.1, 0.0));
        }

        // Apply horizontal movement (use move control for smoother movement)
        mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.0);

        // Look towards target
        double dx = target.x - currentPos.x;
        double dz = target.z - currentPos.z;
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
    }
}
