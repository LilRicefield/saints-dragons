package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Nulljaw egg block that hatches into a baby Nulljaw over time.
 * Hatches in ~10 minutes under normal conditions.
 */
public class NulljawEggBlock extends BaseEntityBlock {
    public static final int MAX_HATCH_LEVEL = 2;
    public static final IntegerProperty HATCH = BlockStateProperties.HATCH;

    // Raevyx uses 2 (~7 minutes); 3 is ~10 minutes under the same tick conditions.
    private static final int NORMAL_HATCH_CHANCE = 3;

    private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 10.0D, 12.0D);

    public NulljawEggBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0));
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH);
    }

    @Override
    public void randomTick(@NotNull BlockState state, ServerLevel level, @NotNull BlockPos pos, RandomSource random) {
        int hatchChance = resolveHatchChance();

        if (random.nextInt(hatchChance) == 0) {
            this.incrementHatch(level, pos, state);
        }
    }

    private void incrementHatch(ServerLevel level, BlockPos pos, BlockState state) {
        int currentHatch = state.getValue(HATCH);

        if (currentHatch < MAX_HATCH_LEVEL) {
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
            level.setBlock(pos, state.setValue(HATCH, currentHatch + 1), 2);
        } else {
            this.hatchEgg(level, pos, state);
        }
    }

    public void instantHatch(ServerLevel level, BlockPos pos, BlockState state) {
        this.hatchEgg(level, pos, state);
    }

    private void hatchEgg(ServerLevel level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
        level.removeBlock(pos, false);

        Nulljaw baby = ModEntities.NULLJAW.get().create(level);
        if (baby != null) {
            if (blockEntity instanceof NulljawEggBlockEntity eggEntity) {
                if (eggEntity.getOwnerUUID() != null) {
                    baby.setOwnerUUID(eggEntity.getOwnerUUID());
                    baby.setTame(true);
                }
                if (eggEntity.getBabyGender() != null) {
                    baby.setGender(eggEntity.getBabyGender());
                }
            } else {
                baby.setGender(level.random.nextBoolean() ? DragonGender.MALE : DragonGender.FEMALE);
            }

            baby.setAge(-24000);
            baby.setBaby(true);
            baby.skipRespawnTicks = 5;
            baby.applyConfiguredAttributes();
            baby.setHealth(baby.getMaxHealth());
            baby.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);

            level.addFreshEntity(baby);
            if (baby.isTame() && baby.getOwnerUUID() != null) {
                DragonCodexSavedData.get(level).addDragon(baby.getOwnerUUID(), baby);
            }
            level.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(baby));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.getEntitiesOfClass(LightningBolt.class,
                new net.minecraft.world.phys.AABB(pos).inflate(3.0D))
                .stream()
                .findFirst()
                .ifPresent(bolt -> this.instantHatch(level, pos, state));
    }

    @Override
    public void entityInside(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        if (!level.isClientSide && entity instanceof LightningBolt) {
            if (level instanceof ServerLevel serverLevel) {
                this.instantHatch(serverLevel, pos, state);
            }
        }
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, Level level, @NotNull BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        return state.getValue(HATCH);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new NulljawEggBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    private int resolveHatchChance() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        double chance = config.extraDouble("egg_hatch_chance_normal", NORMAL_HATCH_CHANCE);
        return Math.max(1, (int) Math.round(chance));
    }
}
