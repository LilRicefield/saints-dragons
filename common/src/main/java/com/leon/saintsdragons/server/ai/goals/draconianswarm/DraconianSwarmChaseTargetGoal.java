package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.AbstractDraconianSwarmEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class DraconianSwarmChaseTargetGoal extends Goal {
    private final AbstractDraconianSwarmEntity swarm;
    private final double speed;

    public DraconianSwarmChaseTargetGoal(AbstractDraconianSwarmEntity swarm, double speed) {
        this.swarm = swarm;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.swarm.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        this.swarm.getSwarmFlightController().clearWaypoint();
    }

    @Override
    public void tick() {
        LivingEntity target = this.swarm.getTarget();
        if (target == null) {
            return;
        }

        this.swarm.getLookControl().setLookAt(target, 30.0F, 30.0F);
        Vec3 targetVelocity = target.getDeltaMovement();
        Vec3 targetPos = target.position().add(
                targetVelocity.x * 3.0D,
                target.getBbHeight() * 0.55D + targetVelocity.y,
                targetVelocity.z * 3.0D);
        this.swarm.getSwarmFlightController().setDirectWaypoint(targetPos, this.speed);
    }
}
