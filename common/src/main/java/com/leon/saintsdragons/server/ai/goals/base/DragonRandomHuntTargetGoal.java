package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class DragonRandomHuntTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
    private final DragonEntity dragon;
    private final BooleanSupplier huntEnabled;

    public DragonRandomHuntTargetGoal(DragonEntity dragon,
                                      int randomInterval,
                                      BooleanSupplier huntEnabled,
                                      Predicate<LivingEntity> preyPredicate) {
        super(dragon, LivingEntity.class, randomInterval, true, false, preyPredicate);
        this.dragon = dragon;
        this.huntEnabled = huntEnabled;
    }

    @Override
    public boolean canUse() {
        if (!dragon.isAlive()
                || dragon.isDying()
                || dragon.isTame()
                || dragon.isBaby()
                || dragon.isVehicle()
                || dragon.isPassenger()
                || dragon.isOrderedToSit()
                || dragon.isSleepLocked()
                || !huntEnabled.getAsBoolean()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleepLocked()
                && huntEnabled.getAsBoolean()
                && super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        LivingEntity target = dragon.getTarget();
        if (DragonTargetingHelper.isPassivePreyType(target)) {
            dragon.markPassiveHuntTarget(target);
        }
    }

    @Override
    public void stop() {
        dragon.clearPassiveHuntTarget();
        super.stop();
    }
}
