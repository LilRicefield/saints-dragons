package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public final class DragonBehaviourUtils {
    private DragonBehaviourUtils() {
    }

    public static <T extends DragonEntity> void setWalkAndLookTarget(DragonBrainContext<T> context,
                                                                     LivingEntity target,
                                                                     float speedModifier,
                                                                     int closeEnoughDistance) {
        setWalkAndLookTarget(context, target, target, speedModifier, closeEnoughDistance);
    }

    public static <T extends DragonEntity> void setWalkAndLookTarget(DragonBrainContext<T> context,
                                                                     LivingEntity lookTarget,
                                                                     Entity walkTarget,
                                                                     float speedModifier,
                                                                     int closeEnoughDistance) {
        context.memories().set(DragonMemories.LOOK_TARGET, new EntityTracker(lookTarget, true));
        context.memories().set(
                DragonMemories.WALK_TARGET,
                new WalkTarget(
                        new EntityTracker(walkTarget, false),
                        speedModifier,
                        Math.max(0, closeEnoughDistance)
                )
        );
    }
}
