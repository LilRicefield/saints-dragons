package com.leon.saintsdragons.server.ai.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.NotNull;

public class DragonWalkNodeEvaluator extends WalkNodeEvaluator {
    @Override
    public @NotNull Node getStart() {
        Node vanillaStart = super.getStart();
        int footprintOffset = getFootprintOffset();
        if (footprintOffset == 0) {
            return vanillaStart;
        }

        return getStartNode(new BlockPos(
                this.mob.getBlockX() - footprintOffset,
                vanillaStart.y,
                this.mob.getBlockZ() - footprintOffset
        ));
    }

    @Override
    public @NotNull Target getGoal(double x, double y, double z) {
        int footprintOffset = getFootprintOffset();
        return getTargetFromNode(getNode(
                Mth.floor(x) - footprintOffset,
                Mth.floor(y),
                Mth.floor(z) - footprintOffset
        ));
    }

    private int getFootprintOffset() {
        return Math.max(0, this.entityWidth / 2);
    }

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
