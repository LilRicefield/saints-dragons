package com.leon.saintsdragons.server.loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class DragonChestLootReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final DragonChestLootReloadListener INSTANCE = new DragonChestLootReloadListener();
    private static volatile ResourceManager loadedManager;

    private DragonChestLootReloadListener() {
        super(GSON, "dragon_chest_loot");
    }

    public static DragonChestLootReloadListener getInstance() {
        return INSTANCE;
    }

    public static void ensureLoaded(ResourceManager resourceManager) {
        if (loadedManager == resourceManager) {
            return;
        }
        synchronized (DragonChestLootReloadListener.class) {
            if (loadedManager == resourceManager) {
                return;
            }
            applyJsonMap(readJsonMap(resourceManager));
            loadedManager = resourceManager;
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap,
                         @NotNull ResourceManager resourceManager,
                         @NotNull ProfilerFiller profiler) {
        applyJsonMap(jsonMap);
        loadedManager = resourceManager;
    }

    private static void applyJsonMap(Map<ResourceLocation, JsonElement> jsonMap) {
        boolean replace = jsonMap.entrySet().stream().anyMatch(DragonChestLootReloadListener::requestsReplace);
        List<DragonChestLootRegistry.Entry> parsed = new ArrayList<>();
        jsonMap.entrySet().stream()
                .filter(entry -> !replace || !DragonChestLootRegistry.DEFAULTS_FILE.equals(entry.getKey()))
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> parseFile(entry.getKey(), entry.getValue(), parsed));
        DragonChestLootRegistry.replaceEntries(parsed);
    }

    private static Map<ResourceLocation, JsonElement> readJsonMap(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> result = new java.util.HashMap<>();
        resourceManager.listResources("dragon_chest_loot", id -> id.getPath().endsWith(".json")).forEach((file, resource) -> {
            ResourceLocation fileId = fileToReloadId(file);
            try (BufferedReader reader = resource.openAsReader()) {
                result.put(fileId, GsonHelper.fromJson(GSON, reader, JsonElement.class));
            } catch (Exception exception) {
                SaintsDragonsCommon.LOGGER.error("Failed to read dragon chest loot file {}", file, exception);
            }
        });
        return result;
    }

    private static ResourceLocation fileToReloadId(ResourceLocation file) {
        String path = file.getPath();
        path = path.substring("dragon_chest_loot/".length(), path.length() - ".json".length());
        return new ResourceLocation(file.getNamespace(), path);
    }

    private static boolean requestsReplace(Map.Entry<ResourceLocation, JsonElement> entry) {
        try {
            JsonObject root = GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey().toString());
            return DragonChestLootRegistry.shouldReplace(root);
        } catch (Exception exception) {
            SaintsDragonsCommon.LOGGER.error("Failed to inspect dragon chest loot file {}", entry.getKey(), exception);
            return false;
        }
    }

    private static void parseFile(ResourceLocation fileId,
                                  JsonElement element,
                                  List<DragonChestLootRegistry.Entry> parsed) {
        try {
            JsonObject root = GsonHelper.convertToJsonObject(element, fileId.toString());
            parsed.addAll(DragonChestLootRegistry.parseEntries(fileId, root));
        } catch (Exception exception) {
            SaintsDragonsCommon.LOGGER.error("Failed to parse dragon chest loot file {}", fileId, exception);
        }
    }
}
