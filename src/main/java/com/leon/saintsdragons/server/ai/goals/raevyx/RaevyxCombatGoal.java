package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

import static com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxConstantsHandler.*;

/**
 * Main combat coordination goal for Lightning Dragon.
 * Decides which attack to use and initiates the appropriate attack state.
 */
public class RaevyxCombatGoal extends Goal {
    protected final Raevyx wyvern;
    private final double attackRange = 4.0; // Reduced from complex FSM ranges
    
    public RaevyxCombatGoal(Raevyx wyvern) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = wyvern.getTarget();
        boolean hasTarget = target != null && target.isAlive();
        
        // Only use when not already attacking and not on cooldown
        if (wyvern.isInAttackState() || !wyvern.canAttack()) {
            return false;
        }

        if (!hasTarget) {
            return false;
        }
        
        // Check if we're in range and have line of sight
        double distanceSq = wyvern.distanceToSqr(target);
        if (distanceSq > getAttackReachSqr(target) || !wyvern.getSensing().hasLineOfSight(target)) {
            return false;
        }
        
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return false; // fire-and-forget: once we choose an attack, hand off to state/attack goals
    }

    @Override
    public void start() {
        LivingEntity target = wyvern.getTarget();
        if (target == null) return;
        
        // Choose attack based on distance and availability
        chooseAttack(target);
    }

    @Override
    public void stop() {
        // No-op: state/attack goals own cleanup once an attack starts
    }

    @Override
    public void tick() {
        // This goal just initiates attacks, the specific attack goals handle the execution
        // No need for complex tick logic here
    }

    /**
     * Choose which attack to use based on distance and cooldowns
     */
    private void chooseAttack(LivingEntity target) {
        double gap = getGapToTarget(target);
        
        // Simple attack selection logic
        if (gap <= 3.0) {
            // Close range - use bite attack
            wyvern.setAttackState(ATTACK_STATE_BITE_WINDUP);
            wyvern.attackCooldown = 3; // 2 second cooldown
        } else if (gap <= 5.0) {
            // Medium range - use horn gore attack
            wyvern.setAttackState(ATTACK_STATE_HORN_WINDUP);
            wyvern.attackCooldown = 3; // 2 second cooldown
        }
        // If too far, don't attack (let movement goal handle getting closer)
    }

    private double getAttackReachSqr(LivingEntity target) {
        double combinedRadii = (this.wyvern.getBbWidth() + target.getBbWidth()) * 0.5;
        double reach = this.attackRange + combinedRadii;
        return reach * reach;
    }

    private double getGapToTarget(LivingEntity target) {
        double centerDistance = this.wyvern.distanceTo(target);
        double combinedRadii = (this.wyvern.getBbWidth() + target.getBbWidth()) * 0.5;
        return Math.max(0.0, centerDistance - combinedRadii);
    }
}
