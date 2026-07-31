package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModBlockEntities;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
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

public class AtroxiiaEggBlock extends AbstractTimedDragonEggBlock<AtroxiiaEggBlockEntity> {
    private static final int DEFAULT_HATCH_TICKS = 24000; // 20 minutes
    private static final VoxelShape EGG_SHAPE = box(5.0D, 0.0D, 5.0D, 11.0D, 8.0D, 11.0D);

    public AtroxiiaEggBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HATCH, 0));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return EGG_SHAPE;
    }

    @Override
    protected ResourceLocation getDragonConfigId() {
        return DragonAttributeConfigLoader.ATROXIIA_ID;
    }

    @Override
    protected int getDefaultNormalHatchTicks() {
        return DEFAULT_HATCH_TICKS;
    }

    @Override
    protected Supplier<BlockEntityType<AtroxiiaEggBlockEntity>> getEggBlockEntityType() {
        return ModBlockEntities.ATROXIIA_EGG;
    }

    @Override
    protected AtroxiiaEggBlockEntity createEggBlockEntity(BlockPos pos, BlockState state) {
        return new AtroxiiaEggBlockEntity(pos, state);
    }

    @Override
    protected DragonEntity createBaby(ServerLevel level) {
        return ModEntities.ATROXIIA.get().create(level);
    }

    @Override
    protected void applyBabyAttributes(DragonEntity baby) {
        ((Atroxiia) baby).applyConfiguredAttributes();
    }

    @Override
    protected ResourceLocation getHatchAdvancementId() {
        return SaintsDragonsCommon.rl("hatch_atroxiia");
    }
}
