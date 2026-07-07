package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.Latcher;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class LatcherPursuitGoal extends Goal {
    private final Latcher latcher;

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
        Vec3 targetPoint = target.getBoundingBox().getCenter().add(target.getDeltaMovement().scale(2.0D));
        this.latcher.getSwarmFlightController().setDirectWaypoint(targetPoint, this.latcher.getChaseSpeed());
    }

    @Override
    public void stop() {
        this.latcher.setCombatAttackWindow(false);
        this.latcher.releaseCombatAttack();
        this.latcher.getSwarmFlightController().clearWaypoint();
    }
}
