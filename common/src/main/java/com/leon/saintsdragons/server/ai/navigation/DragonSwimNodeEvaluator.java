package com.leon.saintsdragons.server.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.SwimNodeEvaluator;
import org.jetbrains.annotations.NotNull;

/**
 * Swim node evaluator that is a little more permissive for large semi-aquatic dragons.
 * Keeps water traversal enabled while smoothing transitions when the mob breaches or
 * moves near the water surface.
 */
public class DragonSwimNodeEvaluator extends SwimNodeEvaluator {

    public DragonSwimNodeEvaluator() {
        super(true);
    }

    public void prepare(PathNavigationRegion region, PathfinderMob mob) {
        super.prepare(region, mob);
        mob.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
    }

    @Override
    public @NotNull Node getStart() {
        // Bias start position toward the entity's mid-body rather than feet to reduce
        // thrashing when entering deep water.
        int y = Mth.floor(this.mob.getBoundingBox().minY + (this.mob.getBbHeight() * 0.5D));
        BlockPos pos = BlockPos.containing(this.mob.position());
        FluidState fluid = this.mob.level().getFluidState(pos);
        if (fluid.isEmpty()) {
            // Slide downward until we find the first fluid column so the path starts underwater
            BlockPos down = pos.mutable();
            while (down.getY() > this.mob.level().getMinBuildHeight()) {
                down = down.below();
                if (!this.mob.level().getFluidState(down).isEmpty()) {
                    pos = down;
                    y = down.getY();
                    break;
                }
            }
        }
        return super.getNode(pos.getX(), y, pos.getZ());
    }

    public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z, PathfinderMob mob) {
        BlockPathTypes type = super.getBlockPathType(level, x, y, z, mob);
        if (type == BlockPathTypes.WATER || type == BlockPathTypes.WATER_BORDER) {
            return BlockPathTypes.WATER;
        }
        if (type == BlockPathTypes.BREACH) {
            return BlockPathTypes.WATER;
        }
        if (type == BlockPathTypes.OPEN) {
            BlockState state = level.getBlockState(BlockPos.containing(x, y, z));
            if (!state.getFluidState().isEmpty()) {
                return BlockPathTypes.WATER;
            }
        }
        return type;
    }
}

