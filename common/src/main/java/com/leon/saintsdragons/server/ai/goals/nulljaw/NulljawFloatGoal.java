package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.server.ai.goals.base.DragonBaseGoal;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class NulljawFloatGoal extends DragonBaseGoal<Nulljaw> {
    private static final double ARRIVAL_DISTANCE_SQR = 4.0D;
    private static final int MIN_TRAVEL_TICKS = 50;
    private static final int MAX_TRAVEL_TICKS = 100;
    private static final double TRAVEL_SPEED = 1.0D;

    private Vec3 target;
    private int travelTicks;
    private int cooldownTicks;

    public NulljawFloatGoal(Nulljaw dragon) {
        super(dragon);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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
        return findTarget() != null;
    }

    @Override
    public void start() {
        this.target = findTarget();
        this.travelTicks = MIN_TRAVEL_TICKS + dragon.getRandom().nextInt(MAX_TRAVEL_TICKS - MIN_TRAVEL_TICKS + 1);
        if (this.target == null) {
            this.cooldownTicks = 20;
            return;
        }
        dragon.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, TRAVEL_SPEED);
    }

    @Override
    protected boolean canContinueAdditional() {
        return this.target != null && this.travelTicks > 0 && !dragon.getNavigation().isDone();
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }

        this.travelTicks--;

        if (dragon.distanceToSqr(this.target) <= ARRIVAL_DISTANCE_SQR) {
            this.travelTicks = 0;
            dragon.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        dragon.getNavigation().stop();
        this.target = null;
        this.cooldownTicks = 25 + dragon.getRandom().nextInt(35);
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
