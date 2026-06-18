package com.leon.saintsdragons.server.entity.npc.chatter;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IvyChatterReloadListener extends SimplePreparableReloadListener<Map<String, List<String>>> {
    private static final IvyChatterReloadListener INSTANCE = new IvyChatterReloadListener();
    private static final String DIRECTORY = "ivy_chatter";
    private static final String EXTENSION = ".txt";

    private IvyChatterReloadListener() {
    }

    public static IvyChatterReloadListener getInstance() {
        return INSTANCE;
    }

    @Override
    protected @NotNull Map<String, List<String>> prepare(@NotNull ResourceManager resourceManager,
                                                         @NotNull ProfilerFiller profiler) {
        Map<String, List<String>> pools = new LinkedHashMap<>();
        resourceManager.listResources(DIRECTORY, id -> id.getPath().endsWith(EXTENSION))
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> readPool(entry.getKey(), entry.getValue(), pools));
        return pools;
    }

    private static void readPool(ResourceLocation id, Resource resource, Map<String, List<String>> pools) {
        String pool = poolName(id);
        List<String> lines = pools.computeIfAbsent(pool, ignored -> new ArrayList<>());
        try (BufferedReader reader = resource.openAsReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    lines.add(trimmed);
                }
            }
        } catch (IOException exception) {
            SaintsDragonsCommon.LOGGER.error("Failed to read Ivy chatter file {}", id, exception);
        }
    }

    private static String poolName(ResourceLocation id) {
        String path = id.getPath();
        return path.substring(DIRECTORY.length() + 1, path.length() - EXTENSION.length());
    }

    @Override
    protected void apply(@NotNull Map<String, List<String>> pools,
                         @NotNull ResourceManager resourceManager,
                         @NotNull ProfilerFiller profiler) {
        IvyChatterRegistry.replace(pools);
    }
}
