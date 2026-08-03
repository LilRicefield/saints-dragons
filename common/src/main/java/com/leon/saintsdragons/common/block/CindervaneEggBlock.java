package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModBlockEntities;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Supplier;


public class CindervaneEggBlock extends AbstractTimedDragonEggBlock<CindervaneEggBlockEntity> {
    public static final int MAX_EGGS = 3;
    public static final IntegerProperty EGGS = IntegerProperty.create("eggs", 1, MAX_EGGS);
    private static final int DEFAULT_HATCH_TICKS = 12000; // 10 minutes
    private static final float MALE_PIEBALD_HATCH_CHANCE = 0.15F;
    private static final float FEMALE_PIEBALD_HATCH_CHANCE = 0.08F;

    private static final VoxelShape ONE_EGG_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D);
    private static final VoxelShape TWO_EGGS_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 10.0D, 14.0D);
    private static final VoxelShape THREE_EGGS_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 10.0D, 15.0D);

    public CindervaneEggBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0).setValue(EGGS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EGGS);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return switch (state.getValue(EGGS)) {
            case 2 -> TWO_EGGS_SHAPE;
            case 3 -> THREE_EGGS_SHAPE;
            default -> ONE_EGG_SHAPE;
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existingState = context.getLevel().getBlockState(context.getClickedPos());
        if (existingState.is(this)) {
            int currentEggs = existingState.getValue(EGGS);
            if (currentEggs < MAX_EGGS) {
                return existingState.setValue(EGGS, currentEggs + 1);
            }
        }
        return this.defaultBlockState();
    }

    @Override
    public boolean canBeReplaced(@NotNull BlockState state, BlockPlaceContext useContext) {
        return !useContext.isSecondaryUseActive()
                && useContext.getItemInHand().is(this.asItem())
                && state.getValue(EGGS) < MAX_EGGS;
    }

    @Override
    public void stepOn(Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Entity entity) {
        if (!level.isClientSide && entity instanceof Player player && !player.isShiftKeyDown()) {
            if (level.random.nextInt(10) == 0) {
                this.destroyEgg(level, state, pos);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected int getEggCount(BlockState state) {
        return state.getValue(EGGS);
    }

    private void destroyEgg(Level level, BlockState state, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS,
                0.7F, 0.9F + level.random.nextFloat() * 0.2F);
        int currentEggs = state.getValue(EGGS);
        if (currentEggs > 1) {
            level.setBlock(pos, state.setValue(EGGS, currentEggs - 1), 2);
        } else {
            level.destroyBlock(pos, false);
        }
    }

    @Override
    protected ResourceLocation getDragonConfigId() {
        return DragonAttributeConfigLoader.CINDERVANE_ID;
    }

    @Override
    protected int getDefaultNormalHatchTicks() {
        return DEFAULT_HATCH_TICKS;
    }

    @Override
    protected Supplier<BlockEntityType<CindervaneEggBlockEntity>> getEggBlockEntityType() {
        return ModBlockEntities.CINDERVANE_EGG;
    }

    @Override
    protected CindervaneEggBlockEntity createEggBlockEntity(BlockPos pos, BlockState state) {
        return new CindervaneEggBlockEntity(pos, state);
    }

    @Override
    protected DragonEntity createBaby(ServerLevel level) {
        return ModEntities.CINDERVANE.get().create(level);
    }

    @Override
    protected void applyBabyAttributes(DragonEntity baby) {
        ((Cindervane) baby).applyConfiguredAttributes();
    }

    @Override
    protected void configureHatchedBabyVariant(ServerLevel level,
                                               BlockPos pos,
                                               CindervaneEggBlockEntity eggEntity,
                                               DragonEntity baby) {
        float piebaldChance = baby.getGender() == DragonGender.FEMALE
                ? FEMALE_PIEBALD_HATCH_CHANCE
                : MALE_PIEBALD_HATCH_CHANCE;
        if (level.random.nextFloat() < piebaldChance) {
            baby.setPendingAdultTextureVariant(Cindervane.VARIANT_PIEBALD);
        }
    }

    @Override
    protected ResourceLocation getHatchAdvancementId() {
        return SaintsDragonsCommon.rl("hatch_cindervane");
    }
}
