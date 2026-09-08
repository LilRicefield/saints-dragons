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
        // DragonBrainOwner applies once after all producers and outgoing cleanup ran.
        // Keep this registered behaviour for existing species profiles/debug layouts.
    }

    public static void applyPending(RideableDragonBase dragon) {
        DragonMovementIntent intent = dragon.getBrain().getMemory(DragonMemories.MOVEMENT_INTENT).orElse(null);
        var movement = dragon.getAIMovement();
        if (intent == null) {
            movement.brainMovement().discardPending();
            return;
        }
        dragon.getBrain().eraseMemory(DragonMemories.MOVEMENT_INTENT);
        movement.brainMovement().apply(intent, movement.getMovementCommandGeneration(), () -> {
            intent.apply(dragon);
            boolean hold = intent instanceof DragonMovementIntent.Stop || intent instanceof DragonMovementIntent.HoldPosition;
            String reason = intent instanceof DragonMovementIntent.Stop stop ? stop.reason() : intent.getClass().getSimpleName();
            movement.brainMovement().commanded(movement.getMovementCommandGeneration(),
                    dragon.level().getGameTime(), reason, hold);
        });
    }
}
