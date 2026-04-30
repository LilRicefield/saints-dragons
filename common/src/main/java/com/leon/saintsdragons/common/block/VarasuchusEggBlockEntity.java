package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class VarasuchusEggBlockEntity extends AbstractDragonEggBlockEntity {
    public VarasuchusEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VARASUCHUS_EGG.get(), pos, state);
    }
}