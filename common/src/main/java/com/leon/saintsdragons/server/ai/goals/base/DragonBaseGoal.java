package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Random;

public abstract class DragonBaseGoal<T extends RideableDragonBase> extends Goal {
    protected final T dragon;
    protected final Random random = new Random();

    public DragonBaseGoal(T dragon) {
        this.dragon = dragon;
    }

    @Override
    public boolean canUse() {
        return isBasicConditionsMet() && canUseAdditional();
    }

    @Override
    public boolean canContinueToUse() {
        return isBasicConditionsMet() && canContinueAdditional();
    }

    protected boolean isBasicConditionsMet() {
        if (dragon.isOrderedToSit()) {
            return false;
        }
        if (dragon.isSittingDownAnimation()) {
            return false;
        }

        if (!allowsDuringRide() && dragon.isVehicle()) {
            return false;
        }

        // Don't run while riding another entity
        if (dragon.isPassenger()) {
            return false;
        }

        return true;
    }

    protected abstract boolean canUseAdditional();

    protected boolean canContinueAdditional() {
        return canUseAdditional();
    }

    protected boolean allowsDuringRide() {
        return false;
    }

    protected boolean isInCombat() {
        return dragon.getTarget() != null && dragon.getTarget().isAlive();
    }

    protected boolean checkCommandCompatible(int... allowedCommands) {
        if (!dragon.isTame()) {
            return true; // Wild dragons ignore commands
        }

        int currentCommand = dragon.getCommand();
        for (int allowed : allowedCommands) {
            if (currentCommand == allowed) {
                return true;
            }
        }
        return false;
    }
}