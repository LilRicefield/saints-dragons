package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

public class ApplyMovementIntentBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
    public ApplyMovementIntentBehaviour() {
        super(Map.of(DragonMemories.MOVEMENT_INTENT, MemoryStatus.VALUE_PRESENT), false);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return context.memories().has(DragonMemories.MOVEMENT_INTENT);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        DragonMovementIntent intent = context.memories()
                .get(DragonMemories.MOVEMENT_INTENT)
                .orElse(DragonMovementIntent.none());
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        intent.apply(context.dragon());
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return false;
    }
}
