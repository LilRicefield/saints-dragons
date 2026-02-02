package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
 * Cindervane egg block that hatches into baby Cindervanes over time.
 * Supports clustering up to 3 eggs in the same block (like turtle eggs).
 * Instantly hatches when struck by lightning.
 */
public class CindervaneEggBlock extends BaseEntityBlock {
    public static final int MAX_HATCH_LEVEL = 2;
    public static final int MAX_EGGS = 3;
    public static final IntegerProperty HATCH = BlockStateProperties.HATCH;
    public static final IntegerProperty EGGS = IntegerProperty.create("eggs", 1, MAX_EGGS);

    // Hatching speeds (lower = faster)
    private static final int NORMAL_HATCH_CHANCE = 2;      // ~7 minutes total (1/2 per random tick)

    // Egg shapes for different counts (similar to turtle eggs but adjusted)
    private static final VoxelShape ONE_EGG_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D);
    private static final VoxelShape TWO_EGGS_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 10.0D, 14.0D);
    private static final VoxelShape THREE_EGGS_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 10.0D, 15.0D);

    public CindervaneEggBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HATCH, 0)
            .setValue(EGGS, 1));
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return switch (state.getValue(EGGS)) {
            case 2 -> TWO_EGGS_SHAPE;
            case 3 -> THREE_EGGS_SHAPE;
            default -> ONE_EGG_SHAPE;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH, EGGS);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existingState = context.getLevel().getBlockState(context.getClickedPos());

        // If there's already an egg here and it's not at max capacity, add to it
        if (existingState.is(this)) {
            int currentEggs = existingState.getValue(EGGS);
            if (currentEggs < MAX_EGGS) {
                return existingState.setValue(EGGS, currentEggs + 1);
            }
        }

        return this.defaultBlockState();
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        // Allow stacking eggs up to max
        return !useContext.isSecondaryUseActive()
            && useContext.getItemInHand().is(this.asItem())
            && state.getValue(EGGS) < MAX_EGGS;
    }

    @Override
    public void randomTick(@NotNull BlockState state, ServerLevel level, @NotNull BlockPos pos, RandomSource random) {
        int hatchChance = resolveHatchChance();

        if (random.nextInt(hatchChance) == 0) {
            this.incrementHatch(level, pos, state);
        }
    }

    /**
     * Increment the hatch level or spawn babies if fully hatched
     */
    private void incrementHatch(ServerLevel level, BlockPos pos, BlockState state) {
        int currentHatch = state.getValue(HATCH);

        if (currentHatch < MAX_HATCH_LEVEL) {
            // Play cracking sound
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
            level.setBlock(pos, state.setValue(HATCH, currentHatch + 1), 2);
        } else {
            // Hatch the eggs
            this.hatchEggs(level, pos, state);
        }
    }

    /**
     * Instantly hatch the eggs (called by lightning strike)
     */
    public void instantHatch(ServerLevel level, BlockPos pos, BlockState state) {
        this.hatchEggs(level, pos, state);
    }

    /**
     * Spawn baby Cindervanes based on egg count and remove egg block
     */
    private void hatchEggs(ServerLevel level, BlockPos pos, BlockState state) {
        int eggCount = state.getValue(EGGS);

        // Play hatching sound and effects
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);

        // Get block entity data before removing block
        BlockEntity blockEntity = level.getBlockEntity(pos);
        CindervaneEggBlockEntity eggEntity = null;
        if (blockEntity instanceof CindervaneEggBlockEntity) {
            eggEntity = (CindervaneEggBlockEntity) blockEntity;
        }

        level.removeBlock(pos, false);

        // Spawn the appropriate number of baby Cindervanes
        for (int i = 0; i < eggCount; i++) {
            Cindervane baby = ModEntities.CINDERVANE.get().create(level);
            if (baby != null) {
                // Inherit parent data from block entity
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
                    // Fallback: random gender
                    baby.setGender(level.random.nextBoolean() ? DragonGender.MALE : DragonGender.FEMALE);
                }

                // Configure as baby
                baby.setAge(-24000); // 20 minutes until adult
                baby.setBaby(true);
                baby.skipRespawnTicks = 5;
                baby.applyConfiguredAttributes();
                baby.setHealth(baby.getMaxHealth());

                // Position at egg location with slight offset for multiple babies
                double offsetX = (level.random.nextDouble() - 0.5D) * 0.5D;
                double offsetZ = (level.random.nextDouble() - 0.5D) * 0.5D;
                baby.moveTo(pos.getX() + 0.5D + offsetX, pos.getY(), pos.getZ() + 0.5D + offsetZ, 0.0F, 0.0F);

                level.addFreshEntity(baby);
                level.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(baby));
            }
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

    @Override
    public void entityInside(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        // Lightning strike instantly hatches the eggs
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
     * Destroy one egg or reduce the count
     */
    private void destroyEgg(Level level, BlockState state, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);

        int currentEggs = state.getValue(EGGS);
        if (currentEggs > 1) {
            // Reduce egg count by 1
            level.setBlock(pos, state.setValue(EGGS, currentEggs - 1), 2);
        } else {
            // Last egg destroyed
            level.destroyBlock(pos, false);
        }
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
        return new CindervaneEggBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    private int resolveHatchChance() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        double chance = config.extraDouble("egg_hatch_chance_normal", NORMAL_HATCH_CHANCE);
        return Math.max(1, (int) Math.round(chance));
    }
}
