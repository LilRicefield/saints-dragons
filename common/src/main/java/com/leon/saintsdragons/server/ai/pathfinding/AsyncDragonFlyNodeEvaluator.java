package com.leon.saintsdragons.server.ai.pathfinding;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;


public class AsyncDragonFlyNodeEvaluator extends FlyNodeEvaluator {
    @Override
    public void prepare(PathNavigationRegion level, Mob mob) {
        super.prepare(level, mob);
        this.entityWidth = Mth.floor(mob.getBbWidth()) + 1;
        this.entityHeight = Mth.floor(mob.getBbHeight());
        this.entityDepth = Mth.floor(mob.getBbWidth()) + 1;
    }
}
