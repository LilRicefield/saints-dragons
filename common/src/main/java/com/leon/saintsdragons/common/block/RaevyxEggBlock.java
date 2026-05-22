package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

public class RaevyxEggBlock extends BaseEntityBlock {
    public static final int MAX_HATCH_LEVEL = 2;
    public static final IntegerProperty HATCH = BlockStateProperties.HATCH;
    private static final int DEFAULT_NORMAL_TOTAL_HATCH_TICKS = 18000;
    private static final int DEFAULT_THUNDER_TOTAL_HATCH_TICKS = 9600;
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
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
            level.setBlock(pos, state.setValue(HATCH, currentHatch + 1), 2);
        } else {
            this.hatchEgg(level, pos, state);
        }
    }

    public void instantHatch(ServerLevel level, BlockPos pos, BlockState state) {
        this.hatchEgg(level, pos, state);
    }

    private void instantHatchFromLightning(ServerLevel level, BlockPos pos, BlockState state) {
        RaevyxEggBlockEntity eggEntity = getEggEntity(level.getBlockEntity(pos));
        this.instantHatch(level, pos, state);
        awardLightningHatchAdvancement(level, pos, eggEntity);
    }


    private void hatchEgg(ServerLevel level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        RaevyxEggBlockEntity eggEntity = getEggEntity(blockEntity);
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
        level.removeBlock(pos, false);
        Raevyx baby = ModEntities.RAEVYX.get().create(level);
        if (baby != null) {
            if (eggEntity != null) {
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
        awardHatchAdvancement(level, pos, eggEntity);
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
    public void tick(@NotNull BlockState state, ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        level.getEntitiesOfClass(LightningBolt.class,
            new net.minecraft.world.phys.AABB(pos).inflate(3.0D))
            .stream()
            .findFirst()
            .ifPresent(bolt -> this.instantHatchFromLightning(level, pos, state));
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
                .ifPresent(bolt -> this.instantHatchFromLightning(serverLevel, pos, state));

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
        if (previousProgress <= 0.0D && nextProgress > 0.0D) {
            spawnHatchingStartedParticles(serverLevel, pos);
        }

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
                this.instantHatchFromLightning(serverLevel, pos, state);
            }
        }
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    private void destroyEgg(Level level, BlockState state, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
        level.destroyBlock(pos, false);
    }

    private void spawnHatchingStartedParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D,
                pos.getY() + 0.85D,
                pos.getZ() + 0.5D,
                8,
                0.28D,
                0.18D,
                0.28D,
                0.02D
        );
    }

    @Nullable
    private RaevyxEggBlockEntity getEggEntity(@Nullable BlockEntity blockEntity) {
        return blockEntity instanceof RaevyxEggBlockEntity eggEntity ? eggEntity : null;
    }

    private void awardHatchAdvancement(ServerLevel level, BlockPos pos, @Nullable RaevyxEggBlockEntity eggEntity) {
        awardAdvancement(level, pos, eggEntity, SaintsDragonsCommon.rl("hatch_raevyx"), "hatch_dragon");
    }

    private void awardLightningHatchAdvancement(ServerLevel level, BlockPos pos, @Nullable RaevyxEggBlockEntity eggEntity) {
        awardAdvancement(level, pos, eggEntity, SaintsDragonsCommon.rl("raevyx_lightning_hatch"), "raevyx_lightning_hatch");
    }

    private void awardAdvancement(ServerLevel level,
                                  BlockPos pos,
                                  @Nullable RaevyxEggBlockEntity eggEntity,
                                  ResourceLocation advancementId,
                                  String criterion) {
        var advancement = level.getServer().getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            return;
        }

        if (eggEntity != null && eggEntity.getHatchAdvancementOwnerUUID() != null) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(eggEntity.getHatchAdvancementOwnerUUID());
            if (owner != null) {
                owner.getAdvancements().award(advancement, criterion);
                return;
            }
        }

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 32.0D * 32.0D) {
                player.getAdvancements().award(advancement, criterion);
            }
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
        return new RaevyxEggBlockEntity(pos, state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(@NotNull Level level,
                            @NotNull BlockPos pos,
                            @NotNull BlockState state,
                            @Nullable LivingEntity placer,
                            @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof RaevyxEggBlockEntity eggEntity) {
            eggEntity.setHatchAdvancementOwnerUUID(player.getUUID());
        }
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

    public int getRemainingHatchTicks(Level level, @Nullable RaevyxEggBlockEntity eggEntity) {
        if (eggEntity == null) {
            return resolveNormalHatchTicks();
        }
        int normalHatchTicks = resolveNormalHatchTicks();
        int activeHatchTicks = level.isThundering()
                ? resolveThunderHatchTicks(normalHatchTicks)
                : normalHatchTicks;
        return Math.max(0, (int) Math.ceil((1.0D - eggEntity.getHatchProgress()) * activeHatchTicks));
    }

    private int getHatchStageForProgress(double progress) {
        return Math.min(MAX_HATCH_LEVEL, (int) Math.floor(progress * (MAX_HATCH_LEVEL + 1)));
    }
}
