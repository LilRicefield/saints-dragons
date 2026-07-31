package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AtroxiiaEggBlockEntity extends AbstractDragonEggBlockEntity {
    public AtroxiiaEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATROXIIA_EGG.get(), pos, state);
    }
}
