package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.server.ai.goals.base.DragonBaseGoal;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class NulljawFloatGoal extends DragonBaseGoal<Nulljaw> {
    private int cooldownTicks;

    public NulljawFloatGoal(Nulljaw dragon) {
        super(dragon);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    protected boolean canUseAdditional() {
        if (dragon.isTame() && dragon.getCommand() == 1) {
            return false;
        }
        if (dragon.getOwner() != null && dragon.distanceToSqr(dragon.getOwner()) > 64.0D && dragon.getCommand() == 0) {
            return false;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }
        return dragon.getAsyncAirController().isIdle();
    }

    @Override
    public void start() {
        Vec3 target = findTarget();
        if (target != null) {
            dragon.getAsyncAirController().setWaypoint(target, 1.0D);
            cooldownTicks = 50 + dragon.getRandom().nextInt(50);
        } else {
            cooldownTicks = 20;
        }
    }

    @Override
    protected boolean canContinueAdditional() {
        return false;
    }

    private Vec3 findTarget() {
        for (int attempt = 0; attempt < 12; attempt++) {
            double x = dragon.getX() + (dragon.getRandom().nextDouble() - 0.5D) * 24.0D;
            double y = dragon.getY() + (dragon.getRandom().nextDouble() - 0.5D) * 10.0D;
            double z = dragon.getZ() + (dragon.getRandom().nextDouble() - 0.5D) * 24.0D;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (!dragon.level().hasChunkAt(pos)) {
                continue;
            }
            if (!dragon.level().getBlockState(pos).getCollisionShape(dragon.level(), pos).isEmpty()) {
                continue;
            }
            if (!dragon.level().getFluidState(pos).isEmpty()) {
                continue;
            }
            return new Vec3(x, y, z);
        }
        return null;
    }
}
