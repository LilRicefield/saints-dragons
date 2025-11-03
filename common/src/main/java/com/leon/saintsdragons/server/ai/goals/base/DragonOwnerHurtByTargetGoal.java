package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;

/**
 * Custom OwnerHurtByTargetGoal that respects the wyvern's ally system.
 * Prevents targeting allies even when they hurt the owner.
 */
public class DragonOwnerHurtByTargetGoal extends OwnerHurtByTargetGoal {
    private final DragonEntity dragon;
    
    public DragonOwnerHurtByTargetGoal(DragonEntity dragon) {
        super(dragon);
        this.dragon = dragon;
    }
    
    @Override
    public boolean canUse() {
        if (!super.canUse()) {
            return false;
        }
        
        // Get the target that would be selected by the parent goal
        LivingEntity owner = dragon.getOwner();
        if (owner == null) {
            return false;
        }
        
        LivingEntity target = owner.getLastHurtByMob();
        if (target == null) {
            return false;
        }
        
        // Use wyvern's canTarget method to respect ally system
        return dragon.canTarget(target);
    }
    
    @Override
    public void start() {
        // Only start if we can still target the entity
        LivingEntity owner = dragon.getOwner();
        if (owner == null) {
            return;
        }
        
        LivingEntity target = owner.getLastHurtByMob();
        if (target != null && dragon.canTarget(target)) {
            super.start();
        }
    }
}
