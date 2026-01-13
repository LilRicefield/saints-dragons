package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Random;

/**
 * Base class for all dragon AI goals providing common checks and utilities.
 * Extends this for dragon-specific behaviors with shared logic.
 */
public abstract class DragonBaseGoal<T extends RideableDragonBase> extends Goal {
    protected final T dragon;
    protected final Random random = new Random();

    public DragonBaseGoal(T dragon) {
        this.dragon = dragon;
    }

    /**
     * Common checks that apply to most dragon goals.
     * Override and call super to add additional checks.
     */
    @Override
    public boolean canUse() {
        return isBasicConditionsMet() && canUseAdditional();
    }

    @Override
    public boolean canContinueToUse() {
        return isBasicConditionsMet() && canContinueAdditional();
    }

    /**
     * Core conditions that must be met for most goals:
     * - Not sitting
     * - Not in sit transition animations
     * - Not being ridden (unless goal allows it)
     * - Not riding another entity
     */
    protected boolean isBasicConditionsMet() {
        if (dragon.isOrderedToSit()) {
            return false;
        }

        // Don't run during sit/stand animations (dragon-specific timing)
        if (dragon.isSittingDownAnimation()) {
            return false;
        }

        // Most goals shouldn't run while being controlled by a rider
        if (!allowsDuringRide() && dragon.isVehicle()) {
            return false;
        }

        // Don't run while riding another entity
        if (dragon.isPassenger()) {
            return false;
        }

        return true;
    }

    /**
     * Additional checks specific to the goal type.
     * Override this instead of canUse() to keep base checks.
     */
    protected abstract boolean canUseAdditional();

    /**
     * Additional checks for continuing the goal.
     * Override this instead of canContinueToUse() to keep base checks.
     */
    protected boolean canContinueAdditional() {
        return canUseAdditional();
    }

    /**
     * Whether this goal can run while the dragon is being ridden.
     * Override to return true for goals that should work during riding.
     */
    protected boolean allowsDuringRide() {
        return false;
    }

    /**
     * Check if dragon is in combat (has a living target).
     */
    protected boolean isInCombat() {
        return dragon.getTarget() != null && dragon.getTarget().isAlive();
    }

    /**
     * Check if goal should respect the command system.
     * Most autonomous goals should only run in Follow(0) or Wander(2) mode.
     */
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
