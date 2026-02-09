package com.leon.saintsdragons.forge.world;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * Forge biome modifier that mirrors Fabric's runtime spawn registration.
 * The JSON file only needs to point at this serializer; all spawn weights come from config.
 */
public final class AddDragonsBiomeModifier implements BiomeModifier {
    public static final Codec<AddDragonsBiomeModifier> CODEC = Codec.unit(AddDragonsBiomeModifier::new);

    private static final TagKey<Biome> HAS_RAEVYX =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_raevyx"));
    private static final TagKey<Biome> HAS_STEGONAUT =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_stegonaut"));
    private static final TagKey<Biome> HAS_CINDERVANE =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_cindervane"));
    private static final TagKey<Biome> HAS_NULLJAW =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_nulljaw"));
    private static final TagKey<Biome> HAS_IGNIVORUS =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_ignivorus"));

    private AddDragonsBiomeModifier() {
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.MODIFY) {
            return;
        }

        try {
            if (shouldSpawnInBiome(biome, HAS_RAEVYX, SaintsDragonsConfig.RAEVYX_ADDITIONAL_BIOMES, SaintsDragonsConfig.RAEVYX_EXCLUDED_BIOMES)) {
                addSpawn(builder,
                        MobCategory.CREATURE,
                        ModEntities.RAEVYX.get(),
                        SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.get());
            }

            if (shouldSpawnInBiome(biome, HAS_STEGONAUT, SaintsDragonsConfig.STEGONAUT_ADDITIONAL_BIOMES, SaintsDragonsConfig.STEGONAUT_EXCLUDED_BIOMES)) {
                addSpawn(builder,
                        MobCategory.CREATURE,
                        ModEntities.STEGONAUT.get(),
                        SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.get());
            }

            if (shouldSpawnInBiome(biome, HAS_CINDERVANE, SaintsDragonsConfig.CINDERVANE_ADDITIONAL_BIOMES, SaintsDragonsConfig.CINDERVANE_EXCLUDED_BIOMES)) {
                addSpawn(builder,
                        MobCategory.CREATURE,
                        ModEntities.CINDERVANE.get(),
                        SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.get());
            }

            if (shouldSpawnInBiome(biome, HAS_NULLJAW, SaintsDragonsConfig.NULLJAW_ADDITIONAL_BIOMES, SaintsDragonsConfig.NULLJAW_EXCLUDED_BIOMES)) {
                addSpawn(builder,
                        MobCategory.CREATURE,
                        ModEntities.NULLJAW.get(),
                        SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.get());
            }

            if (shouldSpawnInBiome(biome, HAS_IGNIVORUS, SaintsDragonsConfig.IGNIVORUS_ADDITIONAL_BIOMES, SaintsDragonsConfig.IGNIVORUS_EXCLUDED_BIOMES)) {
                addSpawn(builder,
                        MobCategory.CREATURE,
                        ModEntities.IGNIVORUS.get(),
                        SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE.get());
            }
        } catch (IllegalStateException e) {
            // Config not loaded yet during datagen or early worldgen, skip spawn modification
        }
    }

    /**
     * Check if a biome is in the configured additional biomes list
     */
    private static boolean isInConfigBiomes(Holder<Biome> biome, com.leon.saintsdragons.platform.ConfigHelper.ListValue configList) {
        try {
            ResourceLocation biomeId = biome.unwrapKey()
                    .map(net.minecraft.resources.ResourceKey::location)
                    .orElse(null);
            if (biomeId == null) {
                return false;
            }
            return configList.get().stream()
                    .map(AddDragonsBiomeModifier::normalizeBiomeId)
                    .anyMatch(id -> id != null && biomeId.equals(id));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a biome matches any configured biome tag entries (format: #namespace:path).
     */
    private static boolean isInConfigBiomeTags(Holder<Biome> biome, com.leon.saintsdragons.platform.ConfigHelper.ListValue configList) {
        try {
            return configList.get().stream()
                    .map(AddDragonsBiomeModifier::normalizeBiomeTag)
                    .anyMatch(tag -> tag != null && biome.is(tag));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean shouldSpawnInBiome(Holder<Biome> biome,
                                              TagKey<Biome> defaultTag,
                                              com.leon.saintsdragons.platform.ConfigHelper.ListValue additionalBiomes,
                                              com.leon.saintsdragons.platform.ConfigHelper.ListValue excludedBiomes) {
        boolean explicitlyIncluded = isInConfigBiomes(biome, additionalBiomes)
                || isInConfigBiomeTags(biome, additionalBiomes);
        boolean explicitlyExcluded = isInConfigBiomes(biome, excludedBiomes)
                || isInConfigBiomeTags(biome, excludedBiomes);
        boolean defaultAllowed = biome.is(defaultTag) && !explicitlyExcluded;
        return explicitlyIncluded || defaultAllowed;
    }

    /**
     * Accept both fully-qualified IDs (e.g. "minecraft:plains")
     * and path-only IDs (e.g. "plains"), defaulting to minecraft namespace.
     */
    private static ResourceLocation normalizeBiomeId(String raw) {
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
    private static TagKey<Biome> normalizeBiomeTag(String raw) {
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

    private static void addSpawn(ModifiableBiomeInfo.BiomeInfo.Builder builder,
                                 MobCategory category,
                                 EntityType<? extends Mob> entityType,
                                 int weight,
                                 int minGroupSize,
                                 int maxGroupSize) {
        if (weight <= 0 || minGroupSize <= 0 || maxGroupSize <= 0) {
            return;
        }
        if (minGroupSize > maxGroupSize) {
            minGroupSize = maxGroupSize;
        }

        MobSpawnSettings.SpawnerData spawnerData =
                new MobSpawnSettings.SpawnerData(entityType, weight, minGroupSize, maxGroupSize);

        var spawnSettings = builder.getMobSpawnSettings();
        boolean alreadyPresent = spawnSettings.getSpawner(category).stream()
                .anyMatch(existing -> existing.type == entityType);

        if (!alreadyPresent) {
            spawnSettings.addSpawn(category, spawnerData);
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}

