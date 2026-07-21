package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.behavior.Behavior;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class FirstApplicableDragonBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
    private final List<DragonBehaviour<T>> behaviours;
    @Nullable
    private DragonBehaviour<T> running;

    @SafeVarargs
    public FirstApplicableDragonBehaviour(DragonBehaviour<T>... behaviours) {
        List<DragonBehaviour<T>> ordered = new ArrayList<>(behaviours.length + 1);
        ordered.add(new DragonMaintainPersonalSpaceBehaviour<>());
        ordered.addAll(Arrays.asList(behaviours));
        this.behaviours = List.copyOf(ordered);
    }

    public List<DragonBehaviour<T>> childBehaviours() {
        return behaviours;
    }

    @Nullable
    public DragonBehaviour<T> runningBehaviour() {
        return running;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        if (controlReserved(context)) {
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
        return !controlReserved(context)
                && running != null
                && running.getStatus() == Behavior.Status.RUNNING;
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        if (running == null) {
            return;
        }
        if (controlReserved(context)) {
            relinquishControl(context);
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
        relinquishControl(context);
    }

    private boolean controlReserved(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        return dragon.isSleeping()
                || dragon.isSleepTransitioning()
                || dragon.isOrderedToSit()
                || dragon.isInSittingPose()
                || dragon.isInSitTransition()
                || dragon.isHuntFoodPursuitActive()
                || context.memories().has(DragonMemories.INVESTIGATION_TARGET)
                && !context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false);
    }

    private void relinquishControl(DragonBrainContext<T> context) {
        if (running == null) {
            return;
        }
        DragonMovementIntent reservedIntent = context.dragon().isHuntFoodPursuitActive()
                ? context.memories().get(DragonMemories.MOVEMENT_INTENT).orElse(null)
                : null;
        running.doStop(context.level(), context.dragon(), context.gameTime());
        running = null;
        if (reservedIntent != null) {
            context.memories().set(DragonMemories.MOVEMENT_INTENT, reservedIntent);
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
