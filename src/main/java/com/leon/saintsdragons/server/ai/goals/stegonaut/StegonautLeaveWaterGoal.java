package com.leon.saintsdragons.server.ai.goals.stegonaut;

import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Urges the Stegonaut to leave water as quickly as possible so it resumes ground behaviour.
 */
public class StegonautLeaveWaterGoal extends Goal {

    private static final int NEARBY_SEARCH_RADIUS = 6;
    private static final int RANDOM_SEARCH_ATTEMPTS = 12;
    private static final int PATH_RECHECK_DELAY = 20;

    private final Stegonaut drake;
    private final double moveSpeed;

    private Vec3 targetPosition;
    private int pathCooldown;
    private boolean forcedStandDuringEscape;

    public StegonautLeaveWaterGoal(Stegonaut drake, double moveSpeed) {
        this.drake = drake;
        this.moveSpeed = moveSpeed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!drake.isInWaterOrBubble()) {
            return false;
        }

        targetPosition = findDryLand();
        return targetPosition != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!drake.isInWaterOrBubble()) {
            return false;
        }

        if (targetPosition == null) {
            return false;
        }

        return !drake.getNavigation().isDone();
    }

    @Override
    public void start() {
        pathCooldown = 0;
        forcedStandDuringEscape = false;

        // Stop playing dead so the drake can actually move out of danger.
        if (drake.isPlayingDead()) {
            drake.clearPlayDeadGoal();
        }

        // Temporarily override sitting so the drake is free to walk out of water.
        if (drake.isOrderedToSit()) {
            forcedStandDuringEscape = true;
            drake.setOrderedToSit(false);
        }

        moveToTarget();
    }

    @Override
    public void stop() {
        drake.getNavigation().stop();
        targetPosition = null;

        if (forcedStandDuringEscape) {
            drake.refreshCommandState();
        }
    }

    @Override
    public void tick() {
        if (targetPosition == null) {
            return;
        }

        drake.getLookControl().setLookAt(targetPosition.x, targetPosition.y, targetPosition.z, 45.0F, 45.0F);

        if (--pathCooldown <= 0) {
            // Try to stay focused on the same target. If pathing fails, look for a new shoreline.
            if (!drake.getNavigation().moveTo(targetPosition.x, targetPosition.y, targetPosition.z, moveSpeed)) {
                targetPosition = findDryLand();
                if (targetPosition != null) {
                    drake.getNavigation().moveTo(targetPosition.x, targetPosition.y, targetPosition.z, moveSpeed);
                }
            }
            pathCooldown = PATH_RECHECK_DELAY;
        }

        // Give a little upward nudge when colliding with blocks underwater.
        if (drake.horizontalCollision && drake.isInWater()) {
            drake.setDeltaMovement(drake.getDeltaMovement().add(0.0D, 0.08D, 0.0D));
        }
    }

    private void moveToTarget() {
        if (targetPosition != null) {
            drake.getNavigation().moveTo(targetPosition.x, targetPosition.y, targetPosition.z, moveSpeed);
        }
    }

    private Vec3 findDryLand() {
        Vec3 nearby = searchNearbyDryLand();
        if (nearby != null) {
            return nearby;
        }

        return findRandomDryLand();
    }

    private Vec3 searchNearbyDryLand() {
        Level level = drake.level();
        BlockPos origin = drake.blockPosition();
        Vec3 originCenter = Vec3.atCenterOf(origin);

        BlockPos bestCandidate = null;
        double bestDistance = Double.MAX_VALUE;

        for (int radius = 1; radius <= NEARBY_SEARCH_RADIUS; radius++) {
            int horizontalRadius = radius;
            for (BlockPos pos : BlockPos.betweenClosed(
                    origin.offset(-horizontalRadius, -1, -horizontalRadius),
                    origin.offset(horizontalRadius, 2, horizontalRadius))) {

                int dx = Math.abs(pos.getX() - origin.getX());
                int dz = Math.abs(pos.getZ() - origin.getZ());
                if (dx > radius || dz > radius) {
                    continue;
                }

                if (!isDryDestination(level, pos)) {
                    continue;
                }

                double distance = pos.distToCenterSqr(originCenter.x, originCenter.y, originCenter.z);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestCandidate = pos.immutable();
                }
            }

            if (bestCandidate != null) {
                return Vec3.atBottomCenterOf(bestCandidate);
            }
        }

        return null;
    }

    private Vec3 findRandomDryLand() {
        Level level = drake.level();
        Vec3 candidate = null;

        for (int attempts = 0; attempts < RANDOM_SEARCH_ATTEMPTS; attempts++) {
            candidate = LandRandomPos.getPos(drake, 24, 7);
            if (candidate == null) {
                continue;
            }

            BlockPos blockPos = BlockPos.containing(candidate);
            if (isDryDestination(level, blockPos)) {
                return Vec3.atBottomCenterOf(blockPos);
            }
        }

        // As a last resort, try to step upward in the same column.
        BlockPos columnExit = climbColumnToFindAir();
        return columnExit != null ? Vec3.atBottomCenterOf(columnExit) : null;
    }

    private BlockPos climbColumnToFindAir() {
        Level level = drake.level();
        BlockPos.MutableBlockPos cursor = drake.blockPosition().mutable();

        // Move upwards until we leave water or hit the build height.
        while (level.getFluidState(cursor).is(FluidTags.WATER) && cursor.getY() < level.getMaxBuildHeight()) {
            cursor.move(Direction.UP);
        }

        if (level.getFluidState(cursor).is(FluidTags.WATER)) {
            return null;
        }

        if (!isDryDestination(level, cursor)) {
            cursor.move(Direction.UP);
            if (!isDryDestination(level, cursor)) {
                return null;
            }
        }

        return cursor.immutable();
    }

    private boolean isDryDestination(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }

        if (!level.isEmptyBlock(pos)) {
            return false;
        }

        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);

        if (!level.getFluidState(below).isEmpty()) {
            return false;
        }

        if (!belowState.isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }

        BlockPos above = pos.above();
        if (!level.isLoaded(above) || !level.isEmptyBlock(above) || !level.getFluidState(above).isEmpty()) {
            return false;
        }

        BlockPos twoAbove = above.above();
        if (!level.isLoaded(twoAbove)) {
            return true;
        }

        return level.isEmptyBlock(twoAbove) && level.getFluidState(twoAbove).isEmpty();
    }
}
