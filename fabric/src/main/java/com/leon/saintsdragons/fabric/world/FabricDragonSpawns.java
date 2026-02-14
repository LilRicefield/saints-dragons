package com.leon.saintsdragons.fabric.world;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.util.BiomeConfigHelper;
import com.leon.saintsdragons.common.world.DragonSpawnRegistry;
import com.leon.saintsdragons.platform.ConfigHelper;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.function.Supplier;

/**
 * Mirrors the Forge biome modifier using Fabric's BiomeModifications API.
 */
public final class FabricDragonSpawns {
    private static final TagKey<Biome> HAS_CINDERVANE =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_cindervane"));
    private static final TagKey<Biome> HAS_NULLJAW_EGGS =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_nulljaw_eggs"));
    private static final ResourceKey<PlacedFeature> CINDERVANE_EGG_PATCH =
            ResourceKey.create(Registries.PLACED_FEATURE, SaintsDragonsCommon.rl("cindervane_egg_patch"));
    private static final ResourceKey<PlacedFeature> NULLJAW_EGG_PATCH =
            ResourceKey.create(Registries.PLACED_FEATURE, SaintsDragonsCommon.rl("nulljaw_egg_patch"));

    private FabricDragonSpawns() {
    }

    public static void register() {
        for (DragonSpawnRegistry.DragonSpawnEntry entry : DragonSpawnRegistry.getAll()) {
            int weight = entry.weight().getAsInt();
            int minGroupSize = entry.minGroupSize().getAsInt();
            int maxGroupSize = entry.maxGroupSize().getAsInt();
            EntityType<?> entityType = entry.entityType().get();
            ConfigHelper.ListValue additionalBiomes = resolveConfigList(entry.additionalBiomes());
            ConfigHelper.ListValue excludedBiomes = resolveConfigList(entry.excludedBiomes());

            if (weight <= 0) {
                continue;
            }

            registerSpawn(
                    entry.biomeTag(),
                    excludedBiomes,
                    entry.category(),
                    entityType,
                    weight,
                    minGroupSize,
                    maxGroupSize
            );
            registerAdditionalBiomes(
                    entry.id(),
                    additionalBiomes,
                    entry.category(),
                    entityType,
                    weight,
                    minGroupSize,
                    maxGroupSize
            );
        }

        registerCindervaneEggs();
        registerNulljawEggs();
    }

    private static void registerCindervaneEggs() {
        if (!SaintsDragonsConfig.CINDERVANE_EGG_BLOCK_WORLDGEN.get()) {
            return;
        }
        BiomeModifications.addFeature(
                BiomeSelectors.tag(HAS_CINDERVANE),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                CINDERVANE_EGG_PATCH
        );

        registerAdditionalFeatures(SaintsDragonsConfig.CINDERVANE_ADDITIONAL_BIOMES, CINDERVANE_EGG_PATCH);
    }

    private static void registerNulljawEggs() {
        if (!SaintsDragonsConfig.NULLJAW_EGG_BLOCK_WORLDGEN.get()) {
            return;
        }
        BiomeModifications.addFeature(
                BiomeSelectors.tag(HAS_NULLJAW_EGGS),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                NULLJAW_EGG_PATCH
        );
    }

    private static void registerSpawn(TagKey<Biome> biomeTag,
                                      ConfigHelper.ListValue excludedBiomes,
                                      MobCategory category,
                                      EntityType<?> entityType,
                                      int weight,
                                      int minGroupSize,
                                      int maxGroupSize) {
        if (weight <= 0 || minGroupSize <= 0 || maxGroupSize <= 0) {
            return;
        }
        if (minGroupSize > maxGroupSize) {
            minGroupSize = maxGroupSize;
        }

        BiomeModifications.addSpawn(
                context -> context.hasTag(biomeTag) && !isBiomeExcluded(context, excludedBiomes),
                category,
                entityType,
                weight,
                minGroupSize,
                maxGroupSize
        );
    }

    private static void registerAdditionalBiomes(ResourceLocation spawnId,
                                                 ConfigHelper.ListValue configList,
                                                 MobCategory category,
                                                 EntityType<?> entityType,
                                                 int weight,
                                                 int minGroupSize,
                                                 int maxGroupSize) {
        if (configList == null) {
            return;
        }
        if (weight <= 0 || minGroupSize <= 0 || maxGroupSize <= 0) {
            return;
        }
        if (minGroupSize > maxGroupSize) {
            minGroupSize = maxGroupSize;
        }

        try {
            for (String rawEntry : configList.get()) {
                try {
                    TagKey<Biome> biomeTag = BiomeConfigHelper.normalizeBiomeTag(rawEntry);
                    if (biomeTag != null) {
                        BiomeModifications.addSpawn(
                                BiomeSelectors.tag(biomeTag),
                                category,
                                entityType,
                                weight,
                                minGroupSize,
                                maxGroupSize
                        );
                        continue;
                    }

                    ResourceLocation biomeId = BiomeConfigHelper.normalizeBiomeId(rawEntry);
                    if (biomeId == null) {
                        SaintsDragonsCommon.LOGGER.warn("Invalid biome ID or biome tag in config for {}: {}", spawnId, rawEntry);
                        continue;
                    }

                    ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeId);
                    BiomeModifications.addSpawn(
                            BiomeSelectors.includeByKey(biomeKey),
                            category,
                            entityType,
                            weight,
                            minGroupSize,
                            maxGroupSize
                    );
                } catch (Exception e) {
                    SaintsDragonsCommon.LOGGER.warn("Invalid biome ID or biome tag in config for {}: {}", spawnId, rawEntry);
                }
            }
        } catch (Exception e) {
            // Config not loaded or error, skip
        }
    }

    private static void registerAdditionalFeatures(ConfigHelper.ListValue configList,
                                                   ResourceKey<PlacedFeature> featureKey) {
        if (configList == null) {
            return;
        }
        try {
            for (String rawEntry : configList.get()) {
                try {
                    TagKey<Biome> biomeTag = BiomeConfigHelper.normalizeBiomeTag(rawEntry);
                    if (biomeTag != null) {
                        BiomeModifications.addFeature(
                                BiomeSelectors.tag(biomeTag),
                                GenerationStep.Decoration.VEGETAL_DECORATION,
                                featureKey
                        );
                        continue;
                    }

                    ResourceLocation biomeId = BiomeConfigHelper.normalizeBiomeId(rawEntry);
                    if (biomeId == null) {
                        SaintsDragonsCommon.LOGGER.warn("Invalid biome ID or biome tag in config: {}", rawEntry);
                        continue;
                    }
                    ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeId);
                    BiomeModifications.addFeature(
                            BiomeSelectors.includeByKey(biomeKey),
                            GenerationStep.Decoration.VEGETAL_DECORATION,
                            featureKey
                    );
                } catch (Exception e) {
                    SaintsDragonsCommon.LOGGER.warn("Invalid biome ID or biome tag in config: {}", rawEntry);
                }
            }
        } catch (Exception e) {
            // Config not loaded or error, skip
        }
    }

    private static boolean isBiomeExcluded(BiomeSelectionContext context, ConfigHelper.ListValue excludedBiomes) {
        if (excludedBiomes == null) {
            return false;
        }
        try {
            ResourceLocation biomeId = context.getBiomeKey().location();
            return excludedBiomes.get().stream().anyMatch(entry -> {
                TagKey<Biome> tag = BiomeConfigHelper.normalizeBiomeTag(entry);
                if (tag != null) {
                    return context.hasTag(tag);
                }
                ResourceLocation id = BiomeConfigHelper.normalizeBiomeId(entry);
                return id != null && biomeId.equals(id);
            });
        } catch (Exception e) {
            return false;
        }
    }

    private static ConfigHelper.ListValue resolveConfigList(Supplier<ConfigHelper.ListValue> supplier) {
        if (supplier == null) {
            return null;
        }
        try {
            return supplier.get();
        } catch (Exception e) {
            return null;
        }
    }
}
