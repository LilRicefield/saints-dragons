package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.Winged;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class WingedPullAttackGoal extends Goal {
    private static final int IMPACT_TICK = 5;
    private static final int ATTACK_INTERVAL = 16;
    private static final double EXTRA_REACH = 1.75D;
    private static final double PULL_STRENGTH = 0.85D;

    private final Winged winged;
    private int attackTick = -1;
    private int cooldown;

    public WingedPullAttackGoal(Winged winged) {
        this.winged = winged;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.winged.getTarget();
        return this.winged.canStartCombatAttack()
                && !this.winged.isSwooping()
                && !this.winged.isDiveBombReady()
                && target != null
                && target.isAlive()
                && isInAttackRange(target)
                && this.winged.tryClaimCombatAttack();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.winged.getTarget();
        return this.attackTick >= 0
                ? !this.winged.isSwooping() && target != null && target.isAlive()
                : canUse();
    }

    @Override
    public void stop() {
        this.winged.releaseCombatAttack();
        this.attackTick = -1;
        this.cooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.winged.getTarget();
        if (target == null) {
            return;
        }

        if (this.cooldown > 0) {
            this.cooldown--;
        }

        if (this.attackTick >= 0) {
            this.attackTick++;
            if (this.attackTick == IMPACT_TICK && isInAttackRange(target)) {
                if (this.winged.doHurtTarget(target)) {
                    this.winged.recordPullAttackHit();
                    pullTarget(target);
                    this.winged.requestCombatRetreat();
                }
            }
            if (this.attackTick >= ATTACK_INTERVAL) {
                this.attackTick = -1;
            }
            return;
        }

        if (this.cooldown == 0 && isInAttackRange(target)) {
            this.attackTick = 0;
            this.cooldown = ATTACK_INTERVAL;
            this.winged.performPullAttackAnimation();
        }
    }

    private void pullTarget(LivingEntity target) {
        Vec3 pull = this.winged.getBoundingBox().getCenter()
                .subtract(target.getBoundingBox().getCenter());
        if (pull.lengthSqr() < 1.0E-4D) {
            return;
        }

        Vec3 current = target.getDeltaMovement().scale(0.25D);
        target.setDeltaMovement(current.add(pull.normalize().scale(PULL_STRENGTH)));
        target.hurtMarked = true;
    }

    private boolean isInAttackRange(LivingEntity target) {
        double reach = this.winged.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D + EXTRA_REACH;
        return this.winged.distanceToSqr(target) <= reach * reach;
    }
}
