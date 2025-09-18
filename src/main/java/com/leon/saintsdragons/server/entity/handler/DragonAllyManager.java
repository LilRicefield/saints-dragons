package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ally relationships for dragons.
 * Handles UUID validation, username resolution, and persistence.
 * Thread-safe for server-side operations.
 */
public class DragonAllyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonAllyManager.class);
    private final DragonEntity dragon;
    
    // Map of ally UUIDs to their usernames (for display purposes)
    private final Map<UUID, String> allies = new ConcurrentHashMap<>();
    
    // Cache for username-to-UUID resolution to avoid repeated lookups
    private final Map<String, UUID> usernameCache = new ConcurrentHashMap<>();
    
    // Maximum number of allies per dragon
    private static final int MAX_ALLIES = 10;
    
    public DragonAllyManager(DragonEntity dragon) {
        this.dragon = dragon;
    }
    
    /**
     * Add an ally by username. Validates the username exists and matches UUID.
     */
    public AllyResult addAlly(String username) {
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
        
        // Check for inappropriate content
        if (containsInappropriateContent(username)) {
            return AllyResult.INAPPROPRIATE_CONTENT;
        }
        
        // Easter egg: Check for famous names first
        if (username.equalsIgnoreCase("Notch")) {
            return AllyResult.EASTER_EGG;
        }
        if (username.equalsIgnoreCase("jeb_")) {
            return AllyResult.EASTER_EGG;
        }
        if (username.equalsIgnoreCase("Dinnerbone")) {
            return AllyResult.EASTER_EGG;
        }
        if (username.equalsIgnoreCase("Grumm")) {
            return AllyResult.EASTER_EGG;
        }
        if (username.equalsIgnoreCase("Herobrine")) {
            return AllyResult.EASTER_EGG;
        }
        
        // Check if trying to add the dragon owner as an ally (they're already the owner!)
        if (dragon.getOwner() != null) {
            String ownerName = dragon.getOwner().getName().getString();
            if (username.equalsIgnoreCase(ownerName)) {
                return AllyResult.IS_OWNER;
            }
        }
        
        // Check if already an ally
        if (usernameCache.containsKey(username)) {
            UUID existingUuid = usernameCache.get(username);
            if (allies.containsKey(existingUuid)) {
                return AllyResult.ALREADY_ALLY;
            }
        }
        
        // Check ally limit
        if (allies.size() >= MAX_ALLIES) {
            return AllyResult.ALLY_LIMIT_REACHED;
        }
        
        // Resolve username to UUID
        UUID playerUuid = resolveUsernameToUuid(username);
        if (playerUuid == null) {
            return AllyResult.PLAYER_NOT_FOUND;
        }
        
        // Validate UUID matches username (extra security)
        String resolvedUsername = resolveUuidToUsername(playerUuid);
        if (resolvedUsername == null || !resolvedUsername.equalsIgnoreCase(username)) {
            return AllyResult.UUID_MISMATCH;
        }
        
        // Add to allies
        allies.put(playerUuid, resolvedUsername);
        usernameCache.put(resolvedUsername.toLowerCase(), playerUuid);
        
        LOGGER.info("Added ally '{}' ({}) to dragon {}", resolvedUsername, playerUuid, dragon);
        return AllyResult.SUCCESS;
    }
    
    /**
     * Remove an ally by username
     */
    public AllyResult removeAlly(String username) {
        if (username == null || username.trim().isEmpty()) {
            return AllyResult.INVALID_USERNAME;
        }
        
        username = username.trim().toLowerCase();
        
        UUID uuid = usernameCache.get(username);
        if (uuid == null) {
            return AllyResult.NOT_ALLY;
        }
        
        allies.remove(uuid);
        usernameCache.remove(username);
        
        LOGGER.info("Removed ally '{}' ({}) from dragon {}", username, uuid, dragon);
        return AllyResult.SUCCESS;
    }
    
    /**
     * Remove an ally by UUID
     */
    public boolean removeAlly(UUID uuid) {
        String username = allies.remove(uuid);
        if (username != null) {
            usernameCache.remove(username.toLowerCase());
            LOGGER.info("Removed ally '{}' ({}) from dragon {}", username, uuid, dragon);
            return true;
        }
        return false;
    }
    
    /**
     * Check if a player is an ally
     */
    public boolean isAlly(Player player) {
        if (player == null) return false;
        return allies.containsKey(player.getUUID());
    }
    
    /**
     * Check if a UUID is an ally
     */
    public boolean isAlly(UUID uuid) {
        return allies.containsKey(uuid);
    }
    
    /**
     * Get all ally usernames
     */
    public List<String> getAllyUsernames() {
        return new ArrayList<>(allies.values());
    }
    
    /**
     * Get all ally UUIDs
     */
    public Set<UUID> getAllyUuids() {
        return new HashSet<>(allies.keySet());
    }
    
    /**
     * Get current ally count
     */
    public int getAllyCount() {
        return allies.size();
    }
    
    /**
     * Get maximum ally limit
     */
    public int getMaxAllies() {
        return MAX_ALLIES;
    }
    
    /**
     * Clear all allies
     */
    public void clearAllies() {
        allies.clear();
        usernameCache.clear();
        LOGGER.info("Cleared all allies from dragon {}", dragon);
    }
    
    /**
     * Resolve username to UUID using server player list
     */
    private UUID resolveUsernameToUuid(String username) {
        Level level = dragon.level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }
        
        MinecraftServer server = serverLevel.getServer();
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
    private String resolveUuidToUsername(UUID uuid) {
        Level level = dragon.level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }
        
        MinecraftServer server = serverLevel.getServer();
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
        ListTag allyList = new ListTag();
        for (Map.Entry<UUID, String> entry : allies.entrySet()) {
            CompoundTag allyTag = new CompoundTag();
            allyTag.putUUID("UUID", entry.getKey());
            allyTag.putString("Username", entry.getValue());
            allyList.add(allyTag);
        }
        tag.put("Allies", allyList);
    }
    
    /**
     * Load ally data from NBT
     */
    public void loadFromNBT(CompoundTag tag) {
        allies.clear();
        usernameCache.clear();
        
        if (tag.contains("Allies", Tag.TAG_LIST)) {
            ListTag allyList = tag.getList("Allies", Tag.TAG_COMPOUND);
            for (int i = 0; i < allyList.size(); i++) {
                CompoundTag allyTag = allyList.getCompound(i);
                if (allyTag.hasUUID("UUID") && allyTag.contains("Username", Tag.TAG_STRING)) {
                    UUID uuid = allyTag.getUUID("UUID");
                    String username = allyTag.getString("Username");
                    allies.put(uuid, username);
                    usernameCache.put(username.toLowerCase(), uuid);
                }
            }
        }
    }
    
    /**
     * Check if username contains inappropriate content
     * Uses pattern matching to catch common profanity and variations
     */
    private boolean containsInappropriateContent(String username) {
        String lowerUsername = username.toLowerCase();
        
        // Common profanity patterns (using regex to catch variations)
        String[] inappropriatePatterns = {
            // Common profanity with character substitutions
            "f[uv]ck", "sh[i1]t", "b[i1]tch", "d[i1]ck", "p[i1]ss", "c[o0]ck",
            "a[s$][s$]", "f[a@]g", "n[i1]gg[a@]", "r[e3]t[a@]rd", "wh[o0]r[e3]",
            // Common variations
            "f[u]ck", "sh[i]t", "b[i]tch", "d[i]ck", "p[i]ss", "c[o]ck",
            "a[s]s", "f[a]g", "n[i]gg[a]", "r[e]t[a]rd", "wh[o]r[e]",
            // Leetspeak variations
            "f[u]ck", "sh[i]t", "b[i]tch", "d[i]ck", "p[i]ss", "c[o]ck",
            // Common misspellings
            "fuk", "shyt", "bich", "dik", "pis", "cok",
            // Other inappropriate terms
            "h[e3]ll", "d[a@]mn", "cr[a@]p", "p[o0]rn", "s[e3]x"
        };
        
        for (String pattern : inappropriatePatterns) {
            if (lowerUsername.matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        
        // Check for repeated characters (common way to bypass filters)
        if (lowerUsername.matches(".*(.)\\1{2,}.*")) {
            return true;
        }
        
        // Check for excessive numbers (another bypass method)
        long numberCount = lowerUsername.chars().filter(Character::isDigit).count();
        if (numberCount > username.length() / 2) {
            return true;
        }
        
        return false;
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
        EASTER_EGG("Easter egg message"),
        IS_OWNER("You are already the owner of this dragon!"),
        INAPPROPRIATE_CONTENT("Inappropriate content detected");
        
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
