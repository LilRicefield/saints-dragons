package com.leon.saintsdragons.server.world;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
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
    private static final int MIN_COMPETING_CREATURE_WEIGHT = 20;

    private DragonSpawnRules() {
    }

    public static boolean hasEstablishedCreaturePool(LevelAccessor level, MobSpawnType spawnType, BlockPos pos) {
        if (!isNaturalWildSpawn(spawnType)) return true;

        // Read the final biome pool here so Forge/Fabric biome modification order cannot
        // change eligibility. Our own spawns must not make an otherwise sparse pool qualify.
        EntityType<?> firstCompetitor = null;
        boolean multipleTypes = false;
        long competingWeight = 0;
        for (var entry : level.getBiome(pos).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap()) {
            int weight = entry.getWeight().asInt();
            if (weight <= 0 || entry.maxCount <= 0
                    || BuiltInRegistries.ENTITY_TYPE.getKey(entry.type).getNamespace().equals("saintsdragons")) continue;
            if (firstCompetitor == null) firstCompetitor = entry.type;
            else if (firstCompetitor != entry.type) multipleTypes = true;
            competingWeight += weight;
            if (multipleTypes && competingWeight >= MIN_COMPETING_CREATURE_WEIGHT) return true;
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

        AABB sameSpeciesBounds = AABB.ofSize(
                net.minecraft.world.phys.Vec3.atCenterOf(pos),
                SAME_SPECIES_RADIUS * 2.0D,
                SAME_SPECIES_RADIUS * 2.0D,
                SAME_SPECIES_RADIUS * 2.0D
        );
        int nearbySameSpecies = serverLevel.getEntitiesOfClass(dragonClass, sameSpeciesBounds, DragonEntity::isAlive).size();
        if (nearbySameSpecies > MAX_NEARBY_SAME_SPECIES) {
            return false;
        }

        AABB anyDragonBounds = AABB.ofSize(
                net.minecraft.world.phys.Vec3.atCenterOf(pos),
                ANY_DRAGON_RADIUS * 2.0D,
                ANY_DRAGON_RADIUS * 2.0D,
                ANY_DRAGON_RADIUS * 2.0D
        );
        int nearbyDragons = serverLevel.getEntitiesOfClass(DragonEntity.class, anyDragonBounds, dragon -> dragon.isAlive() && !dragon.isTame()).size();
        return nearbyDragons <= MAX_NEARBY_TOTAL_DRAGONS;
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
