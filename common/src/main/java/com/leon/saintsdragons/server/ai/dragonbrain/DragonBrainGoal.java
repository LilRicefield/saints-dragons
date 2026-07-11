package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.function.Predicate;

public final class DragonBrainGoal<T extends RideableDragonBase> extends Goal {
    private final T dragon;
    private final Predicate<T> canRun;

    public DragonBrainGoal(T dragon, DragonBrainOwner<T> owner) {
        this(dragon, owner, EnumSet.noneOf(Flag.class), DragonBrainGoal::defaultCanRun);
    }

    public DragonBrainGoal(T dragon, DragonBrainOwner<T> owner, EnumSet<Flag> flags) {
        this(dragon, owner, flags, DragonBrainGoal::defaultCanRun);
    }

    public DragonBrainGoal(T dragon, DragonBrainOwner<T> owner, EnumSet<Flag> flags, Predicate<T> canRun) {
        this.dragon = dragon;
        this.canRun = candidate -> canRun.test(candidate) && owner.shouldTakeControl(candidate);
        setFlags(flags);
    }

    public net.minecraft.world.entity.ai.Brain<?> brain() {
        return dragon.getBrain();
    }

    @Override
    public boolean canUse() {
        return canRun.test(dragon);
    }

    @Override
    public boolean canContinueToUse() {
        return canRun.test(dragon);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
    }

    @Override
    public void stop() {
        dragon.getAIMovement().stopAndClearAllMovement();
    }

    private static boolean defaultCanRun(RideableDragonBase dragon) {
        return !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSittingDownAnimation();
    }
}
