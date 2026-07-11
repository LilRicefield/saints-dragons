package com.leon.saintsdragons.server.ai.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.NotNull;

public class DragonWalkNodeEvaluator extends WalkNodeEvaluator {
    @Override
    public @NotNull BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.LADDER)) {
            return BlockPathTypes.WALKABLE;
        }
        return super.getBlockPathType(level, x, y, z);
    }
}
