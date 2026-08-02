package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonOneShotBehaviour;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

public class ApplyMovementIntentBehaviour<T extends RideableDragonBase> extends DragonOneShotBehaviour<T> {
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
                .take(DragonMemories.MOVEMENT_INTENT)
                .orElse(DragonMovementIntent.none());
        intent.apply(context.dragon());
    }
}
