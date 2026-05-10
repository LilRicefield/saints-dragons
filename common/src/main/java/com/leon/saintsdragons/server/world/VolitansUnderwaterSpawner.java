package com.leon.saintsdragons.server.world;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.world.DragonBiomeMatcher;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class VolitansUnderwaterSpawner {
    private static final TagKey<Biome> DEFAULT_VOLITANS_BIOME_TAG = TagKey.create(
            Registries.BIOME,
            SaintsDragonsCommon.rl("has_volitans")
    );

    private static final int CHECK_INTERVAL = 300;
    private static final int HORIZONTAL_RADIUS = 96;
    private static final int SEARCH_ATTEMPTS = 64;
    private static final int CLUSTER_SIZE = 128;
    private static final int NEARBY_SEARCH_RADIUS = 96;
    private static final int MIN_WATER_COLUMN_HEIGHT = 3;

    private static final Map<ResourceLocation, Integer> tickCounters = new HashMap<>();
    private static final Map<ResourceLocation, Set<Long>> activeClusters = new HashMap<>();

    private VolitansUnderwaterSpawner() {
    }

    public static void tick(ServerLevel level) {
        ResourceLocation dimensionId = level.dimension().location();
        int counter = tickCounters.getOrDefault(dimensionId, 0) + 1;
        tickCounters.put(dimensionId, counter);
        if (counter < CHECK_INTERVAL) {
            return;
        }
        tickCounters.put(dimensionId, 0);

        int weight = SaintsDragonsConfig.VOLITANS_SPAWN_WEIGHT.get();
        if (weight <= 0 || level.players().isEmpty()) {
            return;
        }

        int effectiveChance = Mth.clamp(weight, 0, 100);
        if (level.random.nextInt(100) >= effectiveChance) {
            return;
        }

        Player player = pickEligiblePlayer(level);
        if (player == null) {
            return;
        }

        BlockPos center = player.blockPosition();
        long clusterKey = getClusterKey(center);
        Set<Long> trackedClusters = activeClusters.computeIfAbsent(dimensionId, key -> new HashSet<>());
        if (trackedClusters.contains(clusterKey)) {
            if (hasVolitansNearby(level, center)) {
                return;
            }
            trackedClusters.remove(clusterKey);
        }

        BlockPos spawnPos = findSpawnPos(level, center, level.random);
        if (spawnPos == null) {
            return;
        }

        if (!DragonSpawnRules.passesNearbyDragonDensityCheck(level, MobSpawnType.NATURAL, spawnPos, Volitans.class)) {
            return;
        }

        if (spawnPack(level, spawnPos, level.random)) {
            trackedClusters.add(clusterKey);
        }
    }

    public static void clearTracking() {
        tickCounters.clear();
        activeClusters.clear();
    }

    private static Player pickEligiblePlayer(ServerLevel level) {
        var players = level.players().stream()
                .filter(player -> player.isAlive() && !player.isSpectator())
                .filter(player -> {
                    BlockPos pos = player.blockPosition();
                    boolean biomeAllowed = isVolitansBiomeAllowed(level.getBiome(pos));
                    boolean nearby = hasVolitansNearby(level, pos);
                    return biomeAllowed && !nearby;
                })
                .toList();
        if (players.isEmpty()) {
            return null;
        }
        return players.get(level.random.nextInt(players.size()));
    }

    private static BlockPos findSpawnPos(ServerLevel level, BlockPos center, RandomSource random) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        for (int attempt = 0; attempt < SEARCH_ATTEMPTS; attempt++) {
            int x = center.getX() + random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
            int z = center.getZ() + random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
            BlockPos pos = findUnderwaterSpawnPos(level, x, z, minY, maxY);
            if (pos == null) {
                continue;
            }
            if (!canFitVolitansAt(level, pos)) {
                continue;
            }
            return pos;
        }

        return null;
    }

    private static boolean spawnPack(ServerLevel level, BlockPos origin, RandomSource random) {
        int minGroupSize = SaintsDragonsConfig.VOLITANS_MIN_GROUP_SIZE.get();
        int maxGroupSize = SaintsDragonsConfig.VOLITANS_MAX_GROUP_SIZE.get();
        if (minGroupSize <= 0 || maxGroupSize <= 0) {
            return false;
        }
        if (minGroupSize > maxGroupSize) {
            minGroupSize = maxGroupSize;
        }

        int targetCount = Mth.nextInt(random, minGroupSize, maxGroupSize);
        int spawned = 0;

        if (spawnOne(level, origin, MobSpawnType.NATURAL)) {
            spawned++;
        } else {
            return false;
        }

        for (int i = 1; i < targetCount; i++) {
            BlockPos nearby = findNearbyPackPos(level, origin, random);
            if (nearby != null && spawnOne(level, nearby, MobSpawnType.EVENT)) {
                spawned++;
            }
        }

        return spawned > 0;
    }

    private static boolean spawnOne(ServerLevel level, BlockPos pos, MobSpawnType spawnType) {
        Volitans volitans = ModEntities.VOLITANS.get().create(level);
        if (volitans == null) {
            return false;
        }

        volitans.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(volitans, volitans.getBoundingBox())) {
            return false;
        }
        volitans.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), spawnType, null, null);
        if (!level.noCollision(volitans, volitans.getBoundingBox())) {
            return false;
        }
        return level.addFreshEntity(volitans);
    }

    private static BlockPos findNearbyPackPos(ServerLevel level, BlockPos origin, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int x = origin.getX() + random.nextInt(17) - 8;
            int z = origin.getZ() + random.nextInt(17) - 8;
            BlockPos pos = findUnderwaterSpawnPos(level, x, z, origin.getY() - 8, origin.getY() + 8);
            if (pos != null && canFitVolitansAt(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static BlockPos findUnderwaterSpawnPos(ServerLevel level, int x, int z, int minY, int maxY) {
        int startY = Mth.clamp(maxY, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
        int endY = Mth.clamp(minY, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);

        int fluidTop = Integer.MIN_VALUE;
        int fluidBottom = Integer.MIN_VALUE;
        boolean insideFluid = false;

        for (int y = startY; y >= endY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            boolean hasFluid = !level.getFluidState(pos).isEmpty();

            if (hasFluid) {
                if (!insideFluid) {
                    insideFluid = true;
                    fluidTop = y;
                    fluidBottom = y;
                } else {
                    fluidBottom = y;
                }
            } else if (insideFluid) {
                break;
            }
        }

        if (!insideFluid) {
            return null;
        }

        int columnHeight = fluidTop - fluidBottom + 1;
        if (columnHeight < MIN_WATER_COLUMN_HEIGHT) {
            return null;
        }

        int spawnY = fluidBottom;
        BlockPos spawnPos = new BlockPos(x, spawnY, z);
        if (!isVolitansBiomeAllowed(level.getBiome(spawnPos))) {
            return null;
        }

        for (int dy = 0; dy < MIN_WATER_COLUMN_HEIGHT; dy++) {
            BlockPos checkPos = spawnPos.above(dy);
            if (level.getFluidState(checkPos).isEmpty()) {
                return null;
            }
            if (!level.getBlockState(checkPos).getCollisionShape(level, checkPos).isEmpty()) {
                return null;
            }
        }

        return spawnPos;
    }

    private static boolean canFitVolitansAt(ServerLevel level, BlockPos pos) {
        Volitans probe = ModEntities.VOLITANS.get().create(level);
        if (probe == null) {
            return false;
        }
        probe.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        AABB box = probe.getBoundingBox();
        return level.noCollision(probe, box);
    }

    private static boolean hasVolitansNearby(ServerLevel level, BlockPos center) {
        return !level.getEntitiesOfClass(
                Volitans.class,
                new AABB(center).inflate(NEARBY_SEARCH_RADIUS),
                dragon -> dragon.isAlive() && !dragon.isTame()
        ).isEmpty();
    }

    private static long getClusterKey(BlockPos pos) {
        int x = Math.floorDiv(pos.getX(), CLUSTER_SIZE);
        int z = Math.floorDiv(pos.getZ(), CLUSTER_SIZE);
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private static boolean isVolitansBiomeAllowed(Holder<Biome> biome) {
        return DragonBiomeMatcher.isAllowed(
                biome,
                DEFAULT_VOLITANS_BIOME_TAG,
                SaintsDragonsConfig.VOLITANS_ADDITIONAL_BIOMES,
                SaintsDragonsConfig.VOLITANS_EXCLUDED_BIOMES
        );
    }
}