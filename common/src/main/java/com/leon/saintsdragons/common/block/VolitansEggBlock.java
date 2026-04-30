package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModBlockEntities;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class VolitansEggBlock extends AbstractTimedDragonEggBlock<VolitansEggBlockEntity> implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final int DEFAULT_HATCH_TICKS = 18000; // 15 minutes
    private static final VoxelShape SHAPE = box(4.0D, 0.0D, 4.0D, 12.0D, 10.0D, 12.0D);

    public VolitansEggBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean waterlogged = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return this.defaultBlockState().setValue(WATERLOGGED, waterlogged);
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                        @NotNull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        syncWaterloggedState(level, pos, state);
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction,
                                           @NotNull BlockState neighborState, @NotNull LevelAccessor level,
                                           @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        if (!state.getValue(WATERLOGGED) && level.getFluidState(pos).getType() == Fluids.WATER) {
            state = state.setValue(WATERLOGGED, true);
        }
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected boolean canProgressHatching(ServerLevel level, BlockPos pos, BlockState state, VolitansEggBlockEntity eggEntity) {
        if (syncWaterloggedState(level, pos, state)) {
            state = level.getBlockState(pos);
        }
        return state.getValue(WATERLOGGED);
    }

    @Override
    public boolean isHatchingPaused(Level level, BlockPos pos, BlockState state, AbstractDragonEggBlockEntity eggEntity) {
        if (syncWaterloggedState(level, pos, state)) {
            state = level.getBlockState(pos);
        }
        return !state.getValue(WATERLOGGED);
    }

    @Override
    protected ResourceLocation getDragonConfigId() {
        return DragonAttributeConfigLoader.VOLITANS_ID;
    }

    @Override
    protected int getDefaultNormalHatchTicks() {
        return DEFAULT_HATCH_TICKS;
    }

    @Override
    protected Supplier<BlockEntityType<VolitansEggBlockEntity>> getEggBlockEntityType() {
        return ModBlockEntities.VOLITANS_EGG;
    }

    @Override
    protected VolitansEggBlockEntity createEggBlockEntity(BlockPos pos, BlockState state) {
        return new VolitansEggBlockEntity(pos, state);
    }

    @Override
    protected DragonEntity createBaby(ServerLevel level) {
        return ModEntities.VOLITANS.get().create(level);
    }

    @Override
    protected void applyBabyAttributes(DragonEntity baby) {
        ((Volitans) baby).applyConfiguredAttributes();
    }

    private boolean syncWaterloggedState(LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.getBlock() != this || state.getValue(WATERLOGGED)) {
            return false;
        }
        if (level.getFluidState(pos).getType() != Fluids.WATER) {
            return false;
        }
        level.setBlock(pos, state.setValue(WATERLOGGED, true), 2);
        return true;
    }
}