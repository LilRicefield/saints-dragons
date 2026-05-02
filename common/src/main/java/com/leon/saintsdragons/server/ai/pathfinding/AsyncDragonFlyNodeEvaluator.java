package com.leon.saintsdragons.server.ai.pathfinding;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import org.jetbrains.annotations.NotNull;


public class AsyncDragonFlyNodeEvaluator extends FlyNodeEvaluator {
    @Override
    public void prepare(@NotNull PathNavigationRegion level, @NotNull Mob mob) {
        super.prepare(level, mob);
        this.entityWidth = Mth.floor(mob.getBbWidth()) + 1;
        this.entityHeight = Mth.floor(mob.getBbHeight());
        this.entityDepth = Mth.floor(mob.getBbWidth()) + 1;
    }
}
