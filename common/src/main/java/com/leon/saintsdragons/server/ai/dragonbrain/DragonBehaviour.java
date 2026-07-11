package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public abstract class DragonBehaviour<T extends DragonEntity> extends Behavior<T> {
    private long cooldownEndsAtTick;
    private final boolean claimsControl;
    private Activity activity;

    protected DragonBehaviour() {
        this(Map.of(), true);
    }

    protected DragonBehaviour(boolean claimsControl) {
        this(Map.of(), claimsControl);
    }

    protected DragonBehaviour(Map<MemoryModuleType<?>, MemoryStatus> memoryRequirements) {
        this(memoryRequirements, true);
    }

    protected DragonBehaviour(Map<MemoryModuleType<?>, MemoryStatus> memoryRequirements, boolean claimsControl) {
        super(memoryRequirements, Integer.MAX_VALUE);
        this.claimsControl = claimsControl;
    }

    public final boolean claimsControl() {
        return claimsControl;
    }

    final void bindActivity(Activity activity) {
        if (this.activity != null && this.activity != activity) {
            throw new IllegalStateException("A DragonBehaviour instance cannot belong to multiple activities");
        }
        this.activity = activity;
    }

    @Override
    protected final boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull T dragon) {
        DragonBrainContext<T> context = new DragonBrainContext<>(dragon, level);
        return context.gameTime() >= cooldownEndsAtTick && canStart(context);
    }

    @Override
    protected final boolean canStillUse(@NotNull ServerLevel level, @NotNull T dragon, long gameTime) {
        return (activity == null || dragon.getBrain().getActiveActivities().contains(activity))
                && canContinue(new DragonBrainContext<>(dragon, level));
    }

    @Override
    protected final void start(@NotNull ServerLevel level, @NotNull T dragon, long gameTime) {
        start(new DragonBrainContext<>(dragon, level));
    }

    @Override
    protected final void tick(@NotNull ServerLevel level, @NotNull T dragon, long gameTime) {
        tick(new DragonBrainContext<>(dragon, level));
    }

    @Override
    protected final void stop(@NotNull ServerLevel level, @NotNull T dragon, long gameTime) {
        DragonBrainContext<T> context = new DragonBrainContext<>(dragon, level);
        cooldownEndsAtTick = context.gameTime() + Math.max(0, cooldownForTicks(context));
        stop(context);
        context.memories().eraseAll(clearMemoriesWhenStopped());
    }

    public List<MemoryModuleType<?>> clearMemoriesWhenStopped() {
        return List.of();
    }

    protected boolean canStart(DragonBrainContext<T> context) {
        return true;
    }

    protected boolean canContinue(DragonBrainContext<T> context) {
        return true;
    }

    protected int cooldownForTicks(DragonBrainContext<T> context) {
        return 0;
    }

    protected void start(DragonBrainContext<T> context) {
    }

    protected void tick(DragonBrainContext<T> context) {
    }

    protected void stop(DragonBrainContext<T> context) {
    }
}
