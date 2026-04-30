package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class RaevyxEggBlockEntity extends AbstractDragonEggBlockEntity {
    public RaevyxEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RAEVYX_EGG.get(), pos, state);
    }
}