package com.leon.saintsdragons.server.ai.goals.lightningdragon;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

import static com.leon.saintsdragons.server.entity.dragons.lightningdragon.handlers.LightningDragonConstantsHandler.*;

/**
 * Main combat coordination goal for Lightning Dragon.
 * Decides which attack to use and initiates the appropriate attack state.
 */
public class LightningDragonCombatGoal extends Goal {
    protected final LightningDragonEntity dragon;
    private final double attackRange = 4.0; // Reduced from complex FSM ranges
    
    public LightningDragonCombatGoal(LightningDragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = dragon.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        
        // Only use when not already attacking and not on cooldown
        if (dragon.isInAttackState() || !dragon.canAttack()) {
            return false;
        }
        
        // Check if we're in range and have line of sight
        double distanceSq = dragon.distanceToSqr(target);
        if (distanceSq > getAttackReachSqr(target) || !dragon.getSensing().hasLineOfSight(target)) {
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
        LivingEntity target = dragon.getTarget();
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
            dragon.setAttackState(ATTACK_STATE_BITE_WINDUP);
            dragon.attackCooldown = 40; // 2 second cooldown
            dragon.attackCooldown = 40; // 2 second cooldown
        } else if (gap <= 4.0) {
            // Medium range - use horn gore attack
            dragon.setAttackState(ATTACK_STATE_HORN_WINDUP);
            dragon.attackCooldown = 40; // 2 second cooldown
        }
        // If too far, don't attack (let movement goal handle getting closer)
    }

    private double getAttackReachSqr(LivingEntity target) {
        double combinedRadii = (this.dragon.getBbWidth() + target.getBbWidth()) * 0.5;
        double reach = this.attackRange + combinedRadii;
        return reach * reach;
    }

    private double getGapToTarget(LivingEntity target) {
        double centerDistance = this.dragon.distanceTo(target);
        double combinedRadii = (this.dragon.getBbWidth() + target.getBbWidth()) * 0.5;
        return Math.max(0.0, centerDistance - combinedRadii);
    }
}
