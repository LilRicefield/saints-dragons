package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.AbstractDraconianSwarmEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class DraconianSwarmFloatWanderGoal extends Goal {
    private final AbstractDraconianSwarmEntity swarm;
    private final double speed;

    public DraconianSwarmFloatWanderGoal(AbstractDraconianSwarmEntity swarm, double speed) {
        this.swarm = swarm;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.swarm.getTarget() == null
                && this.swarm.getSwarmFlightController().isIdle()
                && this.swarm.getRandom().nextInt(32) == 0;
    }

    @Override
    public void start() {
        Vec3 target = randomAirPosition();
        if (target != null) {
            this.swarm.getSwarmFlightController().setWaypoint(target, this.speed);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.swarm.getTarget() == null && !this.swarm.getSwarmFlightController().isIdle();
    }

    @Nullable
    private Vec3 randomAirPosition() {
        Vec3 view = this.swarm.getViewVector(0.0F);
        Vec3 target = HoverRandomPos.getPos(this.swarm, 16, 8, view.x, view.z, (float) Math.PI / 2.0F, 6, 4);
        if (target == null) {
            Vec3 forward = this.swarm.position().add(view);
            target = AirAndWaterRandomPos.getPos(this.swarm, 16, 6, -4, forward.x, forward.z, (float) Math.PI / 2.0F);
        }
        if (target == null || !this.swarm.level().noCollision(this.swarm.getBoundingBox().move(target.subtract(this.swarm.position())))) {
            return null;
        }
        return target;
    }
}
