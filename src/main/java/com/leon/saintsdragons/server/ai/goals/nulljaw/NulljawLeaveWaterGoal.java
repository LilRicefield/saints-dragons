package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;

import java.util.EnumSet;

public class NulljawLeaveWaterGoal extends Goal {

    private final Nulljaw drake;
    private Vec3 targetPos;
    private final int executionChance = 30;

    public NulljawLeaveWaterGoal(Nulljaw drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only leave water when in water and should leave
        if (this.drake.level().getFluidState(this.drake.blockPosition()).is(FluidTags.WATER) && 
            (this.drake.getTarget() != null || this.drake.getRandom().nextInt(executionChance) == 0)) {
            if (this.drake.shouldLeaveWater()) {
                targetPos = generateTarget();
                return targetPos != null;
            }
        }
        return false;
    }

    @Override
    public void start() {
        if (targetPos != null) {
            drake.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.0D);
        }
    }

    @Override
    public void tick() {
        if (targetPos != null) {
            drake.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.0D);
        }
        // Help with getting out of water when hitting walls
        if (this.drake.horizontalCollision && this.drake.isInWater()) {
            final float f1 = drake.getYRot() * Mth.DEG_TO_RAD;
            drake.setDeltaMovement(drake.getDeltaMovement().add(-Mth.sin(f1) * 0.2F, 0.1D, Mth.cos(f1) * 0.2F));
        }
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if we no longer should leave water
        if (!this.drake.shouldLeaveWater()) {
            this.drake.getNavigation().stop();
            return false;
        }
        return !this.drake.getNavigation().isDone() && targetPos != null && !this.drake.level().getFluidState(BlockPos.containing(targetPos)).is(FluidTags.WATER);
    }

    private Vec3 generateTarget() {
        Vec3 vector3d = LandRandomPos.getPos(drake, 23, 7);
        int tries = 0;
        while (vector3d != null && tries < 8) {
            boolean waterDetected = false;
            for (BlockPos blockpos1 : BlockPos.betweenClosed(BlockPos.containing(vector3d).offset(-2, -1, -2), BlockPos.containing(vector3d).offset(2, 0, 2))) {
                if (drake.level().getFluidState(blockpos1).is(FluidTags.WATER)) {
                    waterDetected = true;
                    break;
                }
            }
            if (waterDetected) {
                vector3d = LandRandomPos.getPos(drake, 23, 7);
            } else {
                return vector3d;
            }
            tries++;
        }
        return null;
    }
}

