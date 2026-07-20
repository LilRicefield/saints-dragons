package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class FirstApplicableDragonBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private final List<DragonBehaviour<T>> behaviours;
    @Nullable
    private DragonBehaviour<T> running;

    @SafeVarargs
    public FirstApplicableDragonBehaviour(DragonBehaviour<T>... behaviours) {
        this.behaviours = List.copyOf(Arrays.asList(behaviours));
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        if (context.memories().has(DragonMemories.INVESTIGATION_TARGET)
                && !context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false)) {
            return false;
        }
        long gameTime = context.gameTime();
        for (DragonBehaviour<T> behaviour : behaviours) {
            if (behaviour.tryStart(context.level(), context.dragon(), gameTime)) {
                running = behaviour;
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return running != null && running.getStatus() == Behavior.Status.RUNNING;
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        if (running == null) {
            return;
        }
        running.tickOrStop(context.level(), context.dragon(), context.gameTime());
        if (running.getStatus() == Behavior.Status.STOPPED) {
            running = null;
            doStop(context.level(), context.dragon(), context.gameTime());
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        if (running != null) {
            running.doStop(context.level(), context.dragon(), context.gameTime());
            running = null;
        }
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        if (running == null) {
            return Map.of("active_child", "none");
        }
        return Map.of(
                "active_child", running.getClass().getSimpleName(),
                "child_details", running.getDragonBrainDebugDetails().toString()
        );
    }
}
