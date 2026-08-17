package com.leon.saintsdragons.server.ai.navigation;

import com.leon.saintsdragons.server.ai.pathfinding.DragonWalkNodeEvaluator;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import javax.annotation.Nonnull;

public class PathNavigateGround extends GroundPathNavigation {
    private static final double MAX_SHORTCUT_DISTANCE = 10.0D;
    private static final double MAX_GROUNDED_ASCENDING_WAYPOINT_OFFSET = 0.5D;
    private static final double MAX_DESCENDING_WAYPOINT_OFFSET = 1.5D;
    private static final double MIN_BLOCKED_ASCENT_RISE = 0.25D;
    private static final double MAX_BLOCKED_ASCENT_RISE = 1.5D;
    private static final int MAX_SWEEP_STEPS = 12;
    private static final int BLOCKED_ASCENT_STALL_TICKS = 4;
    private static final int BLOCKED_ASCENT_JUMP_COOLDOWN_TICKS = 10;
    private static final int MAX_BLOCKED_ASCENT_JUMPS_PER_NODE = 2;
    private boolean waterEntryAllowed;
    private float finalWaypointTolerance = Float.NaN;
    private @Nullable Path debugTrackedPath;
    private int debugTrackedNodeIndex = -1;
    private int debugNodeStallTicks;
    private int debugSkippedFollowTicks;
    private boolean debugPathActive;
    private boolean debugCanUpdatePath = true;
    private boolean debugMoveCommandIssued;
    private @Nullable Vec3 debugMoveTarget;
    private double debugMoveSpeed;
    private int blockedAscentNodeIndex = -1;
    private int blockedAscentJumpAttempts;
    private int blockedAscentJumpCooldown;
    private int debugBlockedAscentJumpCount;
    private int debugLastBlockedAscentNode = -1;

    public PathNavigateGround(Mob mob, Level world) {
        super(mob, world);
    }

    public void setWaterEntryAllowed(boolean allowed) {
        if (waterEntryAllowed == allowed) {
            return;
        }
        waterEntryAllowed = allowed;
        setCanFloat(allowed);
    }

    public boolean isWaterEntryAllowed() {
        return waterEntryAllowed;
    }

    public void setFinalWaypointTolerance(double tolerance) {
        finalWaypointTolerance = (float) Math.max(0.05D, tolerance);
    }

    public void clearFinalWaypointTolerance() {
        finalWaypointTolerance = Float.NaN;
    }

    @Override
    public void tick() {
        if (blockedAscentJumpCooldown > 0) {
            blockedAscentJumpCooldown--;
        }

        Path pathBeforeTick = this.path;
        boolean activeBeforeTick = pathBeforeTick != null && !pathBeforeTick.isDone();
        debugCanUpdatePath = this.canUpdatePath();
        if (activeBeforeTick) {
            if (pathBeforeTick != debugTrackedPath) {
                debugSkippedFollowTicks = debugCanUpdatePath ? 0 : 1;
            } else if (!debugCanUpdatePath) {
                debugSkippedFollowTicks++;
            } else {
                debugSkippedFollowTicks = 0;
            }
        }

        super.tick();

        Path activePath = this.path;
        debugPathActive = activePath != null && !activePath.isDone();
        if (debugPathActive) {
            int nodeIndex = activePath.getNextNodeIndex();
            boolean newPath = activePath != debugTrackedPath;
            if (newPath || nodeIndex != debugTrackedNodeIndex) {
                if (newPath) {
                    debugBlockedAscentJumpCount = 0;
                    debugLastBlockedAscentNode = -1;
                }
                debugTrackedPath = activePath;
                debugTrackedNodeIndex = nodeIndex;
                debugNodeStallTicks = 0;
                blockedAscentNodeIndex = nodeIndex;
                blockedAscentJumpAttempts = 0;
                blockedAscentJumpCooldown = 0;
            } else {
                debugNodeStallTicks++;
            }

            tryAssistBlockedAscent(activePath, nodeIndex);
        }

        MoveControl moveControl = this.mob.getMoveControl();
        debugMoveCommandIssued = moveControl.hasWanted();
        debugMoveTarget = debugMoveCommandIssued
                ? new Vec3(moveControl.getWantedX(), moveControl.getWantedY(), moveControl.getWantedZ())
                : null;
        debugMoveSpeed = debugMoveCommandIssued ? moveControl.getSpeedModifier() : 0.0D;
    }

    public DebugSnapshot getDebugSnapshot() {
        return new DebugSnapshot(
                debugPathActive,
                debugCanUpdatePath,
                debugSkippedFollowTicks,
                debugNodeStallTicks,
                debugMoveCommandIssued,
                debugMoveTarget,
                debugMoveSpeed,
                finalWaypointTolerance,
                debugBlockedAscentJumpCount,
                debugLastBlockedAscentNode,
                blockedAscentJumpCooldown
        );
    }

    private void tryAssistBlockedAscent(Path path, int nodeIndex) {
        if (nodeIndex != blockedAscentNodeIndex
                || debugNodeStallTicks < BLOCKED_ASCENT_STALL_TICKS
                || blockedAscentJumpCooldown > 0
                || blockedAscentJumpAttempts >= MAX_BLOCKED_ASCENT_JUMPS_PER_NODE
                || !this.mob.onGround()
                || !this.mob.horizontalCollision
                || this.mob.isInWaterOrBubble()) {
            return;
        }

        Vec3 waypoint = path.getNextEntityPos(this.mob);
        double rise = waypoint.y - this.mob.getY();
        double dx = waypoint.x - this.mob.getX();
        double dz = waypoint.z - this.mob.getZ();
        double maximumHorizontalDistance = Math.max(1.0D, this.mob.getBbWidth());
        if (rise <= MIN_BLOCKED_ASCENT_RISE
                || rise > MAX_BLOCKED_ASCENT_RISE
                || dx * dx + dz * dz > maximumHorizontalDistance * maximumHorizontalDistance) {
            return;
        }

        this.mob.getJumpControl().jump();
        blockedAscentJumpAttempts++;
        blockedAscentJumpCooldown = BLOCKED_ASCENT_JUMP_COOLDOWN_TICKS;
        debugBlockedAscentJumpCount++;
        debugLastBlockedAscentNode = nodeIndex;
    }

    @Override
    public boolean moveTo(Path path, double speedModifier) {
        clearFinalWaypointTolerance();
        return super.moveTo(path, speedModifier);
    }

    @Override
    public void stop() {
        clearFinalWaypointTolerance();
        super.stop();
    }

    @Override
    protected @NotNull PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new DragonWalkNodeEvaluator(() ->
                this.mob instanceof DragonEntity dragon
                        && this.level instanceof ServerLevel serverLevel
                        && DragonDestructionManager.canApplyPassiveTreeDestruction(serverLevel, dragon));
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinderGround(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected void followThePath() {
        Path path = Objects.requireNonNull(this.path);
        Vec3 entityPos = this.getTempMobPos();
        int pathLength = path.getNodeCount();
        
        // Find the end of the current horizontal plane to avoid unnecessary vertical checks
        for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) {
            if (path.getNode(i).y != Math.floor(entityPos.y)) {
                pathLength = i;
                break;
            }
        }
        
        final Vec3 base = entityPos.add(-this.mob.getBbWidth() * 0.5F, 0.0F, -this.mob.getBbWidth() * 0.5F);
        final Vec3 max = base.add(this.mob.getBbWidth(), this.mob.getBbHeight(), this.mob.getBbWidth());
        float waypointTolerance = this.mob.getBbWidth() > 0.75F
                ? this.mob.getBbWidth() * 0.5F
                : 0.75F - this.mob.getBbWidth() * 0.5F;
        if (path.getNextNodeIndex() == path.getNodeCount() - 1
                && Float.isFinite(finalWaypointTolerance)) {
            waypointTolerance = Math.min(waypointTolerance, finalWaypointTolerance);
        }
        
        // Try to shortcut to later path nodes for smoother movement
        if (this.tryShortcut(path, new Vec3(this.mob.getX(), this.mob.getY(), this.mob.getZ()), pathLength, base, max)) {
            if (this.isAt(path, waypointTolerance)
                    || this.atElevationChange(path)
                    && this.isAt(path, Math.min(this.mob.getBbWidth() * 0.5F, waypointTolerance))) {
                path.setNextNodeIndex(path.getNextNodeIndex() + 1);
            }
        }
        this.doStuckDetection(entityPos);
    }

    private boolean isAt(Path path, float threshold) {
        final Vec3 pathPos = path.getNextEntityPos(this.mob);
        final double verticalOffset = this.mob.getY() - pathPos.y;
        final boolean reachedAscendingElevation = verticalOffset >= 0.0D
                || this.mob.onGround()
                && verticalOffset >= -MAX_GROUNDED_ASCENDING_WAYPOINT_OFFSET;
        return Mth.abs((float) (this.mob.getX() - pathPos.x)) < threshold &&
                Mth.abs((float) (this.mob.getZ() - pathPos.z)) < threshold &&
                reachedAscendingElevation &&
                verticalOffset <= MAX_DESCENDING_WAYPOINT_OFFSET;
    }

    private boolean atElevationChange(Path path) {
        final int curr = path.getNextNodeIndex();
        final int end = Math.min(path.getNodeCount(), curr + Mth.ceil(this.mob.getBbWidth() * 0.5F) + 1);
        final int currY = path.getNode(curr).y;
        for (int i = curr + 1; i < end; i++) {
            if (path.getNode(i).y != currY) {
                return true;
            }
        }
        return false;
    }

    private boolean tryShortcut(Path path, Vec3 entityPos, int pathLength, Vec3 base, Vec3 max) {
        for (int i = pathLength; --i > path.getNextNodeIndex(); ) {
            final Vec3 vec = path.getEntityPosAtNode(this.mob, i).subtract(entityPos);
            if (vec.lengthSqr() > MAX_SHORTCUT_DISTANCE * MAX_SHORTCUT_DISTANCE) {
                continue;
            }
            if (this.sweep(vec, base, max)) {
                path.setNextNodeIndex(i);
                return false; // Found a shortcut
            }
        }
        return true; // No shortcut found, continue normally
    }

    private boolean sweep(Vec3 vec, Vec3 base, Vec3 max) {
        double distance = vec.length();
        if (distance < 1.0E-6D) return true;

        // Sweep the mob's body AABB along the candidate shortcut path.
        // If any sample collides, reject the shortcut and keep vanilla node progression.
        int steps = Mth.clamp((int) Math.ceil(distance), 2, MAX_SWEEP_STEPS);
        Vec3 step = vec.scale(1.0D / steps);

        double minX = base.x;
        double minY = base.y;
        double minZ = base.z;
        double maxX = max.x;
        double maxY = max.y;
        double maxZ = max.z;

        for (int n = 1; n <= steps; n++) {
            double dx = step.x * n;
            double dy = step.y * n;
            double dz = step.z * n;
            net.minecraft.world.phys.AABB probe = new net.minecraft.world.phys.AABB(
                    minX + dx, minY + dy, minZ + dz,
                    maxX + dx, maxY + dy, maxZ + dz
            );
            if (!this.level.noCollision(this.mob, probe)) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected boolean hasValidPathType(@Nonnull BlockPathTypes pathType) {
        if (pathType == BlockPathTypes.LAVA) {
            return false; // Dragons avoid lava paths entirely
        }

        if (pathType == BlockPathTypes.WATER) {
            return waterEntryAllowed || this.mob.isInWaterOrBubble();
        }

        return pathType != BlockPathTypes.OPEN;
    }

    public record DebugSnapshot(boolean pathActive,
                                boolean canUpdatePath,
                                int skippedFollowTicks,
                                int nodeStallTicks,
                                boolean moveCommandIssued,
                                @Nullable Vec3 moveTarget,
                                double moveSpeed,
                                float finalWaypointTolerance,
                                int blockedAscentJumpCount,
                                int lastBlockedAscentNode,
                                int blockedAscentJumpCooldown) {
    }
}
