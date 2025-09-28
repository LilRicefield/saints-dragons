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
        if (drake.getRandom().nextInt(interval) != 0) return false;
        Vec3 target = findSwimTarget();
        if (target == null) return false;
        drake.getNavigation().moveTo(target.x, target.y, target.z, speedModifier);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return drake.isInWater() && !drake.getNavigation().isDone();
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
