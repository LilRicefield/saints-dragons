package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DraconianNucleusBlockEntity extends BlockEntity {
    private long animationTicks;

    public DraconianNucleusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRACONIAN_NUCLEUS.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DraconianNucleusBlockEntity nucleus) {
        nucleus.animationTicks++;
    }

    public long getAnimationTimeMillis(float partialTick) {
        return (long) ((this.animationTicks + partialTick) * 50.0F);
    }
}
