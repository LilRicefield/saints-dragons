package com.leon.saintsdragons.server.ai.dragonbrain.sensor;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonSensor;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.Set;

public class DragonMovementStateSensor<T extends RideableDragonBase> extends DragonSensor<T> {
    public DragonMovementStateSensor(int scanRateTicks) {
        super(scanRateTicks);
    }

    @Override
    protected void scan(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        int ttl = scanMemoryTtl();
        DragonLocomotionMode mode = dragon.getLocomotionMode();
        context.memories().set(DragonMemories.LOCOMOTION_MODE, mode, ttl);
        context.memories().set(DragonMemories.IS_AERIAL, mode == DragonLocomotionMode.AIR, ttl);
        context.memories().set(DragonMemories.IS_GROUNDED, mode == DragonLocomotionMode.GROUND && dragon.isGroundedForAi(), ttl);
        context.memories().set(DragonMemories.IS_RIDDEN, dragon.isVehicle(), ttl);
        context.memories().set(DragonMemories.IN_WATER, mode == DragonLocomotionMode.WATER && dragon.isInWaterOrBubble(), ttl);
        context.memories().set(DragonMemories.IN_LAVA, mode == DragonLocomotionMode.WATER && dragon.isInLava(), ttl);
    }

    protected int scanMemoryTtl() {
        return 5;
    }

    @Override
    protected Set<MemoryModuleType<?>> memoriesUsed() {
        return Set.of(
                DragonMemories.LOCOMOTION_MODE,
                DragonMemories.IS_AERIAL,
                DragonMemories.IS_GROUNDED,
                DragonMemories.IS_RIDDEN,
                DragonMemories.IN_WATER,
                DragonMemories.IN_LAVA
        );
    }
}
