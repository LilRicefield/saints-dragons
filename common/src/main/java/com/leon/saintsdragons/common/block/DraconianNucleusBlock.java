package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DraconianNucleusBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(2.0D, 2.0D, 2.0D, 14.0D, 14.0D, 14.0D);

    public DraconianNucleusBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull net.minecraft.world.level.BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public float getDestroyProgress(@NotNull BlockState state, @NotNull Player player,
                                    @NotNull BlockGetter level, @NotNull BlockPos pos) {
        if (player.isCreative()) {
            return super.getDestroyProgress(state, player, level, pos);
        }
        if (level.getBlockEntity(pos) instanceof DraconianNucleusBlockEntity nucleus && nucleus.isHarvestable()) {
            return super.getDestroyProgress(state, player, level, pos);
        }
        return 0.0F;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new DraconianNucleusBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                            @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof DraconianNucleusBlockEntity nucleus) {
            nucleus.setControllerActivationOnly(player);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.DRACONIAN_NUCLEUS.get(), DraconianNucleusBlockEntity::tick);
    }
}
