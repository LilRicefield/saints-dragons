package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;


public abstract class AbstractTimedDragonEggBlock<E extends AbstractDragonEggBlockEntity> extends BaseEntityBlock {
    public static final int MAX_HATCH_LEVEL = 2;
    public static final IntegerProperty HATCH = BlockStateProperties.HATCH;

    protected AbstractTimedDragonEggBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                  @NotNull BlockState state,
                                                                  @NotNull BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(
                blockEntityType,
                getEggBlockEntityType().get(),
                this::serverTick
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return createEggBlockEntity(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        return state.getValue(HATCH);
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
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof AbstractDragonEggBlockEntity eggEntity) {
            eggEntity.setHatchAdvancementOwnerUUID(player.getUUID());
        }
    }

    private void serverTick(Level level, BlockPos pos, BlockState state, E eggEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getBlockState(pos).getBlock() != this) {
            return;
        }
        if (!canProgressHatching(serverLevel, pos, state, eggEntity)) {
            return;
        }

        double previousProgress = eggEntity.getHatchProgress();
        int previousStage = getHatchStageForProgress(previousProgress);
        double nextProgress = Math.min(1.0D, previousProgress + (1.0D / resolveNormalHatchTicks()));
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
            hatchEggs(serverLevel, pos, serverLevel.getBlockState(pos));
        }
    }

    protected boolean canProgressHatching(ServerLevel level, BlockPos pos, BlockState state, E eggEntity) {
        return true;
    }

    protected void spawnHatchingStartedParticles(ServerLevel level, BlockPos pos) {
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

    protected int getEggCount(BlockState state) {
        return 1;
    }

    protected void positionBaby(ServerLevel level, BlockPos pos, DragonEntity baby, int index, int totalEggs) {
        if (totalEggs > 1) {
            double offsetX = (level.random.nextDouble() - 0.5D) * 0.5D;
            double offsetZ = (level.random.nextDouble() - 0.5D) * 0.5D;
            baby.moveTo(pos.getX() + 0.5D + offsetX, pos.getY(), pos.getZ() + 0.5D + offsetZ, 0.0F, 0.0F);
            return;
        }
        baby.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
    }

    protected void incrementHatch(ServerLevel level, BlockPos pos, BlockState state) {
        int currentHatch = state.getValue(HATCH);
        if (currentHatch < MAX_HATCH_LEVEL) {
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS,
                    0.7F, 0.9F + level.random.nextFloat() * 0.2F);
            level.setBlock(pos, state.setValue(HATCH, currentHatch + 1), 2);
            return;
        }
        hatchEggs(level, pos, state);
    }

    protected void hatchEggs(ServerLevel level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        E eggEntity = getEggEntity(blockEntity);
        int eggCount = getEggCount(state);

        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS,
                0.7F, 0.9F + level.random.nextFloat() * 0.2F);
        level.removeBlock(pos, false);

        for (int i = 0; i < eggCount; i++) {
            DragonEntity baby = createBaby(level);
            if (baby == null) {
                continue;
            }

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
            applyBabyAttributes(baby);
            baby.setHealth(baby.getMaxHealth());
            positionBaby(level, pos, baby, i, eggCount);

            level.addFreshEntity(baby);
            if (baby.isTame() && baby.getOwnerUUID() != null) {
                DragonCodexSavedData.get(level).addDragon(baby.getOwnerUUID(), baby);
            }
            level.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(baby));
        }

        awardHatchAdvancement(level, pos, eggEntity);
    }

    protected void awardHatchAdvancement(ServerLevel level, BlockPos pos, @Nullable E eggEntity) {
        ResourceLocation advancementId = getHatchAdvancementId();
        if (advancementId == null) {
            return;
        }

        var advancement = level.getServer().getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            return;
        }

        UUID ownerUUID = eggEntity != null ? eggEntity.getHatchAdvancementOwnerUUID() : null;
        if (ownerUUID != null) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUUID);
            if (owner != null) {
                owner.getAdvancements().award(advancement, "hatch_dragon");
                return;
            }
        }

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 32.0D * 32.0D) {
                player.getAdvancements().award(advancement, "hatch_dragon");
            }
        }
    }

    @Nullable
    protected ResourceLocation getHatchAdvancementId() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    protected E getEggEntity(@Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof AbstractDragonEggBlockEntity eggEntity) {
            return (E) eggEntity;
        }
        return null;
    }

    protected int resolveNormalHatchTicks() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(getDragonConfigId());
        double ticks = config.extraDouble("egg_hatch_time_ticks_normal", getDefaultNormalHatchTicks());
        return Math.max(1, (int) Math.round(ticks));
    }

    public int getRemainingHatchTicks(Level level, BlockPos pos, BlockState state, AbstractDragonEggBlockEntity eggEntity) {
        return Math.max(0, (int) Math.ceil((1.0D - eggEntity.getHatchProgress()) * resolveNormalHatchTicks()));
    }

    public boolean isHatchingPaused(Level level, BlockPos pos, BlockState state, AbstractDragonEggBlockEntity eggEntity) {
        return false;
    }

    protected int getHatchStageForProgress(double progress) {
        return Math.min(MAX_HATCH_LEVEL, (int) Math.floor(progress * (MAX_HATCH_LEVEL + 1)));
    }

    protected abstract ResourceLocation getDragonConfigId();

    protected abstract int getDefaultNormalHatchTicks();

    protected abstract Supplier<BlockEntityType<E>> getEggBlockEntityType();

    protected abstract E createEggBlockEntity(BlockPos pos, BlockState state);

    @Nullable
    protected abstract DragonEntity createBaby(ServerLevel level);

    protected abstract void applyBabyAttributes(DragonEntity baby);
}
