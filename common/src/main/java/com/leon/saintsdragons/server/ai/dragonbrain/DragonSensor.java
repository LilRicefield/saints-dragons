package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public abstract class DragonSensor<T extends DragonEntity> extends Sensor<T> {
    protected DragonSensor(int scanRateTicks) {
        super(Math.max(1, scanRateTicks));
    }

    @Override
    protected final void doTick(@NotNull ServerLevel level, @NotNull T dragon) {
        DragonBrainContext<T> context = new DragonBrainContext<>(dragon, level);
        if (canScan(context)) {
            scan(context);
        }
    }

    protected boolean canScan(DragonBrainContext<T> context) {
        return true;
    }

    protected abstract void scan(DragonBrainContext<T> context);

    protected abstract Set<MemoryModuleType<?>> memoriesUsed();

    @Override
    public final @NotNull Set<MemoryModuleType<?>> requires() {
        return memoriesUsed();
    }
}
