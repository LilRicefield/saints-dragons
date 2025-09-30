package com.leon.saintsdragons.server.ai.goals.lightningdragon;

import com.leon.saintsdragons.common.registry.lightningdragon.LightningDragonAbilities;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

import static com.leon.saintsdragons.server.entity.dragons.lightningdragon.handlers.LightningDragonConstantsHandler.*;

/**
 * Handles clean state transitions between attack phases.
 */
public class LightningDragonStateGoal extends Goal {
    protected final LightningDragonEntity dragon;
    private final int getAttackState;
    private final int attackState;
    protected final int attackEndState;
    private final int attackFinalTick;
    protected final int attackSeeTick;

    public LightningDragonStateGoal(LightningDragonEntity dragon, int getAttackState, int attackState, int attackEndState, int attackFinalTick, int attackSeeTick) {
        this.dragon = dragon;
        this.setFlags(EnumSet.noneOf(Flag.class));
        this.getAttackState = getAttackState;
        this.attackState = attackState;
        this.attackEndState = attackEndState;
        this.attackFinalTick = attackFinalTick;
        this.attackSeeTick = attackSeeTick;
    }

    public LightningDragonStateGoal(LightningDragonEntity dragon, int getAttackState, int attackState, int attackEndState, int attackFinalTick, int attackSeeTick, boolean interruptsAI) {
        this.dragon = dragon;
        this.setFlags(interruptsAI ? EnumSet.of(Flag.LOOK) : EnumSet.noneOf(Flag.class));
        this.getAttackState = getAttackState;
        this.attackState = attackState;
        this.attackEndState = attackEndState;
        this.attackFinalTick = attackFinalTick;
        this.attackSeeTick = attackSeeTick;
    }

    public LightningDragonStateGoal(LightningDragonEntity dragon, int getAttackState, int attackState, int attackEndState, int attackFinalTick, int attackSeeTick, EnumSet<Flag> interruptFlagTypes) {
        this.dragon = dragon;
        setFlags(interruptFlagTypes.isEmpty() ? EnumSet.noneOf(Flag.class) : interruptFlagTypes);
        this.getAttackState = getAttackState;
        this.attackState = attackState;
        this.attackEndState = attackEndState;
        this.attackFinalTick = attackFinalTick;
        this.attackSeeTick = attackSeeTick;
    }

    @Override
    public boolean canUse() {
        return this.dragon.getAttackState() == getAttackState;
    }

    @Override
    public void start() {
        if (getAttackState != attackState) {
            this.dragon.setAttackState(attackState);
        }
    }

    @Override
    public void stop() {
        this.dragon.setAttackState(attackEndState);
    }

    @Override
    public boolean canContinueToUse() {
        return attackFinalTick > 0 ? 
               this.dragon.attackTicks <= attackFinalTick && this.dragon.getAttackState() == attackState : 
               canUse();
    }

    @Override
    public void tick() {
        LivingEntity target = dragon.getTarget();
        
        // Handle looking at target during windup
        if (dragon.attackTicks < attackSeeTick && target != null) {
            dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
            dragon.lookAt(target, 30.0F, 30.0F);
        } else {
            // Freeze rotation during attack execution
            dragon.setYRot(dragon.yRotO);
        }
        
        // Handle state transitions
        handleStateTransition();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Handle automatic state transitions based on timing
     */
    private void handleStateTransition() {
        switch (attackState) {
            case ATTACK_STATE_HORN_WINDUP:
                if (dragon.attackTicks >= 3) { // Windup complete
                    dragon.setAttackState(ATTACK_STATE_HORN_ACTIVE);
                }
                break;
            case ATTACK_STATE_HORN_ACTIVE:
                if (dragon.attackTicks >= 3) { // Active phase complete
                    dragon.tryActivateAbility(LightningDragonAbilities.HORN_GORE);
                    dragon.setAttackState(ATTACK_STATE_RECOVERY);
                    dragon.attackCooldown = 4; // Set cooldown
                }
                break;
            case ATTACK_STATE_BITE_WINDUP:
                if (dragon.attackTicks >= 3) { // Windup complete
                    dragon.setAttackState(ATTACK_STATE_BITE_ACTIVE);
                }
                break;
            case ATTACK_STATE_BITE_ACTIVE:
                if (dragon.attackTicks >= 3) { // Active phase complete
                    dragon.tryActivateAbility(LightningDragonAbilities.BITE);
                    dragon.setAttackState(ATTACK_STATE_RECOVERY);
                    dragon.attackCooldown = 4; // Set cooldown
                }
                break;
            case ATTACK_STATE_RECOVERY:
                if (dragon.attackTicks >= 3) { // Recovery complete
                    dragon.setAttackState(ATTACK_STATE_IDLE);
                }
                break;
        }
    }
}
