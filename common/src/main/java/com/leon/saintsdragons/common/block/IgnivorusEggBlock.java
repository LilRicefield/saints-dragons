package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModBlockEntities;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class IgnivorusEggBlock extends AbstractTimedDragonEggBlock<IgnivorusEggBlockEntity> {
    private static final int DEFAULT_HATCH_TICKS = 36000; // 30 minutes
    private static final VoxelShape SHAPE = box(3.0D, 0.0D, 3.0D, 13.0D, 13.0D, 13.0D);

    public IgnivorusEggBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0));
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ResourceLocation getDragonConfigId() {
        return DragonAttributeConfigLoader.IGNIVORUS_ID;
    }

    @Override
    protected int getDefaultNormalHatchTicks() {
        return DEFAULT_HATCH_TICKS;
    }

    @Override
    protected Supplier<BlockEntityType<IgnivorusEggBlockEntity>> getEggBlockEntityType() {
        return ModBlockEntities.IGNIVORUS_EGG;
    }

    @Override
    protected IgnivorusEggBlockEntity createEggBlockEntity(BlockPos pos, BlockState state) {
        return new IgnivorusEggBlockEntity(pos, state);
    }

    @Override
    protected DragonEntity createBaby(ServerLevel level) {
        return ModEntities.IGNIVORUS.get().create(level);
    }

    @Override
    protected void applyBabyAttributes(DragonEntity baby) {
        ((Ignivorus) baby).applyConfiguredAttributes();
    }
}