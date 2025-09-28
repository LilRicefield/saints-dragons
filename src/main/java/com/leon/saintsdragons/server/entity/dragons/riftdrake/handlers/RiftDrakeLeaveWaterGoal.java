package com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers;

import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.FluidTags;

import java.util.EnumSet;

public class RiftDrakeLeaveWaterGoal extends Goal {

    private final RiftDrakeEntity drake;
    private Vec3 targetPos;

    public RiftDrakeLeaveWaterGoal(RiftDrakeEntity drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!drake.isInWater() || drake.getAirSupply() > drake.getMaxAirSupply() - 40) {
            return false;
        }
        targetPos = generateTarget();
        return targetPos != null;
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
    }

    @Override
    public boolean canContinueToUse() {
        if (drake.getAirSupply() > drake.getMaxAirSupply() - 40) {
            drake.getNavigation().stop();
            return false;
        }
        return targetPos != null && drake.isInWater() && !drake.getNavigation().isDone();
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

