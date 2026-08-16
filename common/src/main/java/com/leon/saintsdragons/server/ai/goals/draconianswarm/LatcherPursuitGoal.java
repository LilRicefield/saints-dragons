package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.Latcher;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class LatcherPursuitGoal extends Goal {
    private static final int PATH_REFRESH_TICKS = 8;

    private final Latcher latcher;
    private int waypointTicks;

    public LatcherPursuitGoal(Latcher latcher) {
        this.latcher = latcher;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.latcher.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.waypointTicks = 0;
        this.latcher.setCombatAttackWindow(true);
    }

    @Override
    public void tick() {
        LivingEntity target = this.latcher.getTarget();
        if (target == null) {
            return;
        }

        this.latcher.setCombatAttackWindow(true);
        this.latcher.getLookControl().setLookAt(target, 100.0F, 100.0F);
        if (this.latcher.isInBiteRange(target)
                && this.latcher.getSwarmFlightController().isIdle()) {
            this.waypointTicks = 0;
            return;
        }

        if (--this.waypointTicks > 0 && !this.latcher.getSwarmFlightController().isIdle()) {
            return;
        }

        Vec3 velocity = target.getDeltaMovement();
        Vec3 targetPoint = target.position().add(
                velocity.x * 2.0D,
                target.getBbHeight() * 0.5D + velocity.y,
                velocity.z * 2.0D
        );
        this.latcher.getSwarmFlightController().setWaypoint(targetPoint, this.latcher.getChaseSpeed());
        this.waypointTicks = PATH_REFRESH_TICKS;
    }

    @Override
    public void stop() {
        this.latcher.setCombatAttackWindow(false);
        this.latcher.releaseCombatAttack();
        this.latcher.getSwarmFlightController().clearWaypoint();
        this.waypointTicks = 0;
    }
}
