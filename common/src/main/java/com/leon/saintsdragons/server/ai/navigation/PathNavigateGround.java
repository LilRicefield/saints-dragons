package com.leon.saintsdragons.server.ai.navigation;

import com.leon.saintsdragons.server.ai.pathfinding.DragonWalkNodeEvaluator;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import javax.annotation.Nonnull;

public class PathNavigateGround extends GroundPathNavigation {
    private static final double MAX_SHORTCUT_DISTANCE = 10.0D;
    private static final int MAX_SWEEP_STEPS = 12;

    public PathNavigateGround(Mob mob, Level world) {
        super(mob, world);
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
        
        // Try to shortcut to later path nodes for smoother movement
        if (this.tryShortcut(path, new Vec3(this.mob.getX(), this.mob.getY(), this.mob.getZ()), pathLength, base, max)) {
            if (this.isAt(path, 0.5F) || this.atElevationChange(path) && this.isAt(path, this.mob.getBbWidth() * 0.5F)) {
                path.setNextNodeIndex(path.getNextNodeIndex() + 1);
            }
        }
        this.doStuckDetection(entityPos);
    }

    private boolean isAt(Path path, float threshold) {
        final Vec3 pathPos = path.getNextEntityPos(this.mob);
        return Mth.abs((float) (this.mob.getX() - pathPos.x)) < threshold &&
                Mth.abs((float) (this.mob.getZ() - pathPos.z)) < threshold &&
                Math.abs(this.mob.getY() - pathPos.y) < 1.0D;
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
            return this.mob.isInWaterOrBubble();
        }

        return pathType != BlockPathTypes.OPEN;
    }
}
