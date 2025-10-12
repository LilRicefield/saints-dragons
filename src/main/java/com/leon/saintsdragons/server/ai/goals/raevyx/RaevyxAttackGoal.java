package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.common.registry.raevyx.RaevyxAbilities;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

import static com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxConstantsHandler.*;

/**
 * Pure attack goal for Lightning Dragon.
 * Handles ONLY attack execution - movement is completely stopped during attacks.
 */
public class RaevyxAttackGoal extends Goal {
    protected final Raevyx wyvern;
    protected final int getAttackState;
    protected final int attackState;
    protected final int attackMaxTick;
    protected final int attackSeeTick;
    protected final float attackRange;

    public RaevyxAttackGoal(Raevyx wyvern, int getAttackState, int attackState, int attackMaxTick, int attackSeeTick, float attackRange) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.getAttackState = getAttackState;
        this.attackState = attackState;
        this.attackMaxTick = attackMaxTick;
        this.attackSeeTick = attackSeeTick;
        this.attackRange = attackRange;
    }

    public RaevyxAttackGoal(Raevyx wyvern, int getAttackState, int attackState, int attackMaxTick, int attackSeeTick, float attackRange, EnumSet<Flag> interruptFlagTypes) {
        this.wyvern = wyvern;
        setFlags(interruptFlagTypes);
        this.getAttackState = getAttackState;
        this.attackState = attackState;
        this.attackMaxTick = attackMaxTick;
        this.attackSeeTick = attackSeeTick;
        this.attackRange = attackRange;
    }

    @Override
    public boolean canUse() {
        if (this.wyvern.getAttackState() != getAttackState) {
            return false;
        }

        LivingEntity target = wyvern.getTarget();
        if (target != null && target.isAlive()) {
            double distanceSq = this.wyvern.distanceToSqr(target);
            return distanceSq <= getAttackReachSqr(target);
        }

        return false;
    }

    @Override
    public void start() {
        this.wyvern.setAttackState(attackState);
        this.wyvern.getNavigation().stop(); // KEY: Stop movement during attack
    }

    @Override
    public void stop() {
        LivingEntity target = wyvern.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.wyvern.setTarget((LivingEntity)null);
        }
        this.wyvern.getNavigation().stop();
        if (this.wyvern.getTarget() == null) {
            this.wyvern.setAggressive(false);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.wyvern.getAttackState() == attackState && this.wyvern.attackTicks <= attackMaxTick;
    }

    @Override
    public void tick() {
        LivingEntity target = wyvern.getTarget();
        
        // Only look at target during windup phase
        if (wyvern.attackTicks < attackSeeTick && target != null) {
            wyvern.getLookControl().setLookAt(target, 30.0F, 30.0F);
            wyvern.lookAt(target, 30.0F, 30.0F);
        } else {
            // Freeze rotation during attack execution
            wyvern.setYRot(wyvern.yRotO);
        }
        
        // Keep navigation stopped during attack
        this.wyvern.getNavigation().stop();
        
        // Execute attack logic based on attack type
        executeAttack(target);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }


    private double getAttackReachSqr(LivingEntity target) {
        double combinedRadii = (this.wyvern.getBbWidth() + target.getBbWidth()) * 0.5;
        double reach = this.attackRange + combinedRadii;
        return reach * reach;
    }

    /**
     * Execute the specific attack based on the attack state
     */

    private void executeAttack(LivingEntity target) {
        if (target == null) return;
        
        // Trigger abilities based on attack state
        switch (attackState) {
            case ATTACK_STATE_HORN_ACTIVE:
                if (wyvern.attackTicks == 1) { // Execute once at start of active phase
                    wyvern.tryActivateAbility(RaevyxAbilities.RAEVYX_HORN_GORE);
                }
                break;
            case ATTACK_STATE_BITE_ACTIVE:
                if (wyvern.attackTicks == 1) { // Execute once at start of active phase
                    wyvern.tryActivateAbility(RaevyxAbilities.RAEVYX_BITE);
                }
                break;
        }
    }
}
