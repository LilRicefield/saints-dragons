package com.leon.saintsdragons.server.ai.navigation;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Queue;

public class DragonAIMovementController {
    private static final double MIN_AIRBORNE_LANDING_HORIZONTAL = 6.0D;
    public static final double GROUND_WANDER_SPEED = 0.5D;
    public static final double GROUND_CHASE_SPEED = 0.60D;
    public static final double GROUND_SPRINT_SPEED = 0.80D;

    private final RideableDragonBase dragon;
    private final Queue<QueuedWaypoint> waypointQueue = new ArrayDeque<>();
    private @Nullable QueuedWaypoint currentWaypoint;

    public DragonAIMovementController(RideableDragonBase dragon) {
        this.dragon = dragon;
    }

    public void serverTick() {
        if (dragon.level().isClientSide) {
            return;
        }
        if (dragon.isVehicle() || dragon.isPassenger()) {
            clearAllWaypoints();
            return;
        }
        if (currentWaypoint != null && hasArrived()) {
            advanceToNextWaypoint();
        }
        if (!shouldUseAirMovement() && !canUseGroundNavigation()) {
            dragon.getNavigation().stop();
        }
    }

    public boolean setWaypoint(LivingEntity target, double speed) {
        return target != null && setWaypoint(target.position(), speed);
    }

    public boolean setWaypoint(Vec3 target, double speed) {
        if (target == null || dragon.level().isClientSide) {
            return false;
        }
        waypointQueue.clear();
        return startWaypoint(new QueuedWaypoint(target, speed, false, MovementMode.AUTO));
    }

    public boolean addWaypoint(LivingEntity target, double speed) {
        return target != null && addWaypoint(target.position(), speed);
    }

    public boolean addWaypoint(Vec3 target, double speed) {
        if (target == null || dragon.level().isClientSide) {
            return false;
        }

        QueuedWaypoint waypoint = new QueuedWaypoint(target, speed, false, MovementMode.AUTO);
        waypointQueue.add(waypoint);
        if (!isPathing()) {
            advanceToNextWaypoint();
        }
        return true;
    }

    public boolean setWaypoint(LivingEntity target, double speed, boolean running) {
        setGroundMoveState(running);
        return setWaypoint(target, speed);
    }

    public boolean setWaypoint(Vec3 target, double speed, boolean running) {
        setGroundMoveState(running);
        if (target == null || dragon.level().isClientSide) {
            return false;
        }
        waypointQueue.clear();
        return startWaypoint(new QueuedWaypoint(target, speed, running, MovementMode.AUTO));
    }

    public boolean setGroundWaypoint(LivingEntity target, double speed) {
        if (target == null || !canUseGroundNavigation()) {
            clearGroundPath();
            return false;
        }
        return setGroundWaypoint(target.position(), speed);
    }

    public boolean setGroundWaypoint(Vec3 target, double speed) {
        if (target == null || !canUseGroundNavigation()) {
            clearGroundPath();
            return false;
        }
        waypointQueue.clear();
        return startWaypoint(new QueuedWaypoint(target, speed, false, MovementMode.GROUND));
    }

    public boolean addGroundWaypoint(Vec3 target, double speed) {
        if (target == null || dragon.level().isClientSide) {
            return false;
        }

        waypointQueue.add(new QueuedWaypoint(target, speed, false, MovementMode.GROUND));
        if (!isPathing()) {
            advanceToNextWaypoint();
        }
        return true;
    }

    public boolean moveToGroundTarget(LivingEntity target, double speed, boolean running) {
        setGroundMoveState(running);
        return setGroundWaypoint(target, speed);
    }

    public boolean moveToGroundPosition(Vec3 target, double speed, boolean running) {
        setGroundMoveState(running);
        return setGroundWaypoint(target, speed);
    }

    public void setLandingWaypoint(@Nullable LivingEntity target, double speed) {
        trySetLandingWaypoint(target, speed);
    }

    public boolean trySetLandingWaypoint(@Nullable LivingEntity target, double speed) {
        if (!dragon.canFly() || !(dragon instanceof DragonFlightCapable flightCapable)) {
            return false;
        }
        if (dragon.onGround()) {
            if (dragon.isAerial()) {
                flightCapable.markLandedNow();
                clearAllWaypoints();
                return true;
            }
            return false;
        }

        Vec3 landingTarget = findLandingTarget(target);
        if (landingTarget == null) {
            return false;
        }
        return trySetLandingWaypoint(landingTarget, speed, flightCapable);
    }

    public boolean trySetLandingWaypoint(@Nullable Vec3 landingTarget, double speed) {
        if (!dragon.canFly() || !(dragon instanceof DragonFlightCapable flightCapable)) {
            return false;
        }
        if (dragon.onGround()) {
            if (dragon.isAerial()) {
                flightCapable.markLandedNow();
                clearAllWaypoints();
                return true;
            }
            return false;
        }
        return trySetLandingWaypoint(landingTarget, speed, flightCapable);
    }

    private boolean trySetLandingWaypoint(@Nullable Vec3 landingTarget, double speed, DragonFlightCapable flightCapable) {
        if (landingTarget == null) {
            return false;
        }

        flightCapable.beginAiLanding();
        waypointQueue.clear();
        return startWaypoint(new QueuedWaypoint(landingTarget, speed, false, MovementMode.LANDING));
    }

    public @Nullable Vec3 findLandingTarget(@Nullable LivingEntity target) {
        BlockPos origin = target != null && target.isAlive() ? target.blockPosition() : dragon.blockPosition();
        double currentAltitude = Math.max(0.0D, dragon.getY()
                - dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dragon.getBlockX(), dragon.getBlockZ()));
        double minHorizontalDistanceSqr = currentAltitude > 6.0D
                ? MIN_AIRBORNE_LANDING_HORIZONTAL * MIN_AIRBORNE_LANDING_HORIZONTAL
                : 0.0D;

        for (int radius = 0; radius <= 32; radius += 8) {
            for (int attempt = 0; attempt < 14; attempt++) {
                int dx = radius == 0 ? 0 : dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                int dz = radius == 0 ? 0 : dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                if (dx * dx + dz * dz < minHorizontalDistanceSqr) {
                    continue;
                }

                BlockPos column = origin.offset(dx, 0, dz);
                if (!dragon.level().hasChunkAt(column)) {
                    continue;
                }

                BlockPos ground = findLandingGround(dragon, column, origin.getY());
                if (ground != null && isValidLandingSurface(dragon, ground)) {
                    return new Vec3(column.getX() + 0.5D, ground.getY() + 1.0D, column.getZ() + 0.5D);
                }
            }
        }
        return null;
    }

    public void clearAllWaypoints() {
        currentWaypoint = null;
        waypointQueue.clear();
        dragon.getNavigation().stop();
        if (dragon instanceof RideableFlyingDragon flyingDragon) {
            flyingDragon.clearAiFlightTarget();
        }
    }

    public void stop() {
        currentWaypoint = null;
        waypointQueue.clear();
        if (shouldUseAirMovement()) {
            clearGroundPath();
            if (!(dragon instanceof RideableFlyingDragon)) {
                dragon.getNavigation().stop();
            }
        } else {
            dragon.getNavigation().stop();
            if (dragon instanceof RideableFlyingDragon flyingDragon) {
                flyingDragon.clearAiFlightTarget();
            }
            setGroundIdle();
        }
    }

    public void setGroundIdle() {
        dragon.setRunning(false);
        dragon.setSprinting(false);
        dragon.setGroundMoveStateFromAI(0);
    }

    public void setGroundWalk() {
        dragon.setRunning(false);
        dragon.setSprinting(false);
        dragon.setGroundMoveStateFromAI(1);
    }

    public void setGroundRun() {
        dragon.setRunning(true);
        dragon.setSprinting(true);
        dragon.setGroundMoveStateFromAI(2);
    }

    public void setGroundMoveState(boolean running) {
        if (running) {
            setGroundRun();
        } else {
            setGroundWalk();
        }
    }

    public boolean isPathing() {
        if (currentWaypoint != null && waypointQueue.isEmpty() && hasArrived()) {
            return false;
        }
        if (shouldUseAirMovement()) {
            if (dragon instanceof RideableFlyingDragon flyingDragon) {
                return flyingDragon.isAiFlightPathing();
            }
            return dragon.getNavigation().isInProgress();
        }
        return canUseGroundNavigation() && dragon.getNavigation().isInProgress();
    }

    public boolean hasArrived() {
        if (currentWaypoint != null && currentWaypoint.mode().usesAir()) {
            if (dragon instanceof RideableFlyingDragon flyingDragon) {
                return flyingDragon.isAiFlightDone();
            }
            return dragon.getNavigation().isDone();
        }
        if (shouldUseAirMovement()) {
            if (dragon instanceof RideableFlyingDragon flyingDragon) {
                return flyingDragon.isAiFlightDone();
            }
            return dragon.getNavigation().isDone();
        }
        return !canUseGroundNavigation() || dragon.getNavigation().isDone();
    }

    public boolean hasFailed() {
        return !shouldUseAirMovement() && canUseGroundNavigation() && dragon.getNavigation().isStuck();
    }

    private boolean shouldUseAirMovement() {
        if (dragon instanceof RideableFlyingDragon flyingDragon && flyingDragon.isAiFlightPathing()) {
            return true;
        }
        return dragon.isFlying() || dragon.isTakeoff() || dragon.isHovering() || dragon.isLanding();
    }

    private void clearGroundPath() {
        dragon.getNavigation().stop();
    }

    private boolean startWaypoint(QueuedWaypoint waypoint) {
        currentWaypoint = waypoint;
        if (waypoint.running()) {
            setGroundRun();
        } else if (waypoint.mode() == MovementMode.GROUND) {
            setGroundWalk();
        }

        if (waypoint.mode().usesAir() || (waypoint.mode() == MovementMode.AUTO && shouldUseAirMovement())) {
            if (dragon instanceof RideableFlyingDragon flyingDragon) {
                flyingDragon.trackAiFlightTarget(waypoint.target(), waypoint.speed());
                return true;
            }
            return dragon.getNavigation().moveTo(waypoint.target().x, waypoint.target().y, waypoint.target().z, waypoint.speed());
        }

        if (!canUseGroundNavigation()) {
            clearGroundPath();
            return false;
        }
        return dragon.getNavigation().moveTo(waypoint.target().x, waypoint.target().y, waypoint.target().z, waypoint.speed());
    }

    private void advanceToNextWaypoint() {
        currentWaypoint = null;
        if (waypointQueue.isEmpty()) {
            return;
        }
        startWaypoint(waypointQueue.poll());
    }

    private boolean canUseGroundNavigation() {
        return !dragon.level().isClientSide
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && dragon.isAlive()
                && !dragon.isFlying()
                && !dragon.isTakeoff()
                && !dragon.isHovering()
                && !dragon.isLanding()
                && !dragon.isInWaterOrBubble()
                && !dragon.isInLava();
    }

    private static @Nullable BlockPos findLandingGround(Mob dragon, BlockPos column, int originY) {
        if (!dragon.level().dimensionType().hasCeiling()) {
            int surfaceY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            return new BlockPos(column.getX(), surfaceY - 1, column.getZ());
        }

        int minY = dragon.level().getMinBuildHeight();
        int maxY = dragon.level().getMaxBuildHeight() - 1;
        int startY = Math.min(maxY, Math.max(minY, originY + 8));
        for (int y = startY; y >= minY; y--) {
            BlockPos ground = new BlockPos(column.getX(), y, column.getZ());
            if (isValidLandingSurface(dragon, ground)) {
                return ground;
            }
        }
        return null;
    }

    private static boolean isValidLandingSurface(Mob dragon, BlockPos ground) {
        if (!dragon.level().hasChunkAt(ground)) {
            return false;
        }

        var state = dragon.level().getBlockState(ground);
        if (state.isAir() || !state.getFluidState().isEmpty() || !state.isFaceSturdy(dragon.level(), ground, Direction.UP)) {
            return false;
        }

        BlockPos above = ground.above();
        BlockPos aboveTwo = above.above();
        var aboveState = dragon.level().getBlockState(above);
        var aboveTwoState = dragon.level().getBlockState(aboveTwo);
        return aboveState.getCollisionShape(dragon.level(), above).isEmpty()
                && aboveState.getFluidState().isEmpty()
                && aboveTwoState.getCollisionShape(dragon.level(), aboveTwo).isEmpty()
                && aboveTwoState.getFluidState().isEmpty();
    }

    private record QueuedWaypoint(Vec3 target, double speed, boolean running, MovementMode mode) {
    }

    private enum MovementMode {
        AUTO,
        GROUND,
        LANDING;

        private boolean usesAir() {
            return this == LANDING;
        }
    }
}
