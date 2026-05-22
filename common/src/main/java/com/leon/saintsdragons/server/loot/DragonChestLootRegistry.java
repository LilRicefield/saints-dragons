package com.leon.saintsdragons.server.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DragonChestLootRegistry {
    public static final ResourceLocation DEFAULTS_FILE = new ResourceLocation("saintsdragons", "defaults");

    private static volatile List<Entry> entries = List.of();

    private DragonChestLootRegistry() {
    }

    public static List<Entry> entriesFor(ResourceLocation lootTable) {
        if (lootTable == null) {
            return List.of();
        }
        List<Entry> result = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.lootTable().equals(lootTable)) {
                result.add(entry);
            }
        }
        return result;
    }

    static void replaceEntries(List<Entry> parsed) {
        entries = List.copyOf(parsed);
    }

    static List<Entry> parseEntries(ResourceLocation fileId, JsonObject root) {
        List<Entry> result = new ArrayList<>();
        JsonArray entries = GsonHelper.getAsJsonArray(root, "entries");
        for (int i = 0; i < entries.size(); i++) {
            JsonObject entry = GsonHelper.convertToJsonObject(entries.get(i), fileId + " entry " + i);
            result.add(parseEntry(fileId, entry));
        }
        return result;
    }

    static boolean shouldReplace(JsonObject root) {
        return GsonHelper.getAsBoolean(root, "replace", false);
    }

    private static Entry parseEntry(ResourceLocation fileId, JsonObject entry) {
        ResourceLocation lootTable = new ResourceLocation(GsonHelper.getAsString(entry, "loot_table"));
        ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(entry, "item"));
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(itemId);
        if (item.isEmpty()) {
            throw new IllegalArgumentException("Unknown item " + itemId + " in " + fileId);
        }
        int count = Math.max(1, GsonHelper.getAsInt(entry, "count", 1));
        double chance = Mth.clamp(GsonHelper.getAsDouble(entry, "chance", 1.0D), 0.0D, 1.0D);
        ResourceLocation dragonId = entry.has("dragon_id")
                ? new ResourceLocation(GsonHelper.getAsString(entry, "dragon_id"))
                : null;
        String configKey = GsonHelper.getAsString(entry, "config_key", null);
        return new Entry(lootTable, item.get(), count, chance, dragonId, configKey);
    }

    public record Entry(ResourceLocation lootTable,
                        Item item,
                        int count,
                        double chance,
                        ResourceLocation dragonId,
                        String configKey) {
        public double resolvedChance() {
            double resolved = chance;
            if (dragonId != null && configKey != null) {
                DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(dragonId);
                resolved = config.extraDouble(configKey, chance);
            }
            return Mth.clamp(resolved, 0.0D, 1.0D);
        }

        public ItemStack createStack() {
            return new ItemStack(item, count);
        }
    }
}
