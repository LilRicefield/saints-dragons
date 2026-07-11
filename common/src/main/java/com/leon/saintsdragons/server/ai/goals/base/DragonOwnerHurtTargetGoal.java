package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;

public class DragonOwnerHurtTargetGoal extends OwnerHurtTargetGoal {
    private final DragonEntity dragon;
    
    public DragonOwnerHurtTargetGoal(DragonEntity dragon) {
        super(dragon);
        this.dragon = dragon;
    }
    
    @Override
    public boolean canUse() {
        if (!super.canUse()) {
            return false;
        }
        LivingEntity owner = dragon.getOwner();
        if (owner == null) {
            return false;
        }
        
        LivingEntity target = owner.getLastHurtMob();
        if (target == null) {
            return false;
        }
        return dragon.canTarget(target);
    }
    
    @Override
    public void start() {
        LivingEntity owner = dragon.getOwner();
        if (owner == null) {
            return;
        }
        
        LivingEntity target = owner.getLastHurtMob();
        if (target != null && dragon.canTarget(target)) {
            super.start();
        }
    }
}
