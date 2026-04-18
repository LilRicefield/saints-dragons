package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity for Cindervane eggs to store parent information and baby gender
 */
public class CindervaneEggBlockEntity extends AbstractDragonEggBlockEntity {
    public CindervaneEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CINDERVANE_EGG.get(), pos, state);
    }
}
