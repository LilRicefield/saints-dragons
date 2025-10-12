package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.common.registry.raevyx.RaevyxAbilities;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

import static com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxConstantsHandler.*;

/**
 * Handles clean state transitions between attack phases.
 */
public class RaevyxStateGoal extends Goal {
    protected final Raevyx wyvern;
    private final int getAttackState;
    private final int attackState;
    protected final int attackEndState;
    private final int attackFinalTick;
    protected final int attackSeeTick;

    public RaevyxStateGoal(Raevyx wyvern, int getAttackState, int attackState, int attackEndState, int attackFinalTick, int attackSeeTick) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.noneOf(Flag.class));
        this.getAttackState = getAttackState;
        this.attackState = attackState;
        this.attackEndState = attackEndState;
        this.attackFinalTick = attackFinalTick;
        this.attackSeeTick = attackSeeTick;
    }

    public RaevyxStateGoal(Raevyx wyvern, int getAttackState, int attackState, int attackEndState, int attackFinalTick, int attackSeeTick, boolean interruptsAI) {
        this.wyvern = wyvern;
        this.setFlags(interruptsAI ? EnumSet.of(Flag.LOOK) : EnumSet.noneOf(Flag.class));
        this.getAttackState = getAttackState;
        this.attackState = attackState;
        this.attackEndState = attackEndState;
        this.attackFinalTick = attackFinalTick;
        this.attackSeeTick = attackSeeTick;
    }

    public RaevyxStateGoal(Raevyx wyvern, int getAttackState, int attackState, int attackEndState, int attackFinalTick, int attackSeeTick, EnumSet<Flag> interruptFlagTypes) {
        this.wyvern = wyvern;
        setFlags(interruptFlagTypes.isEmpty() ? EnumSet.noneOf(Flag.class) : interruptFlagTypes);
        this.getAttackState = getAttackState;
        this.attackState = attackState;
        this.attackEndState = attackEndState;
        this.attackFinalTick = attackFinalTick;
        this.attackSeeTick = attackSeeTick;
    }

    @Override
    public boolean canUse() {
        return this.wyvern.getAttackState() == getAttackState;
    }

    @Override
    public void start() {
        if (getAttackState != attackState) {
            this.wyvern.setAttackState(attackState);
        }
    }

    @Override
    public void stop() {
        this.wyvern.setAttackState(attackEndState);
    }

    @Override
    public boolean canContinueToUse() {
        return attackFinalTick > 0 ? 
               this.wyvern.attackTicks <= attackFinalTick && this.wyvern.getAttackState() == attackState :
               canUse();
    }

    @Override
    public void tick() {
        LivingEntity target = wyvern.getTarget();
        
        // Handle looking at target during windup
        if (wyvern.attackTicks < attackSeeTick && target != null) {
            wyvern.getLookControl().setLookAt(target, 30.0F, 30.0F);
            wyvern.lookAt(target, 30.0F, 30.0F);
        } else {
            // Freeze rotation during attack execution
            wyvern.setYRot(wyvern.yRotO);
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
                if (wyvern.attackTicks >= 3) { // Windup complete
                    wyvern.setAttackState(ATTACK_STATE_HORN_ACTIVE);
                }
                break;
            case ATTACK_STATE_HORN_ACTIVE:
                if (wyvern.attackTicks >= 3) { // Active phase complete
                    wyvern.tryActivateAbility(RaevyxAbilities.RAEVYX_HORN_GORE);
                    wyvern.setAttackState(ATTACK_STATE_RECOVERY);
                    wyvern.attackCooldown = 1; // Set cooldown
                }
                break;
            case ATTACK_STATE_BITE_WINDUP:
                if (wyvern.attackTicks >= 3) { // Windup complete
                    wyvern.setAttackState(ATTACK_STATE_BITE_ACTIVE);
                }
                break;
            case ATTACK_STATE_BITE_ACTIVE:
                if (wyvern.attackTicks >= 3) { // Active phase complete
                    wyvern.tryActivateAbility(RaevyxAbilities.RAEVYX_BITE);
                    wyvern.setAttackState(ATTACK_STATE_RECOVERY);
                    wyvern.attackCooldown = 1; // Set cooldown
                }
                break;
            case ATTACK_STATE_RECOVERY:
                if (wyvern.attackTicks >= 3) { // Recovery complete
                    wyvern.setAttackState(ATTACK_STATE_IDLE);
                }
                break;
        }
    }
}
