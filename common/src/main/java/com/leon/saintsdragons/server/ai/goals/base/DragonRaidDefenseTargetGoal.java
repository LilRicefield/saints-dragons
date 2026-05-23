package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.raid.Raider;

public class DragonRaidDefenseTargetGoal extends NearestAttackableTargetGoal<Raider> {
    private final DragonEntity dragon;

    public DragonRaidDefenseTargetGoal(DragonEntity dragon) {
        super(dragon, Raider.class, 10, true, false, DragonRaidDefenseTargetGoal::isValidRaidTarget);
        this.dragon = dragon;
    }

    @Override
    public boolean canUse() {
        if (!canDragonDefendRaid()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return canDragonDefendRaid()
                && isValidRaidTarget(this.target)
                && dragon.canTarget(this.target)
                && super.canContinueToUse();
    }

    private boolean canDragonDefendRaid() {
        return dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isBaby()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleepLocked();
    }

    private static boolean isValidRaidTarget(LivingEntity target) {
        return DragonTargetingHelper.isActiveRaidTarget(target);
    }
}
