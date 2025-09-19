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
    
    private static final int SPAWN_CHUNK_RADIUS = 2; // Check 2 chunks around player (further reduced)
    private static final int SPAWN_ATTEMPTS_PER_CHUNK = 1; // Try 1 spawn per chunk
    private static final int MIN_SPAWN_DISTANCE = 24; // Minimum distance from player
    private static final int MAX_SPAWN_DISTANCE = 32; // Maximum distance from player (further reduced)
    private static final int MAX_DRAKES_PER_PLAYER = 3; // Max drakes per player (reduced)
    private static final int DESPAWN_DISTANCE = 96; // Despawn drakes beyond this distance (reduced)
    private static final int DESPAWN_CHECK_INTERVAL = 1200; // Check despawning every 60 seconds (less frequent)
    private static final int SPAWN_CHECK_INTERVAL = 400; // Check spawning every 20 seconds (less frequent)
    private static final int WANDER_DESPAWN_TIME = 300; // Despawn drakes after 15 seconds of no player looking at them
    private static final int WANDER_CHECK_INTERVAL = 100; // Check wandering every 5 seconds
    
    // Simple biome cache to reduce expensive biome lookups
    private static final java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>, Boolean> BIOME_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Track when drakes were last seen by players
    private static final java.util.Map<Integer, Long> LAST_SEEN_TIMES = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * Attempt to spawn Primitive Drakes around the player
     */
    public static void attemptSpawnAroundPlayer(ServerLevel level, BlockPos playerPos) {
        // Count existing drakes around player
        int existingDrakes = countDrakesAroundPlayer(level, playerPos);
        if (existingDrakes >= MAX_DRAKES_PER_PLAYER) {
            return; // Too many drakes already
        }
        if (level.getGameTime() % SPAWN_CHECK_INTERVAL != 0) return; // Only check every 20 seconds
        
        // Handle despawning of distant drakes
        handleDespawning(level, playerPos);
        
        // Handle wandering despawning (drakes that players aren't looking at)
        handleWanderingDespawning(level, playerPos);
        
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
        
        // Double-check the ground level - find the actual solid ground (limited iterations)
        int maxGroundSearch = 5; // Limit ground search to prevent infinite loops
        while (y > 0 && maxGroundSearch > 0 && !level.getBlockState(new BlockPos(x, y - 1, z)).canOcclude()) {
            y--;
            maxGroundSearch--;
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
                return; // Only spawn one drake per attempt
            }
        }
    }
    
    /**
     * Check if the biome is suitable for Primitive Drake spawning (with caching)
     */
    private static boolean isSuitableBiome(ServerLevel level, BlockPos pos) {
        var biomeKey = level.getBiome(pos).unwrapKey().orElse(null);
        if (biomeKey == null) return false;
        
        // Check cache first
        Boolean cached = BIOME_CACHE.get(biomeKey);
        if (cached != null) {
            return cached;
        }
        
        // Check for suitable biomes - only Plains, Badlands, Deserts, and Savannas
        boolean suitable = biomeKey == Biomes.PLAINS ||
                          biomeKey == Biomes.SUNFLOWER_PLAINS ||
                          biomeKey == Biomes.DESERT ||
                          biomeKey == Biomes.BADLANDS ||
                          biomeKey == Biomes.ERODED_BADLANDS ||
                          biomeKey == Biomes.WOODED_BADLANDS ||
                          biomeKey == Biomes.SAVANNA ||
                          biomeKey == Biomes.SAVANNA_PLATEAU ||
                          biomeKey == Biomes.WINDSWEPT_SAVANNA;
        
        // Cache the result
        BIOME_CACHE.put(biomeKey, suitable);
        
        return suitable;
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
        
        // Check for open space around the spawn point (reduced to 2x2x2 area for performance)
        for (int x = -1; x <= 1; x += 2) { // Only check corners
            for (int y = 1; y <= 2; y++) {
                for (int z = -1; z <= 1; z += 2) { // Only check corners
                    BlockPos checkPos = pos.offset(x, y, z);
                    
                    // Check if any corner position is blocked
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
     * Check if there are walls or cliffs too close to the spawn position (optimized)
     */
    private static boolean hasNearbyWalls(ServerLevel level, BlockPos pos) {
        // Check in a 3x3 area around the spawn point for walls (reduced from 5x5)
        for (int x = -1; x <= 1; x += 2) { // Only check edges
            for (int z = -1; z <= 1; z += 2) { // Only check edges
                BlockPos checkPos = pos.offset(x, 0, z);
                
                // Check if there's a wall (solid block) at ground level
                if (level.getBlockState(checkPos).canOcclude()) {
                    // Check if there's also a wall above it (indicating a cliff/mountain)
                    if (level.getBlockState(checkPos.above()).canOcclude()) {
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
                    // Clean up tracking data before despawning
                    LAST_SEEN_TIMES.remove(drake.getId());
                    drake.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                }
            }
        }
    }
    
    /**
     * Handle despawning of Primitive Drakes that players aren't looking at (wandering off)
     */
    private static void handleWanderingDespawning(ServerLevel level, BlockPos playerPos) {
        // Only check wandering every 5 seconds to avoid performance issues
        if (level.getGameTime() % WANDER_CHECK_INTERVAL != 0) return;
        
        long currentTime = level.getGameTime();
        
        // Find all Primitive Drakes in a medium area around the player
        var drakes = level.getEntitiesOfClass(PrimitiveDrakeEntity.class, 
            new net.minecraft.world.phys.AABB(
                playerPos.getX() - MAX_SPAWN_DISTANCE, playerPos.getY() - 10, playerPos.getZ() - MAX_SPAWN_DISTANCE,
                playerPos.getX() + MAX_SPAWN_DISTANCE, playerPos.getY() + 10, playerPos.getZ() + MAX_SPAWN_DISTANCE));
        
        for (var drake : drakes) {
            if (drake == null || drake.isRemoved()) continue;
            
            int drakeId = drake.getId();
            boolean isBeingLookedAt = false;
            
            // Check if any player is looking at this drake
            for (var player : level.players()) {
                if (player.isAlive() && !player.isSpectator()) {
                    if (isPlayerLookingAtDrake(player, drake)) {
                        isBeingLookedAt = true;
                        // Update last seen time
                        LAST_SEEN_TIMES.put(drakeId, currentTime);
                        break;
                    }
                }
            }
            
            // If not being looked at, check if enough time has passed
            if (!isBeingLookedAt) {
                Long lastSeen = LAST_SEEN_TIMES.get(drakeId);
                if (lastSeen == null) {
                    // First time checking this drake, record current time
                    LAST_SEEN_TIMES.put(drakeId, currentTime);
                } else if (currentTime - lastSeen > WANDER_DESPAWN_TIME) {
                    // Drake hasn't been seen for too long, make it wander off
                    makeDrakeWanderOff(drake);
                    LAST_SEEN_TIMES.remove(drakeId);
                }
            }
        }
        
        // Clean up old entries for drakes that no longer exist
        LAST_SEEN_TIMES.entrySet().removeIf(entry -> {
            var drake = level.getEntity(entry.getKey());
            return drake == null || drake.isRemoved() || !(drake instanceof PrimitiveDrakeEntity);
        });
        
        // Additional safety: clean up entries older than 5 minutes to prevent memory leaks
        long fiveMinutesAgo = currentTime - (5 * 60 * 20); // 5 minutes in ticks
        LAST_SEEN_TIMES.entrySet().removeIf(entry -> entry.getValue() < fiveMinutesAgo);
    }
    
    /**
     * Check if a player is looking at a drake (within field of view)
     */
    private static boolean isPlayerLookingAtDrake(net.minecraft.world.entity.player.Player player, PrimitiveDrakeEntity drake) {
        // Calculate distance
        double distance = player.distanceTo(drake);
        if (distance > 32.0) return false; // Too far to see
        
        // Calculate angle between player's look direction and direction to drake
        net.minecraft.world.phys.Vec3 playerLook = player.getLookAngle();
        net.minecraft.world.phys.Vec3 toDrake = drake.position().subtract(player.position()).normalize();
        
        // Calculate dot product (cosine of angle)
        double dotProduct = playerLook.dot(toDrake);
        
        // Player's field of view is roughly 70 degrees (cos(35°) ≈ 0.82)
        return dotProduct > 0.82;
    }
    
    /**
     * Make a drake wander off (play animation and despawn)
     */
    private static void makeDrakeWanderOff(PrimitiveDrakeEntity drake) {
        if (drake == null || drake.isRemoved()) return;
        
        // Make the drake look around briefly before wandering off
        drake.getLookControl().setLookAt(
            drake.getX() + drake.getRandom().nextGaussian() * 2.0,
            drake.getY() + drake.getRandom().nextGaussian() * 1.0,
            drake.getZ() + drake.getRandom().nextGaussian() * 2.0,
            2.0f, 1.0f
        );
        
        // Remove from tracking immediately
        LAST_SEEN_TIMES.remove(drake.getId());
        
        // Despawn the drake (it has "wandered off")
        drake.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }
    
    /**
     * Periodic cleanup method to prevent memory leaks
     * Should be called occasionally to clean up stale tracking data
     */
    public static void cleanupTrackingData(ServerLevel level) {
        long currentTime = level.getGameTime();
        
        // Clean up entries for entities that no longer exist
        LAST_SEEN_TIMES.entrySet().removeIf(entry -> {
            var entity = level.getEntity(entry.getKey());
            return entity == null || entity.isRemoved() || !(entity instanceof PrimitiveDrakeEntity);
        });
        
        // Clean up entries older than 10 minutes (extra safety)
        long tenMinutesAgo = currentTime - (10 * 60 * 20); // 10 minutes in ticks
        LAST_SEEN_TIMES.entrySet().removeIf(entry -> entry.getValue() < tenMinutesAgo);
        
        // Clean up biome cache if it gets too large (prevent unbounded growth)
        if (BIOME_CACHE.size() > 100) {
            BIOME_CACHE.clear(); // Reset biome cache if it gets too large
        }
    }
}
