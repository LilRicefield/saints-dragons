package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.ai.pathfinding.DragonPathSearchDebug;
import com.leon.saintsdragons.server.ai.pathfinding.DragonPathSearchDebuggable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class AsyncDragonFlyNodeEvaluator extends FlyNodeEvaluator implements DragonPathSearchDebuggable {
    private @Nullable DragonPathSearchDebug.NodeCollector pathSearchDebugCollector;

    @Override
    public void setPathSearchDebugCollector(@Nullable DragonPathSearchDebug.NodeCollector collector) {
        this.pathSearchDebugCollector = collector;
    }

    @Override
    public int getNeighbors(Node[] neighbors, Node current) {
        int count = super.getNeighbors(neighbors, current);
        if (this.pathSearchDebugCollector != null) {
            this.pathSearchDebugCollector.recordExpansion(current, neighbors, count);
        }
        return count;
    }

    @Override
    public void prepare(@NotNull PathNavigationRegion level, @NotNull Mob mob) {
        super.prepare(level, mob);
        this.entityWidth = Mth.floor(mob.getBbWidth()) + 1;
        this.entityHeight = Math.max(1, Mth.floor(mob.getBbHeight()));
        this.entityDepth = Mth.floor(mob.getBbWidth()) + 1;
    }
}
