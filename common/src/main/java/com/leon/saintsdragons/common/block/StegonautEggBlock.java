package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModBlockEntities;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Stegonaut egg block that hatches into a baby Stegonaut over time.
 */
public class StegonautEggBlock extends AbstractTimedDragonEggBlock<StegonautEggBlockEntity> {
    private static final int DEFAULT_HATCH_TICKS = 30000; // 25 minutes
    private static final VoxelShape EGG_SHAPE = box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D);

    public StegonautEggBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return EGG_SHAPE;
    }

    @Override
    protected ResourceLocation getDragonConfigId() {
        return DragonAttributeConfigLoader.STEGONAUT_ID;
    }

    @Override
    protected int getDefaultNormalHatchTicks() {
        return DEFAULT_HATCH_TICKS;
    }

    @Override
    protected Supplier<BlockEntityType<StegonautEggBlockEntity>> getEggBlockEntityType() {
        return ModBlockEntities.STEGONAUT_EGG;
    }

    @Override
    protected StegonautEggBlockEntity createEggBlockEntity(BlockPos pos, BlockState state) {
        return new StegonautEggBlockEntity(pos, state);
    }

    @Override
    protected DragonEntity createBaby(ServerLevel level) {
        return ModEntities.STEGONAUT.get().create(level);
    }

    @Override
    protected void applyBabyAttributes(DragonEntity baby) {
        ((Stegonaut) baby).applyConfiguredAttributes();
    }
}
