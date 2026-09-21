package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.ai.dragonbrain.debug.DragonBrainDebugDetails;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;

public abstract class DragonBehaviour<T extends DragonEntity> extends Behavior<T> implements DragonBrainDebugDetails {
    private long cooldownEndsAtTick;
    private final boolean claimsControl;
    private Activity activity;
    private int priority = -1;

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

    /**
     * Describes whether this behaviour is intended to own an exclusive action slot.
     * Vanilla Brain does not arbitrate this flag; composites and movement intents do.
     */
    public final boolean claimsControl() {
        return claimsControl;
    }

    public final Activity activity() {
        return activity;
    }

    public final int priority() {
        return priority;
    }

    public final long cooldownRemaining(long gameTime) {
        return Math.max(0L, cooldownEndsAtTick - gameTime);
    }

    public DragonBehaviourInterruption interruptionType() {
        return DragonBehaviourInterruption.NONE;
    }

    public boolean requestsInterruption(DragonBrainContext<T> context) {
        return false;
    }

    public boolean canYieldTo(DragonBrainContext<T> context, DragonBehaviourInterruption interruption) {
        return interruption.outranks(interruptionType());
    }

    protected static boolean hasCommittedAction(DragonEntity dragon) {
        return DragonBehaviourEligibility.hasCommittedAction(dragon);
    }

    final void bindActivity(Activity activity, int priority) {
        if (this.activity != null && this.activity != activity) {
            throw new IllegalStateException("A DragonBehaviour instance cannot belong to multiple activities");
        }
        this.activity = activity;
        this.priority = priority;
    }

    @Override
    protected final boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull T dragon) {
        if (pauseCombatForSight(dragon)) return false;
        DragonBrainContext<T> context = new DragonBrainContext<>(dragon, level);
        return context.gameTime() >= cooldownEndsAtTick && asMovementOwner(dragon, false, () -> canStart(context));
    }

    @Override
    protected final boolean canStillUse(@NotNull ServerLevel level, @NotNull T dragon, long gameTime) {
        return (activity == null || dragon.getBrain().getActiveActivities().contains(activity))
                && asMovementOwner(dragon, false, () -> canContinue(new DragonBrainContext<>(dragon, level)));
    }

    @Override
    protected final void start(@NotNull ServerLevel level, @NotNull T dragon, long gameTime) {
        asMovementOwner(dragon, false, () -> { start(new DragonBrainContext<>(dragon, level)); return null; });
    }

    @Override
    protected final void tick(@NotNull ServerLevel level, @NotNull T dragon, long gameTime) {
        // Keep the running behaviour's state, but do not make combat decisions from hidden positions.
        if (pauseCombatForSight(dragon)) return;
        asMovementOwner(dragon, false, () -> { tick(new DragonBrainContext<>(dragon, level)); return null; });
    }

    private boolean pauseCombatForSight(T dragon) {
        var brain = dragon.getBrain();
        return activity == Activity.FIGHT && brain.hasMemoryValue(DragonMemories.ATTACK_TARGET)
                && !brain.getMemory(DragonMemories.TARGET_VISIBLE).orElse(true)
                && !brain.hasMemoryValue(DragonMemories.RESCUE_TARGET)
                && !brain.hasMemoryValue(DragonMemories.INTERCEPT_PROJECTILE);
    }

    @Override
    protected final void stop(@NotNull ServerLevel level, @NotNull T dragon, long gameTime) {
        DragonBrainContext<T> context = new DragonBrainContext<>(dragon, level);
        cooldownEndsAtTick = context.gameTime() + Math.max(0, cooldownForTicks(context));
        asMovementOwner(dragon, true, () -> {
            try {
                stop(context);
                context.memories().eraseAll(clearMemoriesWhenStopped());
            } finally {
                context.utilities().resources().release(this, context.memories());
            }
            return null;
        });
    }

    private <R> R asMovementOwner(T dragon, boolean cleanup, Supplier<R> action) {
        return dragon.getBrainUtilities().resources().runAs(this, cleanup,
                () -> dragon instanceof RideableDragonBase rideable
                        ? rideable.getAIMovement().brainMovement().runAs(this, cleanup, action) : action.get());
    }

    protected final boolean completeTask(T dragon, DragonBehaviourResources.TaskToken token, Runnable action) {
        if (dragon.isRemoved()) return false;
        return dragon.getBrainUtilities().resources().complete(token,
                () -> asMovementOwner(dragon, false, () -> { action.run(); return null; }));
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
