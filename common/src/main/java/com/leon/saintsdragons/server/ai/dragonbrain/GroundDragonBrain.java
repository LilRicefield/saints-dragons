package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.jetbrains.annotations.Nullable;

public abstract class GroundDragonBrain<T extends RideableDragonBase> extends DragonBrainProfile<T> {
    @Override
    protected boolean canFight(T dragon, @Nullable LivingEntity target) {
        return super.canFight(dragon, target) && withinAggroRange(dragon, target);
    }

    protected abstract boolean withinAggroRange(T dragon, LivingEntity target);

    @Override
    protected void onCombatUnavailable(Brain<T> brain, T dragon, @Nullable LivingEntity target) {
        if (target != null && (!DragonTargetLifecycle.isValidTarget(dragon, target)
                || !withinAggroRange(dragon, target))) {
            DragonTargetLifecycle.clearCombatTarget(brain, dragon, true);
        }
    }
}
