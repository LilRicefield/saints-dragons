package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModBlockEntities;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
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

/**
 * Varasuchus egg block that hatches into a baby Varasuchus over time.
 */
public class VarasuchusEggBlock extends AbstractTimedDragonEggBlock<VarasuchusEggBlockEntity> {
    private static final int DEFAULT_HATCH_TICKS = 24000; // 20 minutes
    private static final VoxelShape SHAPE = box(4.0D, 0.0D, 4.0D, 12.0D, 10.0D, 12.0D);

    public VarasuchusEggBlock(Properties properties) {
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
        return DragonAttributeConfigLoader.VARASUCHUS_ID;
    }

    @Override
    protected int getDefaultNormalHatchTicks() {
        return DEFAULT_HATCH_TICKS;
    }

    @Override
    protected Supplier<BlockEntityType<VarasuchusEggBlockEntity>> getEggBlockEntityType() {
        return ModBlockEntities.VARASUCHUS_EGG;
    }

    @Override
    protected VarasuchusEggBlockEntity createEggBlockEntity(BlockPos pos, BlockState state) {
        return new VarasuchusEggBlockEntity(pos, state);
    }

    @Override
    protected DragonEntity createBaby(ServerLevel level) {
        return ModEntities.VARASUCHUS.get().create(level);
    }

    @Override
    protected void applyBabyAttributes(DragonEntity baby) {
        ((Varasuchus) baby).applyConfiguredAttributes();
    }
}
