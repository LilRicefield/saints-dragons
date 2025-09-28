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
        // For swimming dragons, only consider positions underwater as stable
        // This prevents them from trying to surface and bob up and down
        return this.level.getFluidState(pos).isSource() || 
               this.level.getFluidState(pos.below()).isSource();
    }
}

