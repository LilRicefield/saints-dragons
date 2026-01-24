package com.leon.saintsdragons.server.world;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles spawning Ivy the Dragon Merchant in villages.
 *
 * Call {@link #tick(ServerLevel)} from your world tick event to spawn Ivy in villages
 * that don't have her yet.
 */
public class VillageIvySpawner {
    // Track which villages already have Ivy (per world session)
    // Format: "x,z" of village center
    private static final Set<String> villagesWithIvy = new HashSet<>();

    private static final int CHECK_INTERVAL = 600; // Check every 30 seconds
    private static final int VILLAGE_RADIUS = 128; // How far from player to search for villages
    private static final int IVY_SEARCH_RADIUS = 48; // How far to search for existing Ivy

    private static int tickCounter = 0;

    /**
     * Tick the spawner. Call this from your world tick event.
     */
    public static void tick(ServerLevel level) {
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        // Only spawn in overworld
        if (level.dimension() != ServerLevel.OVERWORLD) {
            return;
        }

        // Find villages and spawn Ivy if needed
        trySpawnIvyInVillages(level);
    }

    private static void trySpawnIvyInVillages(ServerLevel level) {
        // Check villages near random players to avoid scanning entire world
        if (level.players().isEmpty()) {
            return;
        }

        // Pick a random player to check villages near
        var player = level.players().get(level.random.nextInt(level.players().size()));
        BlockPos playerPos = player.blockPosition();

        PoiManager poiManager = level.getPoiManager();

        // Find meeting points (village centers) within reasonable range of player
        poiManager.getInRange(
            poi -> poi.is(PoiTypes.MEETING),
            playerPos,
            VILLAGE_RADIUS * 2, // Search radius around player
            PoiManager.Occupancy.ANY
        ).forEach(poi -> {
            BlockPos villageCenter = poi.getPos();
            String villageKey = villageCenter.getX() + "," + villageCenter.getZ();

            // Skip if we already spawned Ivy here this session
            if (villagesWithIvy.contains(villageKey)) {
                return;
            }

            // Check if Ivy already exists nearby
            if (hasIvyNearby(level, villageCenter)) {
                villagesWithIvy.add(villageKey);
                return;
            }

            // Spawn Ivy near the village center
            spawnIvy(level, villageCenter);
            villagesWithIvy.add(villageKey);
        });
    }

    private static boolean hasIvyNearby(ServerLevel level, BlockPos center) {
        List<IvyTheDragonMerchant> nearbyIvys = level.getEntitiesOfClass(
            IvyTheDragonMerchant.class,
            new net.minecraft.world.phys.AABB(center).inflate(IVY_SEARCH_RADIUS),
            ivy -> ivy.isAlive()
        );
        return !nearbyIvys.isEmpty();
    }

    private static void spawnIvy(ServerLevel level, BlockPos villageCenter) {
        // Find a safe spawn position near the village center
        BlockPos spawnPos = findSafeSpawnPos(level, villageCenter);
        if (spawnPos == null) {
            return; // Couldn't find safe spot
        }

        // Spawn Ivy
        IvyTheDragonMerchant ivy = ModEntities.IVY_THE_DRAGON_MERCHANT.get().create(level);
        if (ivy == null) {
            return;
        }

        ivy.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                   level.random.nextFloat() * 360F, 0F);
        ivy.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                         MobSpawnType.STRUCTURE, null, null);

        level.addFreshEntity(ivy);
    }

    private static BlockPos findSafeSpawnPos(ServerLevel level, BlockPos center) {
        // Try positions in a spiral around the village center
        for (int radius = 2; radius <= 16; radius += 2) {
            for (int dx = -radius; dx <= radius; dx += 2) {
                for (int dz = -radius; dz <= radius; dz += 2) {
                    // Only check points on the perimeter of the current radius
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    BlockPos checkPos = center.offset(dx, 0, dz);
                    BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, checkPos);

                    // Check if position is safe (solid ground, air above, not in water)
                    if (isSafeSpawnPosition(level, groundPos)) {
                        return groundPos;
                    }
                }
            }
        }
        return null; // No safe position found
    }

    private static boolean isSafeSpawnPosition(ServerLevel level, BlockPos pos) {
        // Check ground is solid
        if (!level.getBlockState(pos.below()).isSolid()) {
            return false;
        }

        // Check spawn position and above are air
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        // Check not in water
        if (level.getFluidState(pos).isSource()) {
            return false;
        }

        return true;
    }

    /**
     * Clear the village tracking when the world unloads.
     * Call this from your server stopping event.
     */
    public static void clearTracking() {
        villagesWithIvy.clear();
        tickCounter = 0;
    }
}
