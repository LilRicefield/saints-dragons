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

public class DragonAllyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonAllyManager.class);
    private final DragonEntity dragon;
    private static final Map<String, UUID> USERNAME_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_ALLIES = 10;
    
    public DragonAllyManager(DragonEntity dragon) {
        this.dragon = dragon;
    }
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

        if (username.length() < 3 || username.length() > 16) {
            return AllyResult.INVALID_USERNAME;
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return AllyResult.INVALID_USERNAME;
        }
        String ownerName = owner.getName().getString();
        if (username.equalsIgnoreCase(ownerName)) {
                return AllyResult.IS_OWNER;
        }
        GlobalDragonAllySavedData data = GlobalDragonAllySavedData.get(owner.serverLevel());
        UUID cachedUuid = USERNAME_CACHE.get(username.toLowerCase());
        if (cachedUuid != null && data.isAlly(owner.getUUID(), cachedUuid)) {
                return AllyResult.ALREADY_ALLY;
        }
        if (data.getAllyCount(owner.getUUID()) >= MAX_ALLIES) {
            return AllyResult.ALLY_LIMIT_REACHED;
        }
        UUID playerUuid = resolveUsernameToUuid(owner, username);
        if (playerUuid == null) {
            return AllyResult.PLAYER_NOT_FOUND;
        }
        String resolvedUsername = resolveUuidToUsername(owner, playerUuid);
        if (resolvedUsername == null || !resolvedUsername.equalsIgnoreCase(username)) {
            return AllyResult.UUID_MISMATCH;
        }
        data.addAlly(owner.getUUID(), playerUuid, resolvedUsername);
        USERNAME_CACHE.put(resolvedUsername.toLowerCase(), playerUuid);

        LOGGER.info("Added ally '{}' ({}) for owner {}", resolvedUsername, playerUuid, owner.getGameProfile().getName());
        return AllyResult.SUCCESS;
    }

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


    public boolean isAlly(Player player) {
        if (player == null) return false;
        UUID ownerId = getOwnerId();
        net.minecraft.server.level.ServerLevel serverLevel = getServerLevel();
        if (ownerId == null || serverLevel == null) return false;
        GlobalDragonAllySavedData data = GlobalDragonAllySavedData.get(serverLevel);
        return data.isAlly(ownerId, player.getUUID());
    }

    public boolean isAlly(UUID uuid) {
        UUID ownerId = getOwnerId();
        net.minecraft.server.level.ServerLevel serverLevel = getServerLevel();
        if (ownerId == null || serverLevel == null) return false;
        GlobalDragonAllySavedData data = GlobalDragonAllySavedData.get(serverLevel);
        return data.isAlly(ownerId, uuid);
    }

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

    public int getMaxAllies() {
        return MAX_ALLIES;
    }

    public static int getMaxAlliesStatic() {
        return MAX_ALLIES;
    }

    public void clearAllies() {
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) {
            return;
        }
        GlobalDragonAllySavedData data = GlobalDragonAllySavedData.get(owner.serverLevel());
        data.clearAllies(owner.getUUID());
        LOGGER.info("Cleared all allies for owner {}", owner.getGameProfile().getName());
    }

    private static UUID resolveUsernameToUuid(ServerPlayer owner, String username) {
        MinecraftServer server = owner.server;
        if (server == null) return null;
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(username);
        if (onlinePlayer != null) {
            return onlinePlayer.getUUID();
        }

        return null;
    }

    private static String resolveUuidToUsername(ServerPlayer owner, UUID uuid) {
        MinecraftServer server = owner.server;
        if (server == null) return null;
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getGameProfile().getName();
        }
        try {
            return server.getProfileCache().get(uuid).map(profile -> profile.getName()).orElse(null);
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve UUID '{}' to username: {}", uuid, e.getMessage());
            return null;
        }
    }

    public void saveToNBT(CompoundTag tag) {
    }

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

    public enum AllyResult {
        SUCCESS("Successfully managed player ally"),
        INVALID_USERNAME("Invalid username provided"),
        PLAYER_NOT_FOUND("Player not found on server"),
        UUID_MISMATCH("Username-UUID validation failed"),
        ALREADY_ALLY("Player is already an ally"),
        NOT_ALLY("Player is not an ally"),
        ALLY_LIMIT_REACHED("Maximum player ally limit reached"),
        IS_OWNER("You are already the owner of this dragon!");
        
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