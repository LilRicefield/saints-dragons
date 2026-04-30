package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class IgnivorusEggBlockEntity extends AbstractDragonEggBlockEntity {
    public IgnivorusEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IGNIVORUS_EGG.get(), pos, state);
    }
}