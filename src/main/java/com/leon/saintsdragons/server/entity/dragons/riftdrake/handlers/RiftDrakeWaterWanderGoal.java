package com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers;

import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RiftDrakeWaterWanderGoal extends Goal {

    private final RiftDrakeEntity drake;
    private final double speedModifier;
    private final int interval;
    private boolean active;
    private Vec3 targetPosition;
    private int cooldown;

    public RiftDrakeWaterWanderGoal(RiftDrakeEntity drake, double speedModifier, int interval) {
        this.drake = drake;
        this.speedModifier = speedModifier;
        this.interval = interval;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean canUse() {
        if (!active || !drake.isInWater()) return false;
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (drake.getRandom().nextInt(interval) != 0) return false;
        Vec3 target = findSwimTarget();
        if (target == null) return false;
        this.targetPosition = target;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return active && drake.isInWater() && targetPosition != null &&
               !drake.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (targetPosition != null) {
            drake.getNavigation().moveTo(targetPosition.x, targetPosition.y, targetPosition.z, speedModifier);
            drake.setSwimmingTarget(targetPosition);
        }
    }

    @Override
    public void tick() {
        if (targetPosition != null && drake.isInWater()) {
            drake.getNavigation().moveTo(targetPosition.x, targetPosition.y, targetPosition.z, speedModifier);
            drake.setSwimmingTarget(targetPosition);
            drake.getLookControl().setLookAt(targetPosition.x, targetPosition.y, targetPosition.z, 10.0F, drake.getMaxHeadXRot());
        }
    }

    @Override
    public void stop() {
        targetPosition = null;
        cooldown = drake.getRandom().nextInt(40) + 20; // 1-3 second cooldown
        drake.getNavigation().stop();
        drake.clearSwimmingTarget();
    }

    private Vec3 findSwimTarget() {
        final double radius = 6.0;
        final double vertical = 2.0;
        Vec3 start = drake.position();
        for (int i = 0; i < 10; i++) {
            double dx = drake.getRandom().nextDouble() * 2 - 1;
            double dy = drake.getRandom().nextDouble() * 2 - 1;
            double dz = drake.getRandom().nextDouble() * 2 - 1;
            Vec3 candidate = start.add(dx * radius, dy * vertical, dz * radius);
            if (drake.level().getBlockState(net.minecraft.core.BlockPos.containing(candidate)).getFluidState().isEmpty()) {
                continue;
            }
            if (drake.level().getBlockState(net.minecraft.core.BlockPos.containing(candidate).below()).getFluidState().isSource()) {
                return candidate;
            }
            if (drake.level().getFluidState(net.minecraft.core.BlockPos.containing(candidate)).isSource()) {
                return candidate;
            }
        }
        return null;
    }

}
