package com.leon.saintsdragons.server.ai.navigation;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;

/**
 * Water navigation used by semi-aquatic dragons. Extends the vanilla water navigation but swaps in
 * {@link DragonSwimNodeEvaluator} and the shared {@link DragonPathFinder} to keep path logic in sync
 * with the custom ground navigation.
 */
public class DragonSwimNavigate extends net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation {

    public DragonSwimNavigate(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new DragonSwimNodeEvaluator();
        return new DragonPathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.mob.isInWaterOrBubble();
    }

    @Override
    public boolean isStableDestination(net.minecraft.core.BlockPos pos) {
        return !this.level.getBlockState(pos.below()).isAir();
    }
}

