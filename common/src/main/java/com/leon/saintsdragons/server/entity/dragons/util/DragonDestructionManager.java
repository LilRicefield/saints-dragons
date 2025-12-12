package com.leon.saintsdragons.server.entity.dragons.util;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Shared destructive helpers for dragon abilities (fire breath, etc.).
 */
public final class DragonDestructionManager {
    private DragonDestructionManager() {}

    /**
     * Tracks how long each block has been exposed to melting (in ticks).
     * Blocks need sustained exposure to melt, creating a gradual destruction effect.
     */
    private static final Map<BlockPos, Integer> blockMeltProgress = new HashMap<>();

    /**
     * Tracks the last game tick each block was exposed to fire.
     * Used to decay progress if fire stops hitting a block.
     */
    private static final Map<BlockPos, Long> lastExposureTick = new HashMap<>();
    /**
     * Applies fire breath impact with optional block destruction.
     *
     * @param level The server level
     * @param dragon The dragon using the ability
     * @param impactPoint The center point of the impact
     * @param radius The radius of effect for damage/fire
     * @param damage Damage to deal to entities
     * @param fireSeconds How long to set entities on fire
     * @param meltTicksRequired Ticks of continuous per-block exposure needed to melt a block
     * @param canMeltBlocks Whether blocks can be melted (ability must be active long enough)
     */
    public static void applyFireBreathImpact(ServerLevel level,
                                             DragonEntity dragon,
                                             Vec3 impactPoint,
                                             double radius,
                                             float damage,
                                             int fireSeconds,
                                             int meltTicksRequired,
                                             boolean canMeltBlocks) {
        if (level == null || dragon == null || impactPoint == null) {
            return;
        }
        damageEntities(level, dragon, impactPoint, radius, damage, fireSeconds);
        igniteBlocks(level, impactPoint, radius);

        if (canMeltBlocks && meltTicksRequired > 0) {
            // Use smaller radius for block destruction to reduce lag
            double destructionRadius = radius * 0.4;  // 40% of fire radius
            meltBlocks(level, impactPoint, destructionRadius, meltTicksRequired);
        }
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
            if (random.nextFloat() > 0.35F) {
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
            } else if (random.nextFloat() < 0.15F) {
                BlockPos above = pos.above();
                if (level.isEmptyBlock(above) && Blocks.FIRE.defaultBlockState().canSurvive(level, above)) {
                    level.setBlock(above, Blocks.FIRE.defaultBlockState(), 11);
                }
            }
        }
    }

    /**
     * Gradually melts blocks that are continuously exposed to fire.
     * Each block needs to be hit for the required number of ticks before it breaks.
     * This creates a realistic melting effect rather than instant destruction.
     *
     * @param level The server level
     * @param impactPoint Center of the destruction area
     * @param radius Radius of effect (smaller than fire radius to reduce lag)
     * @param requiredTicks Ticks of continuous exposure needed to melt each block
     */
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
            if (state.isAir() || !canMeltBlock(state)) {
                continue;
            }

            BlockPos immutablePos = pos.immutable();
            exposedThisTick.add(immutablePos);

            // Track exposure progress
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

        // Cleanup: decay progress for blocks not exposed this tick
        cleanupStaleProgress(currentTick);
    }

    /**
     * Removes progress from blocks that haven't been exposed recently.
     * This prevents blocks from slowly accumulating damage over multiple uses.
     */
    private static void cleanupStaleProgress(long currentTick) {
        lastExposureTick.entrySet().removeIf(entry -> {
            long lastTick = entry.getValue();
            // If not exposed for 20 ticks (1 second), clear progress
            boolean isStale = (currentTick - lastTick) > 20;
            if (isStale) {
                blockMeltProgress.remove(entry.getKey());
            }
            return isStale;
        });
    }

    /**
     * Determines if a block can be melted/destroyed by dragon fire.
     * Blocks like bedrock, barriers, command blocks, and end portals cannot be destroyed.
     *
     * @param state The block state to check
     * @return true if the block can be melted, false otherwise
     */
    private static boolean canMeltBlock(BlockState state) {
        // Prevent destroying indestructible blocks
        if (state.getDestroySpeed(null, null) < 0) {
            return false;
        }

        // Additional safety checks for specific blocks
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
