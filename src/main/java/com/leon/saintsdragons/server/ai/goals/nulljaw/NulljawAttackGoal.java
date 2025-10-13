package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.common.registry.nulljaw.NulljawAbilities;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Locks the Rift Drake in place while its melee abilities are running,
 * ensuring clean hit windows and preventing navigation jitter.
 */
public class NulljawAttackGoal extends Goal {
    private final Nulljaw drake;

    public NulljawAttackGoal(Nulljaw drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return isPerformingAttack();
    }

    @Override
    public boolean canContinueToUse() {
        return isPerformingAttack();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        drake.getNavigation().stop();
    }

    @Override
    public void stop() {
    }

    @Override
    public void tick() {
        drake.getNavigation().stop();

        LivingEntity target = drake.getTarget();
        if (target != null && target.isAlive()) {
            drake.getLookControl().setLookAt(target, 40.0F, 40.0F);
        }
    }

    private boolean isPerformingAttack() {
        return drake.getActiveAbility() != null
                || drake.combatManager.isAbilityActive(NulljawAbilities.NULLJAW_CLAW);
    }
}
