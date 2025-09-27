package com.leon.saintsdragons.server.ai.goals.lightningdragon;

import com.leon.saintsdragons.common.registry.lightningdragon.LightningDragonAbilities;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

import static com.leon.saintsdragons.server.entity.dragons.lightningdragon.handlers.LightningDragonConstantsHandler.*;

/**
 * Pure attack goal for Lightning Dragon based on Cataclysm's InternalAttackGoal.
 * Handles ONLY attack execution - movement is completely stopped during attacks.
 */
public class LightningDragonAttackGoal extends Goal {
    protected final LightningDragonEntity dragon;
    protected final int getAttackState;
    protected final int attackState;
    protected final int attackMaxTick;
    protected final int attackSeeTick;
    protected final float attackRange;

    public LightningDragonAttackGoal(LightningDragonEntity dragon, int getAttackState, int attackState, int attackMaxTick, int attackSeeTick, float attackRange) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.getAttackState = getAttackState;
        this.attackState = attackState;
        this.attackMaxTick = attackMaxTick;
        this.attackSeeTick = attackSeeTick;
        this.attackRange = attackRange;
    }

    public LightningDragonAttackGoal(LightningDragonEntity dragon, int getAttackState, int attackState, int attackMaxTick, int attackSeeTick, float attackRange, EnumSet<Flag> interruptFlagTypes) {
        this.dragon = dragon;
        setFlags(interruptFlagTypes);
        this.getAttackState = getAttackState;
        this.attackState = attackState;
        this.attackMaxTick = attackMaxTick;
        this.attackSeeTick = attackSeeTick;
        this.attackRange = attackRange;
    }

    @Override
    public boolean canUse() {
        if (this.dragon.getAttackState() != getAttackState) {
            return false;
        }

        LivingEntity target = dragon.getTarget();
        if (target != null && target.isAlive()) {
            double distanceSq = this.dragon.distanceToSqr(target);
            return distanceSq <= getAttackReachSqr(target);
        }

        // Allow phase-two states even without an active target
        return attackState == ATTACK_STATE_SUMMON_STORM_WINDUP || attackState == ATTACK_STATE_SUMMON_STORM_ACTIVE || attackState == ATTACK_STATE_SUMMON_STORM_RECOVERY;
    }

    @Override
    public void start() {
        this.dragon.setAttackState(attackState);
        this.dragon.getNavigation().stop(); // KEY: Stop movement during attack
    }

    @Override
    public void stop() {
        LivingEntity target = dragon.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.dragon.setTarget((LivingEntity)null);
        }
        this.dragon.getNavigation().stop();
        if (this.dragon.getTarget() == null && attackState < ATTACK_STATE_SUMMON_STORM_WINDUP) {
            this.dragon.setAggressive(false);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.dragon.getAttackState() == attackState && this.dragon.attackTicks <= attackMaxTick;
    }

    @Override
    public void tick() {
        LivingEntity target = dragon.getTarget();
        
        // Only look at target during windup phase
        if (dragon.attackTicks < attackSeeTick && target != null) {
            dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
            dragon.lookAt(target, 30.0F, 30.0F);
        } else {
            // Freeze rotation during attack execution
            dragon.setYRot(dragon.yRotO);
        }
        
        // Keep navigation stopped during attack
        this.dragon.getNavigation().stop();
        
        // Execute attack logic based on attack type
        executeAttack(target);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }


    private double getAttackReachSqr(LivingEntity target) {
        double combinedRadii = (this.dragon.getBbWidth() + target.getBbWidth()) * 0.5;
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
                if (dragon.attackTicks == 1) { // Execute once at start of active phase
                    dragon.tryActivateAbility(LightningDragonAbilities.HORN_GORE);
                }
                break;
            case ATTACK_STATE_BITE_ACTIVE:
                if (dragon.attackTicks == 1) { // Execute once at start of active phase
                    dragon.tryActivateAbility(LightningDragonAbilities.BITE);
                }
                break;
            case ATTACK_STATE_SUMMON_STORM_ACTIVE:
                if (dragon.attackTicks == 1) {
                    dragon.tryActivateAbility(LightningDragonAbilities.SUMMON_STORM);
                }
                break;
        }
    }
}
