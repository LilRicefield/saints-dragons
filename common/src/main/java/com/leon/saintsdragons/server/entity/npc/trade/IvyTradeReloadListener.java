package com.leon.saintsdragons.server.entity.npc.trade;

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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class IvyTradeReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final IvyTradeReloadListener INSTANCE = new IvyTradeReloadListener();

    private IvyTradeReloadListener() {
        super(GSON, "ivy_trades");
    }

    public static IvyTradeReloadListener getInstance() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap,
                         @NotNull ResourceManager resourceManager,
                         @NotNull ProfilerFiller profiler) {
        List<IvyTradeRegistry.OfferSource> parsed = new ArrayList<>();
        jsonMap.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> parseFile(entry.getKey(), entry.getValue(), parsed));
        IvyTradeRegistry.replaceDatapackTrades(parsed);
    }

    private static void parseFile(ResourceLocation fileId,
                                  JsonElement element,
                                  List<IvyTradeRegistry.OfferSource> parsed) {
        try {
            JsonObject root = GsonHelper.convertToJsonObject(element, fileId.toString());
            parsed.addAll(IvyTradeRegistry.parseOfferSources(fileId, root));
        } catch (Exception exception) {
            SaintsDragonsCommon.LOGGER.error("Failed to parse Ivy trade file {}", fileId, exception);
        }
    }
}
