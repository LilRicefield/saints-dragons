package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class VolitansEggBlockEntity extends AbstractDragonEggBlockEntity {
    public VolitansEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOLITANS_EGG.get(), pos, state);
    }
}
