package com.leon.saintsdragons.server.world;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

public final class DragonSpawnRules {
    private static final double SAME_SPECIES_RADIUS = 96.0D;
    private static final double ANY_DRAGON_RADIUS = 160.0D;
    private static final int MAX_NEARBY_SAME_SPECIES = 0;
    private static final int MAX_NEARBY_TOTAL_DRAGONS = 2;
    private static final int MIN_COMPETING_CREATURE_WEIGHT = 5;
    private static final double CINDERVANE_SAME_SPECIES_RADIUS = 64.0D;
    private static final double CINDERVANE_ANY_DRAGON_RADIUS = 128.0D;
    private static final int CINDERVANE_SAME_SPECIES_LIMIT = 2;
    private static final int CINDERVANE_TOTAL_DRAGON_LIMIT = 4;

    private DragonSpawnRules() {
    }

    public static boolean hasEstablishedCreaturePool(LevelAccessor level, MobSpawnType spawnType, BlockPos pos) {
        if (!isNaturalWildSpawn(spawnType)) return true;

        // Read the final biome pool here so Forge/Fabric biome modification order cannot
        // change eligibility. Our own spawns must not make an otherwise sparse pool qualify.
        long competingWeight = 0;
        for (var entry : level.getBiome(pos).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap()) {
            int weight = entry.getWeight().asInt();
            if (weight <= 0 || entry.maxCount <= 0
                    || BuiltInRegistries.ENTITY_TYPE.getKey(entry.type).getNamespace().equals("saintsdragons")) continue;
            competingWeight += weight;
            if (competingWeight >= MIN_COMPETING_CREATURE_WEIGHT) return true;
        }
        return false;
    }

    public static boolean hasDryGroundSpawnSpace(LevelAccessor level, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }
        if (!level.getFluidState(below).isEmpty()) {
            return false;
        }

        boolean solidGround = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        boolean feetFree = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        boolean headFree = level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
        return solidGround && feetFree && headFree;
    }

    public static boolean hasCaveGroundSpawnSpace(LevelAccessor level, BlockPos pos) {
        return hasDryGroundSpawnSpace(level, pos) && !level.canSeeSky(pos);
    }

    public static boolean passesNearbyDragonDensityCheck(LevelAccessor level,
                                                         MobSpawnType spawnType,
                                                         BlockPos pos,
                                                         Class<? extends DragonEntity> dragonClass) {
        if (!(level instanceof ServerLevelAccessor serverLevelAccessor)) {
            return true;
        }

        if (!isNaturalWildSpawn(spawnType)) {
            return true;
        }

        var serverLevel = serverLevelAccessor.getLevel();
        if (!serverLevel.getServer().isSameThread()) {
            // C2ME may evaluate chunk-generation spawn rules in parallel. Live entity
            // density queries are server-thread-only, so defer the advisory limit here.
            return true;
        }

        boolean cindervane = dragonClass == Cindervane.class;
        double sameSpeciesRadius = cindervane ? CINDERVANE_SAME_SPECIES_RADIUS : SAME_SPECIES_RADIUS;
        double anyDragonRadius = cindervane ? CINDERVANE_ANY_DRAGON_RADIUS : ANY_DRAGON_RADIUS;
        int sameSpeciesLimit = cindervane ? CINDERVANE_SAME_SPECIES_LIMIT : MAX_NEARBY_SAME_SPECIES + 1;
        int totalDragonLimit = cindervane ? CINDERVANE_TOTAL_DRAGON_LIMIT : MAX_NEARBY_TOTAL_DRAGONS + 1;
        AABB sameSpeciesBounds = AABB.ofSize(
                net.minecraft.world.phys.Vec3.atCenterOf(pos),
                sameSpeciesRadius * 2.0D,
                sameSpeciesRadius * 2.0D,
                sameSpeciesRadius * 2.0D
        );
        int nearbySameSpecies = serverLevel.getEntitiesOfClass(dragonClass, sameSpeciesBounds,
                dragon -> dragon.isAlive() && (!cindervane || !dragon.isTame())).size();
        if (nearbySameSpecies >= sameSpeciesLimit) {
            return false;
        }

        AABB anyDragonBounds = AABB.ofSize(
                net.minecraft.world.phys.Vec3.atCenterOf(pos),
                anyDragonRadius * 2.0D,
                anyDragonRadius * 2.0D,
                anyDragonRadius * 2.0D
        );
        int nearbyDragons = serverLevel.getEntitiesOfClass(DragonEntity.class, anyDragonBounds, dragon -> dragon.isAlive() && !dragon.isTame()).size();
        return nearbyDragons < totalDragonLimit;
    }

    public static boolean isNaturalWildSpawn(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION;
    }

    public static boolean isThundering(LevelAccessor level) {
        if (!(level instanceof ServerLevelAccessor serverLevelAccessor)) {
            return false;
        }
        return serverLevelAccessor.getLevel().isThundering();
    }
}
