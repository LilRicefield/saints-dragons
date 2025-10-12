package com.leon.saintsdragons.server.entity.interfaces;

import net.minecraft.world.entity.LivingEntity;

/**
 * Interface for dragons that can engage in combat.
 * Defines the minimum requirements for combat behaviors.
 */
public interface DragonCombatCapable {
    
    /**
     * Get the wyvern's melee attack range
     */
    double getMeleeRange();
    
    /**
     * Get the wyvern's melee attack damage
     */
    float getMeleeDamage();
    
    /**
     * Check if the wyvern can perform melee attacks
     */
    boolean canMeleeAttack();
    
    /**
     * Perform a melee attack on the target
     */
    void performMeleeAttack(LivingEntity target);
    
    /**
     * Get the wyvern's attack cooldown in ticks
     */
    int getAttackCooldown();
    
    /**
     * Check if the wyvern is currently attacking
     */
    boolean isAttacking();
    
    /**
     * Set the wyvern's attacking state
     */
    void setAttacking(boolean attacking);
}
