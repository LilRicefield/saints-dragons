package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.Latcher;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class LatcherBiteGoal extends Goal {
    private static final int DAMAGE_TICK = 2;
    private static final int ATTACK_INTERVAL = 12;
    private static final double EXTRA_REACH = 1.25D;

    private final Latcher latcher;
    private int attackTick = -1;
    private int cooldown;

    public LatcherBiteGoal(Latcher latcher) {
        this.latcher = latcher;
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
    public void stop() {
        this.attackTick = -1;
        this.cooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.latcher.getTarget();
        if (target == null) {
            return;
        }

        if (this.cooldown > 0) {
            this.cooldown--;
        }

        if (this.attackTick >= 0) {
            this.attackTick++;
            if (this.attackTick == DAMAGE_TICK && isInBiteRange(target)) {
                this.latcher.doHurtTarget(target);
            }
            if (this.attackTick >= ATTACK_INTERVAL) {
                this.attackTick = -1;
            }
            return;
        }

        if (this.cooldown == 0 && isInBiteRange(target)) {
            this.attackTick = 0;
            this.cooldown = ATTACK_INTERVAL;
            this.latcher.performBiteAnimation(this.latcher.isMovingForAnimation());
        }
    }

    private boolean isInBiteRange(LivingEntity target) {
        double reach = this.latcher.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D + EXTRA_REACH;
        return this.latcher.distanceToSqr(target) <= reach * reach;
    }
}
