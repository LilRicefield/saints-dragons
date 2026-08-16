package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.Latcher;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class LatcherBiteGoal extends Goal {
    private static final int DAMAGE_TICK = 2;
    private static final int ATTACK_INTERVAL = 12;

    private final Latcher latcher;
    private int attackTick = -1;
    private int cooldown;

    public LatcherBiteGoal(Latcher latcher) {
        this.latcher = latcher;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.latcher.getTarget();
        return target != null
                && target.isAlive()
                && this.latcher.canStartCombatAttack()
                && this.latcher.isInBiteRange(target)
                && this.latcher.tryClaimCombatAttack();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        this.latcher.releaseCombatAttack();
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
            if (this.attackTick == DAMAGE_TICK && this.latcher.isInBiteRange(target)) {
                this.latcher.doHurtTarget(target);
            }
            if (this.attackTick >= ATTACK_INTERVAL) {
                this.attackTick = -1;
            }
            return;
        }

        if (this.cooldown == 0 && this.latcher.isInBiteRange(target)) {
            this.attackTick = 0;
            this.cooldown = ATTACK_INTERVAL;
            this.latcher.performBiteAnimation(this.latcher.isMovingForAnimation());
        }
    }
}
