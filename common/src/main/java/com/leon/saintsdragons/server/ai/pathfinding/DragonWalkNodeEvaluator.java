package com.leon.saintsdragons.server.ai.pathfinding;

import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;
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
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

public class DragonWalkNodeEvaluator extends WalkNodeEvaluator implements DragonPathSearchDebuggable {
    private final BooleanSupplier canPassThroughTrees;
    private @Nullable DragonPathSearchDebug.NodeCollector pathSearchDebugCollector;

    public DragonWalkNodeEvaluator() {
        this(false);
    }

    public DragonWalkNodeEvaluator(boolean canPassThroughTrees) {
        this(() -> canPassThroughTrees);
    }

    public DragonWalkNodeEvaluator(BooleanSupplier canPassThroughTrees) {
        this.canPassThroughTrees = canPassThroughTrees;
    }

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
        if (this.canPassThroughTrees.getAsBoolean()
                && DragonDestructionManager.isPassivelyBreakableTreeBlock(state)) {
            return BlockPathTypes.OPEN;
        }
        return super.getBlockPathType(level, x, y, z);
    }
}
