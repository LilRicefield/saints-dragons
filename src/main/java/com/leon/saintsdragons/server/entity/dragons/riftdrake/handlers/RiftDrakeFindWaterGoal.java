package com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers;

import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class RiftDrakeFindWaterGoal extends Goal {

    private final RiftDrakeEntity drake;
    private BlockPos targetPos;

    public RiftDrakeFindWaterGoal(RiftDrakeEntity drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!drake.onGround() || drake.level().getFluidState(drake.blockPosition()).is(FluidTags.WATER)) {
            return false;
        }
        if (!drake.isInWater() && drake.getAirSupply() < drake.getMaxAirSupply() - 80) {
            return false;
        }
        Vec3 target = pickWaterPosition();
        targetPos = target != null ? BlockPos.containing(target) : null;
        return targetPos != null;
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
        if (!drake.isInWater() && drake.getAirSupply() < drake.getMaxAirSupply() - 80) {
            drake.getNavigation().stop();
            return false;
        }
        return targetPos != null && !drake.level().getFluidState(drake.blockPosition()).is(FluidTags.WATER) && !drake.getNavigation().isDone();
    }

    @Nullable
    private Vec3 pickWaterPosition() {
        for (int i = 0; i < 15; i++) {
            Vec3 candidate = drake.position().add(drake.getRandom().nextInt(14) - 7, 0, drake.getRandom().nextInt(14) - 7);
            BlockPos pos = BlockPos.containing(candidate);
            if (drake.level().getFluidState(pos).is(FluidTags.WATER)) {
                return new Vec3(pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return null;
    }
}

