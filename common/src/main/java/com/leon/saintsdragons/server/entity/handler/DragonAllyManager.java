package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.server.data.GlobalDragonAllySavedData;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ally relationships for dragons.
 * Handles UUID validation, username resolution, and persistence.
 * Thread-safe for server-side operations.
 */
public class DragonAllyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonAllyManager.class);
    private final DragonEntity dragon;
    
    // Cache for username-to-UUID resolution to avoid repeated lookups
    private static final Map<String, UUID> USERNAME_CACHE = new ConcurrentHashMap<>();
    
    // Maximum number of allies per wyvern
    private static final int MAX_ALLIES = 10;
    
    public DragonAllyManager(DragonEntity dragon) {
        this.dragon = dragon;
    }
    
    /**
     * Add an ally by username. Validates the username exists and matches UUID.
     */
    public AllyResult addAlly(String username) {
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) {
            return AllyResult.INVALID_USERNAME;
        }
        return addAllyForOwner(owner, username);
    }

    public static AllyResult addAllyForOwner(ServerPlayer owner, String username) {
        if (username == null || username.trim().isEmpty()) {
            return AllyResult.INVALID_USERNAME;
        }
        
        username = username.trim();
        
        // Validate username length and format
        if (username.length() < 3 || username.length() > 16) {
            return AllyResult.INVALID_USERNAME;
        }
        
        // Check for valid Minecraft username characters (alphanumeric and underscore only)
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return AllyResult.INVALID_USERNAME;
        }
        
        // Check if trying to add the wyvern owner as an ally (they're already the owner!)
        String ownerName = owner.getName().getString();
        if (username.equalsIgnoreCase(ownerName)) {
                return AllyResult.IS_OWNER;
        }
        
        // Check if already an ally
        GlobalDragonAllySavedData data = GlobalDragonAllySavedData.get(owner.serverLevel());
        UUID cachedUuid = USERNAME_CACHE.get(username.toLowerCase());
        if (cachedUuid != null && data.isAlly(owner.getUUID(), cachedUuid)) {
                return AllyResult.ALREADY_ALLY;
        }
        
        // Check ally limit
        if (data.getAllyCount(owner.getUUID()) >= MAX_ALLIES) {
            return AllyResult.ALLY_LIMIT_REACHED;
        }
        
        // Resolve username to UUID
        UUID playerUuid = resolveUsernameToUuid(owner, username);
        if (playerUuid == null) {
            return AllyResult.PLAYER_NOT_FOUND;
        }
        
        // Validate UUID matches username (extra security)
        String resolvedUsername = resolveUuidToUsername(owner, playerUuid);
        if (resolvedUsername == null || !resolvedUsername.equalsIgnoreCase(username)) {
            return AllyResult.UUID_MISMATCH;
        }
        
        // Add to allies
        data.addAlly(owner.getUUID(), playerUuid, resolvedUsername);
        USERNAME_CACHE.put(resolvedUsername.toLowerCase(), playerUuid);

        LOGGER.info("Added ally '{}' ({}) for owner {}", resolvedUsername, playerUuid, owner.getGameProfile().getName());
        return AllyResult.SUCCESS;
    }
    
    /**
     * Remove an ally by username
     */
    public AllyResult removeAlly(String username) {
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) {
            return AllyResult.INVALID_USERNAME;
        }
        return removeAllyForOwner(owner, username);
    }

    public static AllyResult removeAllyForOwner(ServerPlayer owner, String username) {
        if (username == null || username.trim().isEmpty()) {
            return AllyResult.INVALID_USERNAME;
        }
        
        username = username.trim().toLowerCase();
        
        GlobalDragonAllySavedData data = GlobalDragonAllySavedData.get(owner.serverLevel());
        UUID uuid = USERNAME_CACHE.get(username);
        if (uuid == null) {
            uuid = resolveAllyUuidByName(data, owner.getUUID(), username);
        }
        if (uuid == null) {
            return AllyResult.NOT_ALLY;
        }

        if (!data.removeAlly(owner.getUUID(), uuid)) {
            return AllyResult.NOT_ALLY;
        }
        USERNAME_CACHE.remove(username);

        LOGGER.info("Removed ally '{}' ({}) for owner {}", username, uuid, owner.getGameProfile().getName());
        return AllyResult.SUCCESS;
    }
    
    /**
     * Remove an ally by UUID
     */
    public boolean removeAlly(UUID uuid) {
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null || uuid == null) {
            return false;
        }
        GlobalDragonAllySavedData data = GlobalDragonAllySavedData.get(owner.serverLevel());
        boolean removed = data.removeAlly(owner.getUUID(), uuid);
        if (removed) {
            String username = resolveUuidToUsername(owner, uuid);
            if (username != null) {
                USERNAME_CACHE.remove(username.toLowerCase());
            }
            LOGGER.info("Removed ally '{}' ({}) for owner {}", username, uuid, owner.getGameProfile().getName());
        }
        return removed;
    }
    
    /**
     * Check if a player is an ally
     */
    public boolean isAlly(Player player) {
        if (player == null) return false;
        UUID ownerId = getOwnerId();
        net.minecraft.server.level.ServerLevel serverLevel = getServerLevel();
        if (ownerId == null || serverLevel == null) return false;
        GlobalDragonAllySavedData data = GlobalDragonAllySavedData.get(serverLevel);
        return data.isAlly(ownerId, player.getUUID());
    }
    
    /**
     * Check if a UUID is an ally
     */
    public boolean isAlly(UUID uuid) {
        UUID ownerId = getOwnerId();
        net.minecraft.server.level.ServerLevel serverLevel = getServerLevel();
        if (ownerId == null || serverLevel == null) return false;
        GlobalDragonAllySavedData data = GlobalDragonAllySavedData.get(serverLevel);
        return data.isAlly(ownerId, uuid);
    }
    
    /**
     * Get all ally usernames
     */
    public List<String> getAllyUsernames() {
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) {
            return new ArrayList<>();
        }
        return getAllyUsernamesForOwner(owner);
    }

    public static List<String> getAllyUsernamesForOwner(ServerPlayer owner) {
        GlobalDragonAllySavedData data = GlobalDragonAllySavedData.get(owner.serverLevel());
        return new ArrayList<>(data.getAllies(owner.getUUID()).values());
    }
    
    /**
     * Get all ally UUIDs
     */
    public java.util.Set<UUID> getAllyUuids() {
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) {
            return java.util.Set.of();
        }
        GlobalDragonAllySavedData data = GlobalDragonAllySavedData.get(owner.serverLevel());
        return data.getAllies(owner.getUUID()).keySet();
    }
    
    /**
     * Get current ally count
     */
    public int getAllyCount() {
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) {
            return 0;
        }
        return getAllyCountForOwner(owner);
    }

    public static int getAllyCountForOwner(ServerPlayer owner) {
        GlobalDragonAllySavedData data = GlobalDragonAllySavedData.get(owner.serverLevel());
        return data.getAllyCount(owner.getUUID());
    }
    
    /**
     * Get maximum ally limit
     */
    public int getMaxAllies() {
        return MAX_ALLIES;
    }

    public static int getMaxAlliesStatic() {
        return MAX_ALLIES;
    }
    
    /**
     * Clear all allies
     */
    public void clearAllies() {
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) {
            return;
        }
        GlobalDragonAllySavedData data = GlobalDragonAllySavedData.get(owner.serverLevel());
        data.clearAllies(owner.getUUID());
        LOGGER.info("Cleared all allies for owner {}", owner.getGameProfile().getName());
    }
    
    /**
     * Resolve username to UUID using server player list
     */
    private static UUID resolveUsernameToUuid(ServerPlayer owner, String username) {
        MinecraftServer server = owner.server;
        if (server == null) return null;
        
        // ONLY allow currently online players - no profile cache lookup!
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(username);
        if (onlinePlayer != null) {
            return onlinePlayer.getUUID();
        }
        
        // Player is not online - return null
        return null;
    }
    
    /**
     * Resolve UUID to username using server player list
     */
    private static String resolveUuidToUsername(ServerPlayer owner, UUID uuid) {
        MinecraftServer server = owner.server;
        if (server == null) return null;
        
        // Try to find online player first
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getGameProfile().getName();
        }
        
        // Try to resolve from server's player data
        try {
            return server.getProfileCache().get(uuid).map(profile -> profile.getName()).orElse(null);
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve UUID '{}' to username: {}", uuid, e.getMessage());
            return null;
        }
    }
    
    /**
     * Save ally data to NBT
     */
    public void saveToNBT(CompoundTag tag) {
    }
    
    /**
     * Load ally data from NBT
     */
    public void loadFromNBT(CompoundTag tag) {
    }
    
    private ServerPlayer getOwnerPlayer() {
        if (!dragon.isTame()) {
            return null;
        }
        if (dragon.getOwner() instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        return null;
    }

    private UUID getOwnerId() {
        if (!dragon.isTame()) {
            return null;
        }
        return dragon.getOwnerUUID();
    }

    private net.minecraft.server.level.ServerLevel getServerLevel() {
        if (dragon.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return serverLevel;
        }
        return null;
    }

    private static UUID resolveAllyUuidByName(GlobalDragonAllySavedData data, UUID ownerId, String usernameLower) {
        Map<UUID, String> allies = data.getAllies(ownerId);
        for (Map.Entry<UUID, String> entry : allies.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(usernameLower)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Result enum for ally operations
     */
    public enum AllyResult {
        SUCCESS("Successfully managed ally"),
        INVALID_USERNAME("Invalid username provided"),
        PLAYER_NOT_FOUND("Player not found on server"),
        UUID_MISMATCH("Username-UUID validation failed"),
        ALREADY_ALLY("Player is already an ally"),
        NOT_ALLY("Player is not an ally"),
        ALLY_LIMIT_REACHED("Maximum ally limit reached"),
        IS_OWNER("You are already the owner of this wyvern!");
        
        private final String message;
        
        AllyResult(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }
}
