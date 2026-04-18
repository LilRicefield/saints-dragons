package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
 * Raevyx egg block that hatches into a baby Raevyx over time.
 * Hatches faster during thunderstorms, instantly when struck by lightning.
 */
public class RaevyxEggBlock extends BaseEntityBlock {
    public static final int MAX_HATCH_LEVEL = 2;
    public static final IntegerProperty HATCH = BlockStateProperties.HATCH;
    private static final int DEFAULT_NORMAL_TOTAL_HATCH_TICKS = 18000; // 15 minutes
    private static final int DEFAULT_THUNDER_TOTAL_HATCH_TICKS = 9600; // 8 minutes

    // Egg shape (similar to turtle egg but slightly larger)
    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D);

    public RaevyxEggBlock(Properties properties) {
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

    private void incrementHatch(ServerLevel level, BlockPos pos, BlockState state) {
        int currentHatch = state.getValue(HATCH);

        if (currentHatch < MAX_HATCH_LEVEL) {
            // Play cracking sound
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
            level.setBlock(pos, state.setValue(HATCH, currentHatch + 1), 2);
        } else {
            // Hatch the egg
            this.hatchEgg(level, pos, state);
        }
    }

    /**
     * Instantly hatch the egg (called by lightning strike)
     */
    public void instantHatch(ServerLevel level, BlockPos pos, BlockState state) {
        this.hatchEgg(level, pos, state);
    }

    /**
     * Spawn baby Raevyx and remove egg block
     */
    private void hatchEgg(ServerLevel level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        // Play hatching sound and effects
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
        level.removeBlock(pos, false);

        // Spawn baby Raevyx
        Raevyx baby = ModEntities.RAEVYX.get().create(level);
        if (baby != null) {
            // Get block entity data if it exists
            if (blockEntity instanceof RaevyxEggBlockEntity eggEntity) {
                // Inherit parent data
                if (eggEntity.getOwnerUUID() != null) {
                    baby.setOwnerUUID(eggEntity.getOwnerUUID());
                    baby.setTame(true);
                }
                if (eggEntity.getBabyGender() != null) {
                    baby.setGender(eggEntity.getBabyGender());
                } else {
                    baby.setGender(level.random.nextBoolean() ? DragonGender.MALE : DragonGender.FEMALE);
                }
            } else {
                // Fallback: random gender
                baby.setGender(level.random.nextBoolean() ? DragonGender.MALE : DragonGender.FEMALE);
            }

            // Configure as baby
            baby.setAge(-24000); // 20 minutes until adult
            baby.setBaby(true);
            baby.skipRespawnTicks = 5;
            baby.applyConfiguredAttributes();
            baby.setHealth(baby.getMaxHealth());

            // Position at egg location
            baby.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);

            level.addFreshEntity(baby);
            if (baby.isTame() && baby.getOwnerUUID() != null) {
                DragonCodexSavedData.get(level).addDragon(baby.getOwnerUUID(), baby);
            }
            level.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(baby));
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        // Eggs can be trampled and destroyed (like turtle eggs)
        if (!level.isClientSide && entity instanceof Player player && !player.isShiftKeyDown()) {
            if (level.random.nextInt(10) == 0) {
                this.destroyEgg(level, state, pos);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Check for nearby lightning bolts every tick
        level.getEntitiesOfClass(LightningBolt.class,
            new net.minecraft.world.phys.AABB(pos).inflate(3.0D))
            .stream()
            .findFirst()
            .ifPresent(bolt -> this.instantHatch(level, pos, state));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                  @NotNull BlockState state,
                                                                  @NotNull BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(
                blockEntityType,
                com.leon.saintsdragons.common.registry.ModBlockEntities.RAEVYX_EGG.get(),
                this::serverTick
        );
    }

    private void serverTick(Level level, BlockPos pos, BlockState state, RaevyxEggBlockEntity eggEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.getEntitiesOfClass(LightningBolt.class,
                new net.minecraft.world.phys.AABB(pos).inflate(3.0D))
                .stream()
                .findFirst()
                .ifPresent(bolt -> this.instantHatch(serverLevel, pos, state));

        if (serverLevel.getBlockState(pos).getBlock() != this) {
            return;
        }

        int normalHatchTicks = resolveNormalHatchTicks();
        int thunderHatchTicks = resolveThunderHatchTicks(normalHatchTicks);
        double previousProgress = eggEntity.getHatchProgress();
        int previousStage = getHatchStageForProgress(previousProgress);
        double perTickProgress = serverLevel.isThundering()
                ? 1.0D / thunderHatchTicks
                : 1.0D / normalHatchTicks;
        double nextProgress = Math.min(1.0D, previousProgress + perTickProgress);
        eggEntity.setHatchProgress(nextProgress);

        int nextStage = getHatchStageForProgress(nextProgress);
        if (nextStage != previousStage && nextStage <= MAX_HATCH_LEVEL) {
            incrementHatch(serverLevel, pos, serverLevel.getBlockState(pos));
            return;
        }

        if (nextProgress >= 1.0D) {
            hatchEgg(serverLevel, pos, serverLevel.getBlockState(pos));
        }
    }

    @Override
    public void entityInside(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        // Lightning strike instantly hatches the egg
        if (!level.isClientSide && entity instanceof LightningBolt) {
            if (level instanceof ServerLevel serverLevel) {
                this.instantHatch(serverLevel, pos, state);
            }
        }
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, Level level, @NotNull BlockPos pos, net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean isMoving) {
        // Schedule a tick to check for lightning when neighbors change (like when lightning strikes nearby)
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    /**
     * Destroy the egg without spawning a baby
     */
    private void destroyEgg(Level level, BlockState state, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
        level.destroyBlock(pos, false);
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        // Comparator output based on hatch level (0-2)
        return state.getValue(HATCH);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new RaevyxEggBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    private int resolveNormalHatchTicks() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        double ticks = config.extraDouble("egg_hatch_time_ticks_normal", DEFAULT_NORMAL_TOTAL_HATCH_TICKS);
        return Math.max(1, (int) Math.round(ticks));
    }

    private int resolveThunderHatchTicks(int normalHatchTicks) {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        double ticks = config.extraDouble("egg_hatch_time_ticks_thunder", DEFAULT_THUNDER_TOTAL_HATCH_TICKS);
        return Math.max(1, Math.min(normalHatchTicks, (int) Math.round(ticks)));
    }

    private int getHatchStageForProgress(double progress) {
        return Math.min(MAX_HATCH_LEVEL, (int) Math.floor(progress * (MAX_HATCH_LEVEL + 1)));
    }
}
