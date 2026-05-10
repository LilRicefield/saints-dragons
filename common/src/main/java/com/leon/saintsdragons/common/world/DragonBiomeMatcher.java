package com.leon.saintsdragons.common.world;

import com.leon.saintsdragons.common.util.BiomeConfigHelper;
import com.leon.saintsdragons.platform.ConfigHelper;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.function.Predicate;

public final class DragonBiomeMatcher {
    private DragonBiomeMatcher() {
    }

    public static boolean isAllowed(Holder<Biome> biome,
                                    TagKey<Biome> defaultTag,
                                    ConfigHelper.ListValue additionalBiomes,
                                    ConfigHelper.ListValue excludedBiomes) {
        ResourceLocation biomeId = biome.unwrapKey()
                .map(net.minecraft.resources.ResourceKey::location)
                .orElse(null);
        if (biomeId == null) {
            return false;
        }
        return isAllowed(biomeId, biome::is, defaultTag, additionalBiomes, excludedBiomes);
    }

    public static boolean isAllowed(ResourceLocation biomeId,
                                    Predicate<TagKey<Biome>> hasTag,
                                    TagKey<Biome> defaultTag,
                                    ConfigHelper.ListValue additionalBiomes,
                                    ConfigHelper.ListValue excludedBiomes) {
        if (isInConfigBiomes(biomeId, excludedBiomes) || isInConfigBiomeTags(hasTag, excludedBiomes)) {
            return false;
        }
        return hasTag.test(defaultTag)
                || isInConfigBiomes(biomeId, additionalBiomes)
                || isInConfigBiomeTags(hasTag, additionalBiomes);
    }

    public static boolean isInConfigBiomes(ResourceLocation biomeId, ConfigHelper.ListValue configList) {
        if (biomeId == null || configList == null) {
            return false;
        }
        try {
            return configList.get().stream()
                    .map(BiomeConfigHelper::normalizeBiomeId)
                    .anyMatch(id -> id != null && biomeId.equals(id));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isInConfigBiomeTags(Predicate<TagKey<Biome>> hasTag, ConfigHelper.ListValue configList) {
        if (hasTag == null || configList == null) {
            return false;
        }
        try {
            return configList.get().stream()
                    .map(BiomeConfigHelper::normalizeBiomeTag)
                    .anyMatch(tag -> tag != null && hasTag.test(tag));
        } catch (Exception e) {
            return false;
        }
    }
}
