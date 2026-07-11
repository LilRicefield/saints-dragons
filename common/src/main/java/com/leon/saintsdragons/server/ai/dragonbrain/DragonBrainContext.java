package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.server.level.ServerLevel;

public final class DragonBrainContext<T extends DragonEntity> {
    private final T dragon;
    private final ServerLevel level;
    private final DragonMemoryMap memories;

    DragonBrainContext(T dragon, ServerLevel level) {
        this.dragon = dragon;
        this.level = level;
        this.memories = new DragonMemoryMap(dragon.getBrain());
    }

    public T dragon() {
        return dragon;
    }

    public ServerLevel level() {
        return level;
    }

    public DragonMemoryMap memories() {
        return memories;
    }

    public long gameTime() {
        return level.getGameTime();
    }
}
