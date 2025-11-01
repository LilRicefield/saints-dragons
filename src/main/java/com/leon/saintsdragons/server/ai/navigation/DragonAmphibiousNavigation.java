package com.leon.saintsdragons.server.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Shared amphibious navigation that blends the vanilla {@link AmphibiousPathNavigation}
 * monitoring helpers modelled after Critters & Companions' otter navigator so large semiaquatic dragons avoid jitter when swimming.
 */
public class DragonAmphibiousNavigation extends AmphibiousPathNavigation {
    private static final double MIN_PROGRESS_SQR = 0.35D * 0.35D;
    private static final int PROGRESS_TICKS = 20;

    private Vec3 lastProgressSample = Vec3.ZERO;
    private int lastProgressTick;
    private int stalledRepaths;

    public DragonAmphibiousNavigation(Mob mob, Level level) {
        super(mob, level);
        this.setCanFloat(true);
        this.lastProgressSample = this.getTempMobPos();
        this.lastProgressTick = this.mob.tickCount;
    }

    @Override
    protected @NotNull PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new DragonAmphibiousNodeEvaluator();
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    public boolean isStableDestination(@NotNull BlockPos pos) {
        if (!this.level.getFluidState(pos).isEmpty()) {
            return true;
        }

        BlockState state = this.level.getBlockState(pos);
        if (state.isAir()) {
            BlockState below = this.level.getBlockState(pos.below());
            return !below.isAir() || !below.getFluidState().isEmpty();
        }

        return super.isStableDestination(pos);
    }

    @Override
    protected void followThePath() {
        Path currentPath = this.path;
        if (currentPath == null || currentPath.isDone()) {
            return;
        }

        Vec3 entityPos = this.getTempMobPos();
        tryAdvanceSameY(currentPath, entityPos);
        tryShortcut(currentPath, entityPos);

        if (currentPath.isDone()) {
            return;
        }

        Vec3 target = currentPath.getNextEntityPos(this.mob);
        if (target == null) {
            return;
        }

        float reach = Math.max(0.8F, this.mob.getBbWidth() * 0.5F);
        if (entityPos.distanceToSqr(target) < (reach * reach)) {
            currentPath.advance();
            if (currentPath.isDone()) {
                return;
            }
            target = currentPath.getNextEntityPos(this.mob);
            if (target == null) {
                return;
            }
        }

        this.mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, this.speedModifier);
        monitorProgress(entityPos, target);
    }

    private void tryAdvanceSameY(Path path, Vec3 entityPos) {
        int next = path.getNextNodeIndex();
        int limit = path.getNodeCount();
        double yFloor = Math.floor(entityPos.y);

        int lastSameY = next;
        while (lastSameY < limit && path.getNode(lastSameY).y == yFloor) {
            lastSameY++;
        }

        for (int i = lastSameY - 1; i > next; --i) {
            Vec3 candidate = path.getEntityPosAtNode(this.mob, i);
            if (hasLineOfSight(entityPos, candidate)) {
                path.setNextNodeIndex(i);
                return;
            }
        }
    }

    private void tryShortcut(Path path, Vec3 entityPos) {
        int next = path.getNextNodeIndex();
        int max = Math.min(next + 6, path.getNodeCount() - 1);
        for (int i = max; i > next; --i) {
            Vec3 candidate = path.getEntityPosAtNode(this.mob, i);
            if (candidate.distanceToSqr(entityPos) <= 16.0D && hasLineOfSight(entityPos, candidate)) {
                path.setNextNodeIndex(i);
                return;
            }
        }
    }

    private void monitorProgress(Vec3 entityPos, Vec3 target) {
        if (this.mob.tickCount - this.lastProgressTick < PROGRESS_TICKS) {
            return;
        }

        if (entityPos.distanceToSqr(this.lastProgressSample) < MIN_PROGRESS_SQR) {
            this.recomputePath();
            this.stalledRepaths++;
            if (this.stalledRepaths >= 2) {
                nudgeSideways(entityPos, target);
                this.stalledRepaths = 0;
            }
        } else {
            this.stalledRepaths = 0;
        }

        this.lastProgressSample = entityPos;
        this.lastProgressTick = this.mob.tickCount;
    }

    private void nudgeSideways(Vec3 current, Vec3 target) {
        Vec3 direction = target.subtract(current);
        if (direction.lengthSqr() <= 1.0E-4D) {
            return;
        }

        Vec3 flatDir = new Vec3(direction.x, 0.0D, direction.z);
        if (flatDir.lengthSqr() <= 1.0E-4D) {
            return;
        }

        Vec3 perpendicular = new Vec3(-flatDir.z, 0.0D, flatDir.x).normalize();
        double offset = Math.max(1.5D, this.mob.getBbWidth());
        double side = this.mob.getRandom().nextBoolean() ? offset : -offset;

        BlockPos sidePos = BlockPos.containing(current.x + perpendicular.x * side,
                current.y,
                current.z + perpendicular.z * side);
        if (this.isStableDestination(sidePos)) {
            this.moveTo(sidePos.getX() + 0.5D, sidePos.getY(), sidePos.getZ() + 0.5D, Math.max(1.0D, this.speedModifier));
        }
    }

    private boolean hasLineOfSight(Vec3 from, Vec3 to) {
        if (this.nodeEvaluator == null) {
            return false;
        }

        Vec3 delta = to.subtract(from);
        double maxT = delta.length();
        if (maxT < 1.0E-6D) {
            return true;
        }

        double dx = delta.x / maxT;
        double dy = delta.y / maxT;
        double dz = delta.z / maxT;

        int currentX = Mth.floor(from.x);
        int currentY = Mth.floor(from.y);
        int currentZ = Mth.floor(from.z);

        double nextX = computeNextBoundary(from.x, dx);
        double nextY = computeNextBoundary(from.y, dy);
        double nextZ = computeNextBoundary(from.z, dz);

        double deltaX = dx == 0.0D ? Double.POSITIVE_INFINITY : 1.0D / Math.abs(dx);
        double deltaY = dy == 0.0D ? Double.POSITIVE_INFINITY : 1.0D / Math.abs(dy);
        double deltaZ = dz == 0.0D ? Double.POSITIVE_INFINITY : 1.0D / Math.abs(dz);

        double t = 0.0D;
        while (t <= maxT) {
            BlockPathTypes type = this.nodeEvaluator.getBlockPathType(this.level, currentX, currentY, currentZ, this.mob);
            float malus = this.mob.getPathfindingMalus(type);
            if (malus < 0.0F || malus >= 8.0F || type == BlockPathTypes.DAMAGE_FIRE || type == BlockPathTypes.DAMAGE_OTHER) {
                return false;
            }

            if (nextX < nextY) {
                if (nextX < nextZ) {
                    currentX += dx > 0.0D ? 1 : -1;
                    t = nextX;
                    nextX += deltaX;
                } else {
                    currentZ += dz > 0.0D ? 1 : -1;
                    t = nextZ;
                    nextZ += deltaZ;
                }
            } else {
                if (nextY < nextZ) {
                    currentY += dy > 0.0D ? 1 : -1;
                    t = nextY;
                    nextY += deltaY;
                } else {
                    currentZ += dz > 0.0D ? 1 : -1;
                    t = nextZ;
                    nextZ += deltaZ;
                }
            }
        }
        return true;
    }

    private double computeNextBoundary(double start, double direction) {
        if (direction == 0.0D) {
            return Double.POSITIVE_INFINITY;
        }

        double boundary = direction > 0.0D ? Mth.floor(start) + 1 : Mth.floor(start);
        return (boundary - start) / direction;
    }

    public void stop() {
        super.stop();
        this.stalledRepaths = 0;
        this.lastProgressSample = this.getTempMobPos();
        this.lastProgressTick = this.mob.tickCount;
    }
}
