package com.leon.saintsdragons.common.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

public final class BiomeConfigHelper {
    private BiomeConfigHelper() {
    }

    /**
     * Accept both fully-qualified IDs (e.g. "minecraft:plains")
     * and path-only IDs (e.g. "plains"), defaulting to minecraft namespace.
     */
    @Nullable
    public static ResourceLocation normalizeBiomeId(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }
        String candidate = trimmed.contains(":") ? trimmed : "minecraft:" + trimmed;
        return ResourceLocation.tryParse(candidate);
    }

    /**
     * Parse biome tag entries from config, using "#namespace:path" syntax.
     */
    @Nullable
    public static TagKey<Biome> normalizeBiomeTag(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("#")) {
            return null;
        }
        String tagId = trimmed.substring(1).trim();
        if (tagId.isEmpty()) {
            return null;
        }
        String candidate = tagId.contains(":") ? tagId : "minecraft:" + tagId;
        ResourceLocation rl = ResourceLocation.tryParse(candidate);
        if (rl == null) {
            return null;
        }
        return TagKey.create(Registries.BIOME, rl);
    }
}
