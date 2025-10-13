package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.common.registry.nulljaw.NulljawAbilities;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Handles Rift Drake pursuit behaviour while combat goal decides which ability to use.
 * Keeps navigation running smoothly and toggles sprinting while chasing.
 */
public class NulljawMoveGoal extends Goal {
    private final Nulljaw drake;
    private final boolean followWhenUnseen;
    private final double moveSpeed;
    private int pathDelay;

    public NulljawMoveGoal(Nulljaw drake, boolean followWhenUnseen, double moveSpeed) {
        this.drake = drake;
        this.followWhenUnseen = followWhenUnseen;
        this.moveSpeed = moveSpeed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = drake.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (isPerformingAttack()) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        this.pathDelay = 0;
        this.drake.setAggressive(true);
        this.drake.setAccelerating(true);
    }

    @Override
    public void stop() {
        drake.getNavigation().stop();
        LivingEntity target = drake.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            drake.setTarget(null);
        }
        this.drake.setAggressive(false);
        this.drake.setAccelerating(false);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = drake.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (isPerformingAttack()) {
            return false;
        }
        if (!followWhenUnseen && drake.getNavigation().isDone()) {
            return false;
        }
        if (!drake.isWithinRestriction(target.blockPosition())) {
            return false;
        }
        return !(target instanceof Player player) || (!player.isCreative() && !player.isSpectator());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = drake.getTarget();
        if (target == null || isPerformingAttack()) {
            return;
        }

        drake.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (--pathDelay <= 0) {
            double distance = Math.sqrt(drake.distanceToSqr(target.getX(), target.getY(), target.getZ()));
            pathDelay = Mth.clamp((int) (distance * 0.45D), 4, 12);

            boolean moved = drake.getNavigation().moveTo(target, moveSpeed);
            if (!moved) {
                pathDelay += 5;
            }
        }
    }

    private boolean isPerformingAttack() {
        return drake.getActiveAbility() != null
                || drake.combatManager.isAbilityActive(NulljawAbilities.NULLJAW_CLAW);
    }
}
