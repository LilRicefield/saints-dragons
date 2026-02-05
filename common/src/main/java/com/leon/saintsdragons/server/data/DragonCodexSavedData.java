package com.leon.saintsdragons.server.data;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistent per-player registry for tamed dragons shown in the Draconic Codex.
 */
public class DragonCodexSavedData extends SavedData {
    private static final String DATA_NAME = "saintsdragons_draconic_codex";

    private final Map<UUID, List<DragonCodexEntry>> entriesByOwner = new HashMap<>();

    public static DragonCodexSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                DragonCodexSavedData::load,
                DragonCodexSavedData::new,
                DATA_NAME
        );
    }

    public void addDragon(ServerPlayer owner, DragonEntity dragon) {
        if (owner == null || dragon == null) {
            return;
        }
        UUID ownerId = owner.getUUID();
        UUID dragonId = dragon.getUUID();
        List<DragonCodexEntry> entries = entriesByOwner.computeIfAbsent(ownerId, id -> new ArrayList<>());
        for (DragonCodexEntry entry : entries) {
            if (entry.dragonId().equals(dragonId)) {
                entry.setDisplayName(dragon.getName().getString());
                entry.setMaxHealth(dragon.getMaxHealth());
                entry.setCurrentHealth(dragon.getHealth());
                entry.setArmor(dragon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR));
                entry.setHunger(dragon.getHunger());
                entry.setHappiness(dragon.getHappiness());
                entry.setVariantId(resolveVariantId(dragon));
                entry.setGenderId(dragon.getGender().getId());
                entry.setGenderKnown(dragon.hasGender());
                entry.setDragonType(resolveDragonType(dragon));
                entry.setIsBaby(dragon.isBaby());
                entry.setPosition(dragon.getX(), dragon.getY(), dragon.getZ());
                entry.setBiomeId(resolveBiomeId(dragon));
                setDirty();
                return;
            }
        }
        entries.add(new DragonCodexEntry(
                dragonId,
                dragon.getName().getString(),
                dragon.getMaxHealth(),
                dragon.getHealth(),
                dragon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR),
                dragon.getHunger(),
                dragon.getHappiness(),
                resolveVariantId(dragon),
                dragon.getGender().getId(),
                dragon.hasGender(),
                resolveDragonType(dragon),
                dragon.isBaby(),
                dragon.getX(),
                dragon.getY(),
                dragon.getZ(),
                resolveBiomeId(dragon)
        ));
        setDirty();
    }

    public void removeDragon(UUID ownerId, UUID dragonId) {
        if (ownerId == null || dragonId == null) {
            return;
        }
        List<DragonCodexEntry> entries = entriesByOwner.get(ownerId);
        if (entries == null || entries.isEmpty()) {
            return;
        }
        boolean removed = entries.removeIf(entry -> entry.dragonId().equals(dragonId));
        if (removed) {
            setDirty();
        }
    }

    public void updateDragonName(UUID ownerId, UUID dragonId, String displayName) {
        if (ownerId == null || dragonId == null || displayName == null) {
            return;
        }
        List<DragonCodexEntry> entries = entriesByOwner.get(ownerId);
        if (entries == null) {
            return;
        }
        for (DragonCodexEntry entry : entries) {
            if (entry.dragonId().equals(dragonId)) {
                if (!Objects.equals(entry.displayName(), displayName)) {
                    entry.setDisplayName(displayName);
                    setDirty();
                }
                return;
            }
        }
    }

    public void updateDragonStats(UUID ownerId, DragonEntity dragon) {
        if (ownerId == null || dragon == null) {
            return;
        }
        List<DragonCodexEntry> entries = entriesByOwner.get(ownerId);
        if (entries == null) {
            return;
        }
        for (DragonCodexEntry entry : entries) {
            if (entry.dragonId().equals(dragon.getUUID())) {
                entry.setMaxHealth(dragon.getMaxHealth());
                entry.setCurrentHealth(dragon.getHealth());
                entry.setArmor(dragon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR));
                entry.setHunger(dragon.getHunger());
                entry.setHappiness(dragon.getHappiness());
                entry.setVariantId(resolveVariantId(dragon));
                entry.setGenderId(dragon.getGender().getId());
                entry.setGenderKnown(dragon.hasGender());
                entry.setDragonType(resolveDragonType(dragon));
                entry.setIsBaby(dragon.isBaby());
                entry.setPosition(dragon.getX(), dragon.getY(), dragon.getZ());
                entry.setBiomeId(resolveBiomeId(dragon));
                setDirty();
                return;
            }
        }
    }

    public List<DragonCodexEntry> getEntriesFor(ServerPlayer owner) {
        if (owner == null) {
            return List.of();
        }
        List<DragonCodexEntry> entries = entriesByOwner.get(owner.getUUID());
        if (entries == null) {
            return List.of();
        }
        return new ArrayList<>(entries);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag playerList = new ListTag();
        for (Map.Entry<UUID, List<DragonCodexEntry>> entry : entriesByOwner.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("Owner", entry.getKey());
            ListTag dragonList = new ListTag();
            for (DragonCodexEntry dragonEntry : entry.getValue()) {
                CompoundTag dragonTag = new CompoundTag();
                dragonTag.putUUID("DragonId", dragonEntry.dragonId());
                dragonTag.putString("Name", dragonEntry.displayName());
                dragonTag.putDouble("MaxHealth", dragonEntry.maxHealth());
                dragonTag.putDouble("CurrentHealth", dragonEntry.currentHealth());
                dragonTag.putDouble("Armor", dragonEntry.armor());
                dragonTag.putDouble("Hunger", dragonEntry.hunger());
                dragonTag.putDouble("Happiness", dragonEntry.happiness());
                dragonTag.putInt("VariantId", dragonEntry.variantId());
                dragonTag.putByte("GenderId", dragonEntry.genderId());
                dragonTag.putBoolean("GenderKnown", dragonEntry.genderKnown());
                dragonTag.putString("DragonType", dragonEntry.dragonType());
                dragonTag.putBoolean("IsBaby", dragonEntry.isBaby());
                dragonTag.putDouble("PosX", dragonEntry.posX());
                dragonTag.putDouble("PosY", dragonEntry.posY());
                dragonTag.putDouble("PosZ", dragonEntry.posZ());
                dragonTag.putString("BiomeId", dragonEntry.biomeId());
                dragonList.add(dragonTag);
            }
            playerTag.put("Dragons", dragonList);
            playerList.add(playerTag);
        }
        tag.put("Players", playerList);
        return tag;
    }

    public static DragonCodexSavedData load(CompoundTag tag) {
        DragonCodexSavedData data = new DragonCodexSavedData();
        if (tag.contains("Players", Tag.TAG_LIST)) {
            ListTag playerList = tag.getList("Players", Tag.TAG_COMPOUND);
            for (int i = 0; i < playerList.size(); i++) {
                CompoundTag playerTag = playerList.getCompound(i);
                if (!playerTag.hasUUID("Owner")) {
                    continue;
                }
                UUID ownerId = playerTag.getUUID("Owner");
                List<DragonCodexEntry> entries = new ArrayList<>();
                if (playerTag.contains("Dragons", Tag.TAG_LIST)) {
                    ListTag dragonList = playerTag.getList("Dragons", Tag.TAG_COMPOUND);
                    for (int j = 0; j < dragonList.size(); j++) {
                        CompoundTag dragonTag = dragonList.getCompound(j);
                        if (!dragonTag.hasUUID("DragonId")) {
                            continue;
                        }
                        UUID dragonId = dragonTag.getUUID("DragonId");
                        String name = dragonTag.getString("Name");
                        double maxHealth = dragonTag.contains("MaxHealth") ? dragonTag.getDouble("MaxHealth") : 0.0;
                        double currentHealth = dragonTag.contains("CurrentHealth") ? dragonTag.getDouble("CurrentHealth") : maxHealth;
                        double armor = dragonTag.contains("Armor") ? dragonTag.getDouble("Armor") : 0.0;
                        double hunger = dragonTag.contains("Hunger") ? dragonTag.getDouble("Hunger") : DragonEntity.HUNGER_MAX;
                        double happiness = dragonTag.contains("Happiness") ? dragonTag.getDouble("Happiness") : DragonEntity.HAPPINESS_MAX;
                        int variantId = dragonTag.contains("VariantId") ? dragonTag.getInt("VariantId") : 0;
                        byte genderId = dragonTag.contains("GenderId") ? dragonTag.getByte("GenderId") : 0;
                        boolean genderKnown = dragonTag.contains("GenderKnown") && dragonTag.getBoolean("GenderKnown");
                        String dragonType = dragonTag.contains("DragonType") ? dragonTag.getString("DragonType") : "ignivorus";
                        boolean isBaby = dragonTag.contains("IsBaby") && dragonTag.getBoolean("IsBaby");
                        double posX = dragonTag.contains("PosX") ? dragonTag.getDouble("PosX") : 0.0D;
                        double posY = dragonTag.contains("PosY") ? dragonTag.getDouble("PosY") : 0.0D;
                        double posZ = dragonTag.contains("PosZ") ? dragonTag.getDouble("PosZ") : 0.0D;
                        String biomeId = dragonTag.contains("BiomeId") ? dragonTag.getString("BiomeId") : "minecraft:unknown";
                        entries.add(new DragonCodexEntry(dragonId, name, maxHealth, currentHealth, armor, hunger, happiness,
                                variantId, genderId, genderKnown, dragonType, isBaby, posX, posY, posZ, biomeId));
                    }
                }
                data.entriesByOwner.put(ownerId, entries);
            }
        }
        return data;
    }

    public static class DragonCodexEntry {
        private final UUID dragonId;
        private String displayName;
        private double maxHealth;
        private double currentHealth;
        private double armor;
        private double hunger;
        private double happiness;
        private int variantId;
        private byte genderId;
        private boolean genderKnown;
        private String dragonType;
        private boolean isBaby;
        private double posX;
        private double posY;
        private double posZ;
        private String biomeId;

        public DragonCodexEntry(UUID dragonId, String displayName, double maxHealth, double currentHealth, double armor,
                                double hunger, double happiness, int variantId, byte genderId, boolean genderKnown,
                                String dragonType, boolean isBaby, double posX, double posY, double posZ, String biomeId) {
            this.dragonId = dragonId;
            this.displayName = displayName;
            this.maxHealth = maxHealth;
            this.currentHealth = currentHealth;
            this.armor = armor;
            this.hunger = hunger;
            this.happiness = happiness;
            this.variantId = variantId;
            this.genderId = genderId;
            this.genderKnown = genderKnown;
            this.dragonType = dragonType;
            this.isBaby = isBaby;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.biomeId = biomeId;
        }

        public UUID dragonId() {
            return dragonId;
        }

        public String displayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public double maxHealth() {
            return maxHealth;
        }

        public void setMaxHealth(double maxHealth) {
            this.maxHealth = maxHealth;
        }

        public double currentHealth() {
            return currentHealth;
        }

        public void setCurrentHealth(double currentHealth) {
            this.currentHealth = currentHealth;
        }

        public double armor() {
            return armor;
        }

        public void setArmor(double armor) {
            this.armor = armor;
        }

        public double hunger() {
            return hunger;
        }

        public void setHunger(double hunger) {
            this.hunger = hunger;
        }

        public double happiness() {
            return happiness;
        }

        public void setHappiness(double happiness) {
            this.happiness = happiness;
        }

        public int variantId() {
            return variantId;
        }

        public void setVariantId(int variantId) {
            this.variantId = variantId;
        }

        public byte genderId() {
            return genderId;
        }

        public void setGenderId(byte genderId) {
            this.genderId = genderId;
        }

        public boolean genderKnown() {
            return genderKnown;
        }

        public void setGenderKnown(boolean genderKnown) {
            this.genderKnown = genderKnown;
        }

        public String dragonType() {
            return dragonType;
        }

        public void setDragonType(String dragonType) {
            this.dragonType = dragonType;
        }

        public boolean isBaby() {
            return isBaby;
        }

        public void setIsBaby(boolean isBaby) {
            this.isBaby = isBaby;
        }

        public double posX() {
            return posX;
        }

        public double posY() {
            return posY;
        }

        public double posZ() {
            return posZ;
        }

        public void setPosition(double posX, double posY, double posZ) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
        }

        public String biomeId() {
            return biomeId;
        }

        public void setBiomeId(String biomeId) {
            this.biomeId = biomeId;
        }
    }

    private static int resolveVariantId(DragonEntity dragon) {
        if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus ignivorus) {
            return ignivorus.getTextureVariant();
        }
        return 0;
    }

    private static String resolveDragonType(DragonEntity dragon) {
        if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus) {
            return "ignivorus";
        } else if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx) {
            return "raevyx";
        } else if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw) {
            return "nulljaw";
        } else if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane) {
            return "cindervane";
        } else if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut) {
            return "stegonaut";
        }
        return "ignivorus"; // Default fallback
    }

    private static String resolveBiomeId(DragonEntity dragon) {
        return dragon.level().getBiome(dragon.blockPosition())
                .unwrapKey()
                .map(key -> key.location().toString())
                .orElse("minecraft:unknown");
    }
}
