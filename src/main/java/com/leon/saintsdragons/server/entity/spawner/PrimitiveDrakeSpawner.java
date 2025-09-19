package com.leon.saintsdragons.server.entity.spawner;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.RandomSource;

/**
 * Custom spawner for Primitive Drakes that bypasses biome modifier issues
 */
public class PrimitiveDrakeSpawner {
    
    private static final int SPAWN_CHUNK_RADIUS = 4; // Check 4 chunks around player (reduced)
    private static final int SPAWN_ATTEMPTS_PER_CHUNK = 1; // Try 1 spawn per chunk (reduced)
    private static final int MIN_SPAWN_DISTANCE = 24; // Minimum distance from player
    private static final int MAX_SPAWN_DISTANCE = 48; // Maximum distance from player (reduced)
    private static final int MAX_DRAKES_PER_PLAYER = 5; // Max drakes per player
    private static final int DESPAWN_DISTANCE = 128; // Despawn drakes beyond this distance
    private static final int DESPAWN_CHECK_INTERVAL = 600; // Check despawning every 30 seconds
    
    /**
     * Attempt to spawn Primitive Drakes around the player
     */
    public static void attemptSpawnAroundPlayer(ServerLevel level, BlockPos playerPos) {
        // Count existing drakes around player
        int existingDrakes = countDrakesAroundPlayer(level, playerPos);
        if (existingDrakes >= MAX_DRAKES_PER_PLAYER) {
            return; // Too many drakes already
        }
        if (level.getGameTime() % 200 != 0) return; // Only check every 10 seconds
        
        // Handle despawning of distant drakes
        handleDespawning(level, playerPos);
        
        RandomSource random = level.getRandom();
        
        // Check multiple chunks around the player
        for (int chunkX = -SPAWN_CHUNK_RADIUS; chunkX <= SPAWN_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = -SPAWN_CHUNK_RADIUS; chunkZ <= SPAWN_CHUNK_RADIUS; chunkZ++) {
                BlockPos chunkCenter = playerPos.offset(chunkX * 16, 0, chunkZ * 16);
                
                // Skip if too close or too far
                double distance = playerPos.distSqr(chunkCenter);
                if (distance < MIN_SPAWN_DISTANCE * MIN_SPAWN_DISTANCE || 
                    distance > MAX_SPAWN_DISTANCE * MAX_SPAWN_DISTANCE) {
                    continue;
                }
                
                // Attempt spawns in this chunk
                for (int attempt = 0; attempt < SPAWN_ATTEMPTS_PER_CHUNK; attempt++) {
                    if (random.nextFloat() < 0.1f) { // 10% chance per attempt
                        attemptSpawnInChunk(level, chunkCenter, random);
                    }
                }
            }
        }
    }
    
    /**
     * Attempt to spawn a Primitive Drake in a specific chunk
     */
    private static void attemptSpawnInChunk(ServerLevel level, BlockPos chunkCenter, RandomSource random) {
        // Random position within the chunk
        int x = chunkCenter.getX() + random.nextInt(16);
        int z = chunkCenter.getZ() + random.nextInt(16);
        
        // Get surface height and ensure proper positioning
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        
        // Double-check the ground level - find the actual solid ground
        while (y > 0 && !level.getBlockState(new BlockPos(x, y - 1, z)).canOcclude()) {
            y--;
        }
        
        BlockPos spawnPos = new BlockPos(x, y, z);
        
        // Ensure the drake has proper space - check for solid blocks above and below
        if (!isValidSpawnPosition(level, spawnPos)) {
            return; // Skip this spawn attempt
        }
        
        // Check if this is a suitable biome
        if (!isSuitableBiome(level, spawnPos)) {
            return;
        }
        
        // Check spawn conditions
        if (!canSpawnAt(level, spawnPos)) {
            return;
        }
        
        // Create and spawn the entity
        PrimitiveDrakeEntity drake = ModEntities.PRIMITIVE_DRAKE.get().create(level);
        if (drake != null) {
            // Ensure proper spawn height - spawn on top of the ground block
            double spawnY = spawnPos.getY() + 0.1; // Slightly above ground to prevent clipping
            drake.moveTo(spawnPos.getX() + 0.5, spawnY, spawnPos.getZ() + 0.5, 
                         random.nextFloat() * 360.0f, 0.0f);
            drake.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), 
                               MobSpawnType.NATURAL, null, null);
            
            if (level.addFreshEntity(drake)) {
                // Spawn successful!
                System.out.println("Spawned Primitive Drake at " + spawnPos);
                return; // Only spawn one drake per attempt
            }
        }
    }
    
    /**
     * Check if the biome is suitable for Primitive Drake spawning
     */
    private static boolean isSuitableBiome(ServerLevel level, BlockPos pos) {
        var biomeKey = level.getBiome(pos).unwrapKey().orElse(null);
        
        // Check for suitable biomes - only Plains, Badlands, Deserts, and Savannas
        return biomeKey == Biomes.PLAINS ||
               biomeKey == Biomes.SUNFLOWER_PLAINS ||
               biomeKey == Biomes.DESERT ||
               biomeKey == Biomes.BADLANDS ||
               biomeKey == Biomes.ERODED_BADLANDS ||
               biomeKey == Biomes.WOODED_BADLANDS ||
               biomeKey == Biomes.SAVANNA ||
               biomeKey == Biomes.SAVANNA_PLATEAU ||
               biomeKey == Biomes.WINDSWEPT_SAVANNA;
    }
    
    /**
     * Check if the position is suitable for spawning
     */
    private static boolean canSpawnAt(ServerLevel level, BlockPos pos) {
        // Check if there's enough space
        if (!level.getBlockState(pos.below()).canOcclude()) {
            return false;
        }
        
        // Check if the spawn area is clear
        for (int dy = 0; dy < 2; dy++) {
            if (!level.getBlockState(pos.above(dy)).isAir()) {
                return false;
            }
        }
        
        // Check if there's light (daytime spawning)
        if (level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) < 8) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Count existing Primitive Drakes around a player
     */
    private static int countDrakesAroundPlayer(ServerLevel level, BlockPos playerPos) {
        int count = 0;
        int radius = MAX_SPAWN_DISTANCE;
        
        for (var entity : level.getEntitiesOfClass(PrimitiveDrakeEntity.class, 
                new net.minecraft.world.phys.AABB(
                    playerPos.getX() - radius, playerPos.getY() - 10, playerPos.getZ() - radius,
                    playerPos.getX() + radius, playerPos.getY() + 10, playerPos.getZ() + radius))) {
            if (entity != null) count++;
        }
        
        return count;
    }
    
    
    /**
     * Check if a position is valid for spawning a Primitive Drake
     */
    private static boolean isValidSpawnPosition(ServerLevel level, BlockPos pos) {
        // Check if there's solid ground below
        if (!level.getBlockState(pos.below()).canOcclude()) {
            return false;
        }
        
        // Check if the spawn position itself is clear
        if (level.getBlockState(pos).canOcclude()) {
            return false;
        }
        
        // Check for water/lava at spawn position
        if (level.getBlockState(pos).getFluidState().isSource()) {
            return false;
        }
        
        // Check for WIDE open space around the spawn point (3x3x3 area)
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    
                    // Skip the ground check for the bottom layer
                    if (y == 0 && x == 0 && z == 0) continue;
                    
                    // Check if any position in the 3x3x3 area is blocked
                    if (level.getBlockState(checkPos).canOcclude()) {
                        return false; // Too cramped!
                    }
                }
            }
        }
        
        // Additional check: ensure no steep cliffs or walls nearby
        if (hasNearbyWalls(level, pos)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if there are walls or cliffs too close to the spawn position
     */
    private static boolean hasNearbyWalls(ServerLevel level, BlockPos pos) {
        // Check in a 5x5 area around the spawn point for walls
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x == 0 && z == 0) continue; // Skip center
                
                BlockPos checkPos = pos.offset(x, 0, z);
                
                // Check if there's a wall (solid block) at ground level
                if (level.getBlockState(checkPos).canOcclude()) {
                    // Check if there's also a wall above it (indicating a cliff/mountain)
                    if (level.getBlockState(checkPos.above()).canOcclude() && 
                        level.getBlockState(checkPos.above(2)).canOcclude()) {
                        return true; // Too close to a wall/cliff!
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Handle despawning of Primitive Drakes that are too far from players
     */
    private static void handleDespawning(ServerLevel level, BlockPos playerPos) {
        // Only check despawning every 30 seconds to avoid performance issues
        if (level.getGameTime() % DESPAWN_CHECK_INTERVAL != 0) return;
        
        // Find all Primitive Drakes in a large area around the player
        var drakes = level.getEntitiesOfClass(PrimitiveDrakeEntity.class, 
            new net.minecraft.world.phys.AABB(
                playerPos.getX() - DESPAWN_DISTANCE, playerPos.getY() - 20, playerPos.getZ() - DESPAWN_DISTANCE,
                playerPos.getX() + DESPAWN_DISTANCE, playerPos.getY() + 20, playerPos.getZ() + DESPAWN_DISTANCE));
        
        for (var drake : drakes) {
            if (drake == null || drake.isRemoved()) continue;
            
            // Check distance to nearest player
            double distanceToPlayer = playerPos.distSqr(drake.blockPosition());
            
            // Despawn if too far from any player
            if (distanceToPlayer > DESPAWN_DISTANCE * DESPAWN_DISTANCE) {
                // Check if there are any other players nearby
                boolean hasNearbyPlayer = false;
                for (var player : level.players()) {
                    if (player.isAlive() && !player.isSpectator()) {
                        double distanceToOtherPlayer = player.blockPosition().distSqr(drake.blockPosition());
                        if (distanceToOtherPlayer <= DESPAWN_DISTANCE * DESPAWN_DISTANCE) {
                            hasNearbyPlayer = true;
                            break;
                        }
                    }
                }
                
                // Only despawn if no players are nearby
                if (!hasNearbyPlayer) {
                    System.out.println("Despawning Primitive Drake at " + drake.blockPosition() + " - too far from players");
                    drake.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                }
            }
        }
    }
}
