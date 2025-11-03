package com.leon.saintsdragons.server.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import org.jetbrains.annotations.NotNull;

/**
 * Amphibious evaluator that keeps the generous water handling from {@link DragonSwimNodeEvaluator}
 * while still allowing surface/ground traversal via the vanilla amphibious logic.
 * Designed to be shared by all semiaquatic dragons that rely on {@link DragonAmphibiousNavigation}.
 */
public class DragonAmphibiousNodeEvaluator extends AmphibiousNodeEvaluator {

    public DragonAmphibiousNodeEvaluator() {
        super(true); // Allow breaching so mobs can leave the water column.
    }

    public void prepare(PathNavigationRegion region, PathfinderMob mob) {
        super.prepare(region, mob);
        mob.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        mob.setPathfindingMalus(BlockPathTypes.BREACH, 0.0F);
    }

    @Override
    public @NotNull Node getStart() {
        // Sample around mid-body height to reduce thrashing at the surface for tall mobs.
        int centerY = Mth.floor(this.mob.getBoundingBox().minY + (this.mob.getBbHeight() * 0.5D));
        BlockPos origin = BlockPos.containing(this.mob.position());

        if (!isWaterColumn(origin)) {
            BlockPos.MutableBlockPos cursor = origin.mutable();
            while (cursor.getY() > this.mob.level().getMinBuildHeight()) {
                cursor.move(0, -1, 0);
                if (isWaterColumn(cursor)) {
                    origin = cursor.immutable();
                    centerY = cursor.getY();
                    break;
                }
            }
        }
        return super.getNode(origin.getX(), centerY, origin.getZ());
    }

    public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z, PathfinderMob mob) {
        BlockPathTypes base = super.getBlockPathType(level, x, y, z, mob);
        if (base == BlockPathTypes.WATER_BORDER || base == BlockPathTypes.BREACH) {
            return BlockPathTypes.WATER;
        }

        if (base == BlockPathTypes.OPEN) {
            BlockState state = level.getBlockState(BlockPos.containing(x, y, z));
            if (!state.getFluidState().isEmpty()) {
                return BlockPathTypes.WATER;
            }
        }
        return base;
    }

    private boolean isWaterColumn(BlockPos pos) {
        FluidState fluid = this.mob.level().getFluidState(pos);
        return !fluid.isEmpty();
    }
}
