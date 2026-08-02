package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

public abstract class DragonOneShotBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    protected DragonOneShotBehaviour() {
        super();
    }

    protected DragonOneShotBehaviour(boolean claimsControl) {
        super(claimsControl);
    }

    protected DragonOneShotBehaviour(Map<MemoryModuleType<?>, MemoryStatus> memoryRequirements) {
        super(memoryRequirements);
    }

    protected DragonOneShotBehaviour(Map<MemoryModuleType<?>, MemoryStatus> memoryRequirements,
                                     boolean claimsControl) {
        super(memoryRequirements, claimsControl);
    }

    @Override
    protected final boolean canContinue(DragonBrainContext<T> context) {
        return false;
    }
}
