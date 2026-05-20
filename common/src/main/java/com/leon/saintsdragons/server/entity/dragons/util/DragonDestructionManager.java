package com.leon.saintsdragons.server.entity.dragons.util;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public final class DragonDestructionManager {
    private DragonDestructionManager() {}
    private static final Map<BlockPos, Integer> blockMeltProgress = new HashMap<>();
    private static final Map<BlockPos, Long> lastExposureTick = new HashMap<>();
    private static final int FIRE_BREATH_FURNACE_BOOST_TICKS = 10;
    private static final int FIRE_BREATH_SMOKER_BOOST_TICKS = 20;
    private static final int FIRE_BREATH_BLAST_FURNACE_BOOST_TICKS = 20;
    private static final int FIRE_BREATH_FURNACE_LIT_TICKS = 40;
    private static final int FIRE_BODY_FURNACE_BOOST_TICKS = 4;
    private static final int FIRE_BODY_SMOKER_BOOST_TICKS = 6;
    private static final int FIRE_BODY_BLAST_FURNACE_BOOST_TICKS = 6;
    private static final int FIRE_BODY_FURNACE_LIT_TICKS = 20;
    private static final String COOKING_PROGRESS_FIELD = "cookingProgress";
    private static final String COOKING_TOTAL_FIELD = "cookingTotalTime";
    private static final String LIT_TIME_FIELD = "litTime";
    private static final String LIT_DURATION_FIELD = "litDuration";
    private static Field cookingProgressField;
    private static Field cookingTotalField;
    private static Field litTimeField;
    private static Field litDurationField;
    public static void applyFireBreathImpact(ServerLevel level,
                                             DragonEntity dragon,
                                             Vec3 impactPoint,
                                             double radius,
                                             float damage,
                                             int fireSeconds,
                                             int meltTicksRequired,
                                             boolean canMeltBlocks) {
        applyFireBreathImpact(level, dragon, impactPoint, radius, damage, fireSeconds, meltTicksRequired, canMeltBlocks, true);
    }

    public static void applyFireBreathImpact(ServerLevel level,
                                             DragonEntity dragon,
                                             Vec3 impactPoint,
                                             double radius,
                                             float damage,
                                             int fireSeconds,
                                             int meltTicksRequired,
                                             boolean canMeltBlocks,
                                             boolean igniteBlocks) {
        if (level == null || dragon == null || impactPoint == null) {
            return;
        }
        damageEntities(level, dragon, impactPoint, radius, damage, fireSeconds);
        if (igniteBlocks) {
            igniteBlocks(level, impactPoint, radius);
        }
        accelerateCooking(level, impactPoint, radius,
                FIRE_BREATH_FURNACE_BOOST_TICKS,
                FIRE_BREATH_SMOKER_BOOST_TICKS,
                FIRE_BREATH_BLAST_FURNACE_BOOST_TICKS,
                FIRE_BREATH_FURNACE_LIT_TICKS);

        if (canMeltBlocks && meltTicksRequired > 0) {
            double destructionRadius = radius * 0.6;
            meltBlocks(level, impactPoint, destructionRadius, meltTicksRequired);
        }
    }

    public static void applyFlameImpact(ServerLevel level, Vec3 impactPoint, double radius) {
        if (level == null || impactPoint == null) {
            return;
        }
        igniteBlocks(level, impactPoint, radius);
    }

    private static void damageEntities(ServerLevel level,
                                       DragonEntity dragon,
                                       Vec3 impactPoint,
                                       double radius,
                                       float damage,
                                       int fireSeconds) {
        AABB area = new AABB(impactPoint, impactPoint).inflate(radius);
        Set<LivingEntity> hit = new HashSet<>();
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e.isAlive() && e != dragon && !dragon.isAlly(e))) {

            if (hit.add(entity)) {
                entity.hurt(level.damageSources().dragonBreath(), damage);
                entity.setSecondsOnFire(fireSeconds);
            }
        }
    }

    private static void igniteBlocks(ServerLevel level, Vec3 impactPoint, double radius) {
        BlockPos center = BlockPos.containing(impactPoint);
        int r = (int) Math.ceil(radius);
        RandomSource random = level.random;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (random.nextFloat() > 0.6F) {
                continue;
            }
            double dx = pos.getX() + 0.5D - impactPoint.x;
            double dy = pos.getY() + 0.5D - impactPoint.y;
            double dz = pos.getZ() + 0.5D - impactPoint.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > radius * radius) {
                continue;
            }
            if (!level.isLoaded(pos)) {
                continue;
            }

            if (level.isEmptyBlock(pos)) {
                BlockState fire = Blocks.FIRE.defaultBlockState();
                if (fire.canSurvive(level, pos)) {
                    level.setBlock(pos, fire, 11);
                }
            } else if (random.nextFloat() < 0.35F) {
                BlockPos above = pos.above();
                if (level.isEmptyBlock(above) && Blocks.FIRE.defaultBlockState().canSurvive(level, above)) {
                    level.setBlock(above, Blocks.FIRE.defaultBlockState(), 11);
                }
            }
        }
    }

    public static void applyFireBodyCookingAura(ServerLevel level, Vec3 impactPoint, double radius) {
        accelerateCooking(level, impactPoint, radius,
                FIRE_BODY_FURNACE_BOOST_TICKS,
                FIRE_BODY_SMOKER_BOOST_TICKS,
                FIRE_BODY_BLAST_FURNACE_BOOST_TICKS,
                FIRE_BODY_FURNACE_LIT_TICKS);
    }

    private static void accelerateCooking(ServerLevel level,
                                          Vec3 impactPoint,
                                          double radius,
                                          int furnaceBoost,
                                          int smokerBoost,
                                          int blastBoost,
                                          int litTicks) {
        BlockPos center = BlockPos.containing(impactPoint);
        int r = (int) Math.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            double dx = pos.getX() + 0.5D - impactPoint.x;
            double dy = pos.getY() + 0.5D - impactPoint.y;
            double dz = pos.getZ() + 0.5D - impactPoint.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > radius * radius || !level.isLoaded(pos)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof AbstractFurnaceBlockEntity furnace)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.FURNACE) && !state.is(Blocks.SMOKER) && !state.is(Blocks.BLAST_FURNACE)) {
                continue;
            }

            int total = getCookingTotalTime(furnace);
            if (total <= 0) {
                continue;
            }

            boolean changed = false;
            int litTime = getLitTime(furnace);
            if (litTime < litTicks) {
                setLitTime(furnace, litTicks);
                setLitDuration(furnace, litTicks);
                changed = true;
            }
            if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), 3);
                changed = true;
            }

            int progress = getCookingProgress(furnace);
            int boostTicks = state.is(Blocks.SMOKER)
                    ? smokerBoost
                    : (state.is(Blocks.BLAST_FURNACE)
                        ? blastBoost
                        : furnaceBoost);
            int boosted = Math.min(total - 1, progress + boostTicks);
            if (boosted > progress) {
                setCookingProgress(furnace, boosted);
                changed = true;
            }
            if (changed) {
                blockEntity.setChanged();
            }
        }
    }

    private static int getCookingProgress(AbstractFurnaceBlockEntity furnace) {
        Field field = resolveCookingProgressField();
        if (field == null) {
            return 0;
        }
        try {
            return field.getInt(furnace);
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    private static int getCookingTotalTime(AbstractFurnaceBlockEntity furnace) {
        Field field = resolveCookingTotalField();
        if (field == null) {
            return 0;
        }
        try {
            return field.getInt(furnace);
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    private static void setCookingProgress(AbstractFurnaceBlockEntity furnace, int value) {
        Field field = resolveCookingProgressField();
        if (field == null) {
            return;
        }
        try {
            field.setInt(furnace, value);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static Field resolveCookingProgressField() {
        if (cookingProgressField != null) {
            return cookingProgressField;
        }
        cookingProgressField = resolveFurnaceField(COOKING_PROGRESS_FIELD);
        return cookingProgressField;
    }

    private static Field resolveCookingTotalField() {
        if (cookingTotalField != null) {
            return cookingTotalField;
        }
        cookingTotalField = resolveFurnaceField(COOKING_TOTAL_FIELD);
        return cookingTotalField;
    }

    private static int getLitTime(AbstractFurnaceBlockEntity furnace) {
        Field field = resolveLitTimeField();
        if (field == null) {
            return 0;
        }
        try {
            return field.getInt(furnace);
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    private static void setLitTime(AbstractFurnaceBlockEntity furnace, int value) {
        Field field = resolveLitTimeField();
        if (field == null) {
            return;
        }
        try {
            field.setInt(furnace, value);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static void setLitDuration(AbstractFurnaceBlockEntity furnace, int value) {
        Field field = resolveLitDurationField();
        if (field == null) {
            return;
        }
        try {
            field.setInt(furnace, value);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static Field resolveLitTimeField() {
        if (litTimeField != null) {
            return litTimeField;
        }
        litTimeField = resolveFurnaceField(LIT_TIME_FIELD);
        return litTimeField;
    }

    private static Field resolveLitDurationField() {
        if (litDurationField != null) {
            return litDurationField;
        }
        litDurationField = resolveFurnaceField(LIT_DURATION_FIELD);
        return litDurationField;
    }

    private static Field resolveFurnaceField(String name) {
        try {
            Field field = AbstractFurnaceBlockEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private static void meltBlocks(ServerLevel level, Vec3 impactPoint, double radius, int requiredTicks) {
        BlockPos center = BlockPos.containing(impactPoint);
        int r = (int) Math.ceil(radius);
        long currentTick = level.getGameTime();
        Set<BlockPos> exposedThisTick = new HashSet<>();

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            double dx = pos.getX() + 0.5D - impactPoint.x;
            double dy = pos.getY() + 0.5D - impactPoint.y;
            double dz = pos.getZ() + 0.5D - impactPoint.z;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > radius * radius || !level.isLoaded(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !canMeltBlock(level, pos, state)) {
                continue;
            }

            BlockPos immutablePos = pos.immutable();
            exposedThisTick.add(immutablePos);
            int progress = blockMeltProgress.getOrDefault(immutablePos, 0);
            progress++;
            blockMeltProgress.put(immutablePos, progress);
            lastExposureTick.put(immutablePos, currentTick);

            // Break block once it's been exposed long enough
            if (progress >= requiredTicks) {
                level.destroyBlock(immutablePos, true);
                blockMeltProgress.remove(immutablePos);
                lastExposureTick.remove(immutablePos);
            }
        }
        cleanupStaleProgress(currentTick);
    }
    private static void cleanupStaleProgress(long currentTick) {
        lastExposureTick.entrySet().removeIf(entry -> {
            long lastTick = entry.getValue();
            boolean isStale = (currentTick - lastTick) > 20;
            if (isStale) {
                blockMeltProgress.remove(entry.getKey());
            }
            return isStale;
        });
    }

    private static boolean canMeltBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        return state.getBlock() != Blocks.BEDROCK
                && state.getBlock() != Blocks.BARRIER
                && state.getBlock() != Blocks.COMMAND_BLOCK
                && state.getBlock() != Blocks.CHAIN_COMMAND_BLOCK
                && state.getBlock() != Blocks.REPEATING_COMMAND_BLOCK
                && state.getBlock() != Blocks.STRUCTURE_BLOCK
                && state.getBlock() != Blocks.JIGSAW
                && state.getBlock() != Blocks.END_PORTAL
                && state.getBlock() != Blocks.END_PORTAL_FRAME
                && state.getBlock() != Blocks.END_GATEWAY;
    }
}