package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class StegonautEggBlockEntity extends AbstractDragonEggBlockEntity {
    public StegonautEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEGONAUT_EGG.get(), pos, state);
    }
}