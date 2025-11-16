package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Extended-range swimming goal for Nulljaw.
 *
 * Fixes circular swimming pattern by using MUCH longer patrol ranges (128 blocks).
 * Nulljaw is a large aquatic dragon - it needs room to roam!
 *
 * Based on NulljawRandomSwimGoal but with:
 * - 128 block horizontal range (vs 10!)
 * - 20 block vertical range (vs 4)
 * - Faster AI swimming speed (2.0x vs 1.2x)
 * - Varied target selection
 */
public class NulljawSwimGoal extends RandomStrollGoal {

    private final Nulljaw drake;
    private static final double AI_SWIM_SPEED = 2.0D; // Fast AI patrol speed

    public NulljawSwimGoal(Nulljaw drake, double v, int chance) {
        super(drake, AI_SWIM_SPEED, chance, false);
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
        // 25% chance to head toward surface
        if (drake.getRandom().nextFloat() < 0.25F) {
            Vec3 surface = findSurfaceTarget();
            if (surface != null) {
                return surface;
            }
        }

        // LONG RANGE SWIMMING - 128 blocks horizontal, 20 vertical
        // This prevents circular swimming patterns!
        Vec3 pos = DefaultRandomPos.getPos(drake, 128, 20);
        int attempts = 0;

        while (pos != null &&
               !drake.level().getBlockState(net.minecraft.core.BlockPos.containing(pos))
                   .isPathfindable(drake.level(), net.minecraft.core.BlockPos.containing(pos), PathComputationType.WATER)
               && attempts++ < 16) { // More attempts for longer range
            pos = DefaultRandomPos.getPos(drake, 128, 20);
        }

        return pos;
    }

    @Nullable
    private Vec3 findSurfaceTarget() {
        net.minecraft.core.BlockPos.MutableBlockPos cursor = drake.blockPosition().mutable();

        // Swim upward to find surface
        while (drake.level().getFluidState(cursor).is(FluidTags.WATER) &&
               cursor.getY() < drake.level().getMaxBuildHeight()) {
            cursor.move(0, 1, 0);
        }

        cursor.move(0, -1, 0); // Back down one block

        if (!drake.level().getFluidState(cursor).is(FluidTags.WATER)) {
            return null;
        }

        if (drake.level().getBlockState(cursor.above()).isAir()) {
            return new Vec3(cursor.getX() + 0.5D, cursor.getY() + 0.2D, cursor.getZ() + 0.5D);
        }

        return null;
    }
}
