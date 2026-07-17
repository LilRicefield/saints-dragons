package com.leon.saintsdragons.server.ai.pathfinding;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsyncSwarmFlyNodeEvaluator extends FlyNodeEvaluator implements DragonPathSearchDebuggable {
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
        this.entityWidth = Mth.ceil(mob.getBbWidth());
        this.entityHeight = Mth.ceil(mob.getBbHeight());
        this.entityDepth = Mth.ceil(mob.getBbWidth());
    }

    @Override
    protected Node findAcceptedNode(int x, int y, int z) {
        Node node = super.findAcceptedNode(x, y, z);
        if (node == null) {
            return null;
        }

        AABB candidateBounds = this.mob.getBoundingBox().move(
                x + 0.5D - this.mob.getX(),
                y + 0.5D - this.mob.getY(),
                z + 0.5D - this.mob.getZ());
        return this.level.noCollision(this.mob, candidateBounds) ? node : null;
    }
}
