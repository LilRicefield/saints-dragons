package com.leon.saintsdragons.server.entity.ability.abilities.atroxiia;

import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

final class AtroxiiaFrostWalker {
    private static final int MAX_RADIUS = 84;
    private static final int SHORE_VERTICAL_REACH = 10;

    private AtroxiiaFrostWalker() {
    }

    static void freezeNearbyWater(Atroxiia dragon, int frostWalkerLevel) {
        if (!(dragon.level() instanceof ServerLevel level)) {
            return;
        }

        int radius = Math.min(MAX_RADIUS, 2 + Math.max(0, frostWalkerLevel));
        int radiusSqr = radius * radius;
        int centerX = Mth.floor(dragon.getX());
        int centerZ = Mth.floor(dragon.getZ());
        int topY = Mth.floor(dragon.getBoundingBox().minY);
        BlockPos.MutableBlockPos waterPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos abovePos = new BlockPos.MutableBlockPos();

        for (int xOffset = -radius; xOffset <= radius; xOffset++) {
            for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                if (xOffset * xOffset + zOffset * zOffset > radiusSqr) {
                    continue;
                }

                int x = centerX + xOffset;
                int z = centerZ + zOffset;
                for (int yOffset = 0; yOffset <= SHORE_VERTICAL_REACH; yOffset++) {
                    waterPos.set(x, topY - yOffset, z);
                    if (!level.hasChunkAt(waterPos) || !level.getWorldBorder().isWithinBounds(waterPos)) {
                        continue;
                    }

                    BlockState waterState = level.getBlockState(waterPos);
                    FluidState fluidState = waterState.getFluidState();
                    abovePos.set(x, waterPos.getY() + 1, z);
                    if (waterState.is(Blocks.WATER)
                            && fluidState.is(FluidTags.WATER)
                            && fluidState.isSource()
                            && level.getBlockState(abovePos).isAir()) {
                        level.setBlockAndUpdate(waterPos, Blocks.ICE.defaultBlockState());
                        break;
                    }
                }
            }
        }
    }
}
