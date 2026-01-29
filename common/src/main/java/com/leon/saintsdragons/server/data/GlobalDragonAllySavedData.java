package com.leon.saintsdragons.server.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent per-player ally registry shared across all dragons.
 */
public class GlobalDragonAllySavedData extends SavedData {
    private static final String DATA_NAME = "saintsdragons_global_allies";

    private final Map<UUID, Map<UUID, String>> alliesByOwner = new HashMap<>();

    public static GlobalDragonAllySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                GlobalDragonAllySavedData::load,
                GlobalDragonAllySavedData::new,
                DATA_NAME
        );
    }

    public Map<UUID, String> getAllies(UUID ownerId) {
        Map<UUID, String> allies = alliesByOwner.get(ownerId);
        if (allies == null) {
            return Map.of();
        }
        return new HashMap<>(allies);
    }

    public boolean isAlly(UUID ownerId, UUID allyId) {
        Map<UUID, String> allies = alliesByOwner.get(ownerId);
        if (allies == null) {
            return false;
        }
        return allies.containsKey(allyId);
    }

    public int getAllyCount(UUID ownerId) {
        Map<UUID, String> allies = alliesByOwner.get(ownerId);
        return allies == null ? 0 : allies.size();
    }

    public void addAlly(UUID ownerId, UUID allyId, String username) {
        alliesByOwner.computeIfAbsent(ownerId, id -> new HashMap<>()).put(allyId, username);
        setDirty();
    }

    public boolean removeAlly(UUID ownerId, UUID allyId) {
        Map<UUID, String> allies = alliesByOwner.get(ownerId);
        if (allies == null) {
            return false;
        }
        boolean removed = allies.remove(allyId) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public void clearAllies(UUID ownerId) {
        Map<UUID, String> allies = alliesByOwner.get(ownerId);
        if (allies != null && !allies.isEmpty()) {
            allies.clear();
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag playerList = new ListTag();
        for (Map.Entry<UUID, Map<UUID, String>> entry : alliesByOwner.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("Owner", entry.getKey());
            ListTag allyList = new ListTag();
            for (Map.Entry<UUID, String> allyEntry : entry.getValue().entrySet()) {
                CompoundTag allyTag = new CompoundTag();
                allyTag.putUUID("UUID", allyEntry.getKey());
                allyTag.putString("Username", allyEntry.getValue());
                allyList.add(allyTag);
            }
            playerTag.put("Allies", allyList);
            playerList.add(playerTag);
        }
        tag.put("Players", playerList);
        return tag;
    }

    public static GlobalDragonAllySavedData load(CompoundTag tag) {
        GlobalDragonAllySavedData data = new GlobalDragonAllySavedData();
        if (tag.contains("Players", Tag.TAG_LIST)) {
            ListTag playerList = tag.getList("Players", Tag.TAG_COMPOUND);
            for (int i = 0; i < playerList.size(); i++) {
                CompoundTag playerTag = playerList.getCompound(i);
                if (!playerTag.hasUUID("Owner")) {
                    continue;
                }
                UUID ownerId = playerTag.getUUID("Owner");
                Map<UUID, String> allies = new HashMap<>();
                if (playerTag.contains("Allies", Tag.TAG_LIST)) {
                    ListTag allyList = playerTag.getList("Allies", Tag.TAG_COMPOUND);
                    for (int j = 0; j < allyList.size(); j++) {
                        CompoundTag allyTag = allyList.getCompound(j);
                        if (allyTag.hasUUID("UUID") && allyTag.contains("Username", Tag.TAG_STRING)) {
                            allies.put(allyTag.getUUID("UUID"), allyTag.getString("Username"));
                        }
                    }
                }
                data.alliesByOwner.put(ownerId, allies);
            }
        }
        return data;
    }
}
