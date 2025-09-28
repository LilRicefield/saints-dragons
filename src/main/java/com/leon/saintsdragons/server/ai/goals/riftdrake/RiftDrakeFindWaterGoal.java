package com.leon.saintsdragons.server.ai.goals.riftdrake;

import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class RiftDrakeFindWaterGoal extends Goal {

    private final RiftDrakeEntity drake;
    private BlockPos targetPos;
    private final int executionChance = 30;

    public RiftDrakeFindWaterGoal(RiftDrakeEntity drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only seek water when on ground and not already in water
        if (this.drake.onGround() && !this.drake.level().getFluidState(this.drake.blockPosition()).is(FluidTags.WATER)) {
            // Use the AquaticDragon interface method to determine if we should enter water
            if (this.drake.shouldEnterWater() && (this.drake.getTarget() != null || this.drake.getRandom().nextInt(executionChance) == 0)) {
                targetPos = generateTarget();
                return targetPos != null;
            }
        }
        return false;
    }

    @Override
    public void start() {
        if (targetPos != null) {
            drake.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0D);
        }
    }

    @Override
    public void tick() {
        if (targetPos != null) {
            drake.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0D);
        }
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if we no longer should enter water
        if (!this.drake.shouldEnterWater()) {
            this.drake.getNavigation().stop();
            return false;
        }
        return !this.drake.getNavigation().isDone() && targetPos != null && !this.drake.level().getFluidState(this.drake.blockPosition()).is(FluidTags.WATER);
    }

    public BlockPos generateTarget() {
        BlockPos blockpos = null;
        final RandomSource random = this.drake.getRandom();
        final int range = this.drake.getWaterSearchRange();
        for(int i = 0; i < 15; i++) {
            BlockPos blockPos = this.drake.blockPosition().offset(random.nextInt(range) - range/2, 3, random.nextInt(range) - range/2);
            while (this.drake.level().isEmptyBlock(blockPos) && blockPos.getY() > 1) {
                blockPos = blockPos.below();
            }

            if (this.drake.level().getFluidState(blockPos).is(FluidTags.WATER)) {
                blockpos = blockPos;
            }
        }
        return blockpos;
    }
}

