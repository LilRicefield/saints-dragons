package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.DragonCombatCapable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Base class for dragon melee attack goals.
 * Works with any dragon that implements DragonCombatCapable.
 */
public abstract class DragonMeleeAttackGoalBase extends Goal {
    protected final DragonEntity dragon;
    protected final DragonCombatCapable combatCapable;
    
    // Combat parameters - can be overridden by subclasses
    protected double attackRange = 2.0;
    protected double directChaseRange = 10.0;
    protected int pathCooldown = 15;
    protected int attackCooldown = 20;
    
    // Internal state
    private int pathCommitmentTicks = 0;
    private int lastPathTime = 0;
    private int attackTimer = 0;
    
    public DragonMeleeAttackGoalBase(DragonEntity dragon) {
        this.dragon = dragon;
        this.combatCapable = (DragonCombatCapable) dragon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        LivingEntity target = dragon.getTarget();
        if (target == null || !target.isAlive()) return false;
        
        // Use interface methods instead of hardcoded dragon type
        return combatCapable.canMeleeAttack() && 
               dragon.distanceToSqr(target) <= attackRange * attackRange;
    }
    
    @Override
    public boolean canContinueToUse() {
        LivingEntity target = dragon.getTarget();
        if (target == null || !target.isAlive()) return false;
        
        return combatCapable.canMeleeAttack() && 
               dragon.distanceToSqr(target) <= attackRange * attackRange * 4;
    }
    
    @Override
    public void start() {
        pathCommitmentTicks = 0;
        lastPathTime = dragon.tickCount;
        attackTimer = 0;
    }
    
    @Override
    public void stop() {
        pathCommitmentTicks = 0;
        combatCapable.setAttacking(false);
    }
    
    @Override
    public void tick() {
        LivingEntity target = dragon.getTarget();
        if (target == null) return;
        
        attackTimer++;
        
        // Handle pathfinding
        handlePathfinding(target);
        
        // Handle attack
        handleAttack(target);
    }
    
    protected void handlePathfinding(LivingEntity target) {
        PathNavigation navigation = dragon.getNavigation();
        double distSqr = dragon.distanceToSqr(target);
        
        // Direct chase for close targets
        if (distSqr <= directChaseRange * directChaseRange) {
            dragon.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
            dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
            return;
        }
        
        // Pathfinding for distant targets
        boolean needsNewPath = (dragon.tickCount - lastPathTime) >= pathCooldown;
        boolean pathDone = navigation.isDone();
        
        if (needsNewPath || (pathDone && pathCommitmentTicks > 20)) {
            Path path = navigation.createPath(target, 0);
            if (path != null) {
                navigation.moveTo(path, 1.0);
                lastPathTime = dragon.tickCount;
                pathCommitmentTicks = 0;
            }
        }
        
        if (!pathDone) {
            pathCommitmentTicks++;
        }
    }
    
    protected void handleAttack(LivingEntity target) {
        double distSqr = dragon.distanceToSqr(target);
        
        if (distSqr <= attackRange * attackRange && attackTimer >= attackCooldown) {
            // Perform attack using interface method
            combatCapable.performMeleeAttack(target);
            attackTimer = 0;
            combatCapable.setAttacking(true);
            
            // Call subclass-specific attack logic
            onAttackPerformed(target);
        }
    }
    
    /**
     * Called when an attack is performed. Override for dragon-specific behavior.
     */
    protected abstract void onAttackPerformed(LivingEntity target);
    
    /**
     * Configure combat parameters. Override in subclasses for different dragon types.
     */
    protected void configureCombatParameters() {
        // Default parameters - can be overridden
        this.attackRange = combatCapable.getMeleeRange();
        this.attackCooldown = combatCapable.getAttackCooldown();
    }
}
