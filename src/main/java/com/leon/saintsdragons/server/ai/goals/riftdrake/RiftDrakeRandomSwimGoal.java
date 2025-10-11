package com.leon.saintsdragons.server.ai.goals.riftdrake;

import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RiftDrakeRandomSwimGoal extends RandomStrollGoal {

    private final RiftDrakeEntity drake;

    public RiftDrakeRandomSwimGoal(RiftDrakeEntity drake, double speed, int chance) {
        super(drake, speed, chance, false);
        this.drake = drake;
    }

    public void forceTrigger() {
        this.forceTrigger = true;
    }

    @Override
    public boolean canUse() {
        // Don't use if being ridden, is a passenger, has a target, or is sitting
        if (this.drake.isVehicle() || this.drake.isPassenger() ||
            this.drake.getTarget() != null || this.drake.isOrderedToSit()) {
            return false;
        }

        // Only use when actually in water and swimming
        if (!this.drake.isInWater() || !this.drake.isSwimming()) {
            return false;
        }

        // Random chance check (unless force triggered)
        if (!this.forceTrigger) {
            if (this.drake.getRandom().nextInt(this.interval) != 0) {
                return false;
            }
        }

        // Try to find a valid position to swim to
        Vec3 vector3d = this.getPosition();
        if (vector3d == null) {
            return false;
        }

        this.wantedX = vector3d.x;
        this.wantedY = vector3d.y;
        this.wantedZ = vector3d.z;
        this.forceTrigger = false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if no longer in water or being controlled
        if (!this.drake.isInWater() || this.drake.isVehicle() ||
            this.drake.getTarget() != null || this.drake.isOrderedToSit()) {
            return false;
        }

        // Continue if navigation is still in progress
        return !this.drake.getNavigation().isDone();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        if (drake.getRandom().nextFloat() < 0.25F) {
            Vec3 surface = findSurfaceTarget();
            if (surface != null) {
                return surface;
            }
        }
        Vec3 pos = DefaultRandomPos.getPos(drake, 10, 4);
        int attempts = 0;
        while (pos != null && !drake.level().getBlockState(net.minecraft.core.BlockPos.containing(pos)).isPathfindable(drake.level(), net.minecraft.core.BlockPos.containing(pos), PathComputationType.WATER) && attempts++ < 12) {
            pos = DefaultRandomPos.getPos(drake, 10, 4);
        }
        return pos;
    }

    @Nullable
    private Vec3 findSurfaceTarget() {
        net.minecraft.core.BlockPos.MutableBlockPos cursor = drake.blockPosition().mutable();
        while (drake.level().getFluidState(cursor).is(FluidTags.WATER) && cursor.getY() < drake.level().getMaxBuildHeight()) {
            cursor.move(0, 1, 0);
        }
        cursor.move(0, -1, 0);
        if (!drake.level().getFluidState(cursor).is(FluidTags.WATER)) {
            return null;
        }
        if (drake.level().getBlockState(cursor.above()).isAir()) {
            return new Vec3(cursor.getX() + 0.5D, cursor.getY() + 0.2D, cursor.getZ() + 0.5D);
        }
        return null;
    }
}

