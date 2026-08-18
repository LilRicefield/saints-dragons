package com.leon.saintsdragons.server.world;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.common.world.DragonBiomeMatcher;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class RaevyxStormSpawner {
    private static final int CHECK_INTERVAL = 120;
    private static final int HORIZONTAL_RADIUS = 96;
    private static final int SEARCH_ATTEMPTS = 24;
    private static final int CLUSTER_SIZE = 160;
    private static final int NEARBY_SEARCH_RADIUS = 128;

    private static final Map<ResourceLocation, Integer> tickCounters = new HashMap<>();
    private static final Map<ResourceLocation, Set<Long>> activeClusters = new HashMap<>();

    private RaevyxStormSpawner() {
    }

    public static void tick(ServerLevel level) {
        ResourceLocation dimensionId = level.dimension().location();
        int counter = tickCounters.getOrDefault(dimensionId, 0) + 1;
        tickCounters.put(dimensionId, counter);
        if (counter < CHECK_INTERVAL) {
            return;
        }
        tickCounters.put(dimensionId, 0);

        int weight = SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.get();
        if (weight <= 0 || level.players().isEmpty() || !level.isThundering()) {
            return;
        }

        // Stronger than vanilla creature spawning, but still conservative.
        int effectiveChance = Mth.clamp(weight * 2, 0, 100);
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
            if (hasRaevyxNearby(level, center)) {
                return;
            }
            trackedClusters.remove(clusterKey);
        }

        BlockPos spawnPos = findSpawnPos(level, center, level.random);
        if (spawnPos == null) {
            return;
        }

        if (!DragonSpawnRules.passesNearbyDragonDensityCheck(level, MobSpawnType.NATURAL, spawnPos, Raevyx.class)) {
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
                    return isRaevyxBiomeAllowed(level.getBiome(pos))
                            && level.canSeeSky(pos)
                            && !hasRaevyxNearby(level, pos);
                })
                .toList();
        if (players.isEmpty()) {
            return null;
        }
        return players.get(level.random.nextInt(players.size()));
    }

    private static BlockPos findSpawnPos(ServerLevel level, BlockPos center, RandomSource random) {
        for (int attempt = 0; attempt < SEARCH_ATTEMPTS; attempt++) {
            int x = center.getX() + random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
            int z = center.getZ() + random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);

            if (!isRaevyxBiomeAllowed(level.getBiome(pos))) {
                continue;
            }
            if (!level.canSeeSky(pos)) {
                continue;
            }
            if (!Raevyx.canSpawnHere(ModEntities.RAEVYX.get(), level, MobSpawnType.NATURAL, pos, random)) {
                continue;
            }
            if (!canFitRaevyxAt(level, pos)) {
                continue;
            }
            return pos;
        }
        return null;
    }

    private static boolean spawnPack(ServerLevel level, BlockPos origin, RandomSource random) {
        int minGroupSize = SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.get();
        int maxGroupSize = SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.get();
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
        Raevyx raevyx = ModEntities.RAEVYX.get().create(level);
        if (raevyx == null) {
            return false;
        }

        raevyx.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(raevyx, raevyx.getBoundingBox())) {
            return false;
        }
        raevyx.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), spawnType, null, null);
        if (!level.noCollision(raevyx, raevyx.getBoundingBox())) {
            return false;
        }
        return level.addFreshEntity(raevyx);
    }

    private static BlockPos findNearbyPackPos(ServerLevel level, BlockPos origin, RandomSource random) {
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = origin.getX() + random.nextInt(17) - 8;
            int z = origin.getZ() + random.nextInt(17) - 8;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.canSeeSky(pos)) {
                continue;
            }
            if (Raevyx.canSpawnHere(ModEntities.RAEVYX.get(), level, MobSpawnType.EVENT, pos, random)
                    && canFitRaevyxAt(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static boolean canFitRaevyxAt(ServerLevel level, BlockPos pos) {
        Raevyx probe = ModEntities.RAEVYX.get().create(level);
        if (probe == null) {
            return false;
        }
        probe.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        AABB box = probe.getBoundingBox();
        return level.noCollision(probe, box);
    }

    private static boolean hasRaevyxNearby(ServerLevel level, BlockPos center) {
        return !level.getEntitiesOfClass(
                Raevyx.class,
                new AABB(center).inflate(NEARBY_SEARCH_RADIUS),
                dragon -> dragon.isAlive() && !dragon.isTame()
        ).isEmpty();
    }

    private static long getClusterKey(BlockPos pos) {
        int x = Math.floorDiv(pos.getX(), CLUSTER_SIZE);
        int z = Math.floorDiv(pos.getZ(), CLUSTER_SIZE);
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private static boolean isRaevyxBiomeAllowed(Holder<Biome> biome) {
        return DragonBiomeMatcher.isAllowed(biome, ModTags.Biomes.HAS_RAEVYX);
    }
}
