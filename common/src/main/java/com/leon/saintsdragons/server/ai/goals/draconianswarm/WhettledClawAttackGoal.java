package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.AbstractDraconianSwarmEntity;
import com.leon.saintsdragons.server.entity.draconianswarm.Whettled;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class WhettledClawAttackGoal extends Goal {
    private static final int IMPACT_TICK = 5;
    private static final int ATTACK_INTERVAL = 18;
    private static final double EXTRA_REACH = 2.5D;

    private final Whettled whettled;
    private int attackTick = -1;
    private int cooldown;

    public WhettledClawAttackGoal(Whettled whettled) {
        this.whettled = whettled;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.whettled.getTarget();
        return this.whettled.canStartCombatAttack()
                && !this.whettled.isSwooping()
                && target != null
                && target.isAlive()
                && isInRange(target)
                && this.whettled.tryClaimCombatAttack();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        this.whettled.releaseCombatAttack();
        this.attackTick = -1;
        this.cooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.whettled.getTarget();
        if (target == null) {
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (this.attackTick >= 0) {
            this.attackTick++;
            if (this.attackTick == IMPACT_TICK) {
                List<LivingEntity> victims = this.whettled.level().getEntitiesOfClass(
                        LivingEntity.class,
                        this.whettled.getBoundingBox().inflate(1.25D),
                        entity -> entity.isAlive()
                                && entity != this.whettled
                                && !(entity instanceof AbstractDraconianSwarmEntity));
                boolean hit = false;
                for (LivingEntity victim : victims) {
                    hit |= this.whettled.doHurtTarget(victim);
                }
                if (hit) {
                    this.whettled.requestCombatRetreat();
                }
            }
            if (this.attackTick >= ATTACK_INTERVAL) {
                this.attackTick = -1;
            }
            return;
        }
        if (this.cooldown == 0 && isInRange(target)) {
            this.attackTick = 0;
            this.cooldown = ATTACK_INTERVAL;
            this.whettled.performClawAnimation();
        }
    }

    private boolean isInRange(LivingEntity target) {
        double reach = this.whettled.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D + EXTRA_REACH;
        return this.whettled.distanceToSqr(target) <= reach * reach;
    }
}
