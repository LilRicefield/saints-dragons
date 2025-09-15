package com.leon.saintsdragons.server.entity.interfaces;

import net.minecraft.world.entity.LivingEntity;

/**
 * Interface for dragons that can engage in combat.
 * Defines the minimum requirements for combat behaviors.
 */
public interface DragonCombatCapable {
    
    /**
     * Get the dragon's melee attack range
     */
    double getMeleeRange();
    
    /**
     * Get the dragon's melee attack damage
     */
    float getMeleeDamage();
    
    /**
     * Check if the dragon can perform melee attacks
     */
    boolean canMeleeAttack();
    
    /**
     * Perform a melee attack on the target
     */
    void performMeleeAttack(LivingEntity target);
    
    /**
     * Get the dragon's attack cooldown in ticks
     */
    int getAttackCooldown();
    
    /**
     * Check if the dragon is currently attacking
     */
    boolean isAttacking();
    
    /**
     * Set the dragon's attacking state
     */
    void setAttacking(boolean attacking);
}
