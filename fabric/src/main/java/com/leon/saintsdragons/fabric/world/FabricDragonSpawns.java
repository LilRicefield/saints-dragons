package com.leon.saintsdragons.fabric.world;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Mirrors the Forge biome modifier using Fabric's BiomeModifications API.
 */
public final class FabricDragonSpawns {
    private static final TagKey<net.minecraft.world.level.biome.Biome> HAS_RAEVYX =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_raevyx"));
    private static final TagKey<net.minecraft.world.level.biome.Biome> HAS_STEGONAUT =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_stegonaut"));
    private static final TagKey<net.minecraft.world.level.biome.Biome> HAS_CINDERVANE =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_cindervane"));
    private static final TagKey<net.minecraft.world.level.biome.Biome> HAS_NULLJAW =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_nulljaw"));
    private static final TagKey<net.minecraft.world.level.biome.Biome> HAS_IGNIVORUS =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_ignivorus"));
    private static final TagKey<net.minecraft.world.level.biome.Biome> HAS_NULLJAW_EGGS =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_nulljaw_eggs"));
    private static final ResourceKey<PlacedFeature> CINDERVANE_EGG_PATCH =
            ResourceKey.create(Registries.PLACED_FEATURE, SaintsDragonsCommon.rl("cindervane_egg_patch"));
    private static final ResourceKey<PlacedFeature> NULLJAW_EGG_PATCH =
            ResourceKey.create(Registries.PLACED_FEATURE, SaintsDragonsCommon.rl("nulljaw_egg_patch"));

    private FabricDragonSpawns() {
    }

    public static void register() {
        if (SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.get() > 0) {
            registerSpawn(HAS_RAEVYX,
                    MobCategory.CREATURE,
                    ModEntities.RAEVYX.get(),
                    SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.get(),
                    SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.get(),
                    SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.get());
            registerAdditionalBiomes(SaintsDragonsConfig.RAEVYX_ADDITIONAL_BIOMES,
                    MobCategory.CREATURE,
                    ModEntities.RAEVYX.get(),
                    SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.get(),
                    SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.get(),
                    SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.get());
        }

        if (SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.get() > 0) {
            registerSpawn(HAS_STEGONAUT,
                    MobCategory.CREATURE,
                    ModEntities.STEGONAUT.get(),
                    SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.get(),
                    SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.get(),
                    SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.get());
            registerAdditionalBiomes(SaintsDragonsConfig.STEGONAUT_ADDITIONAL_BIOMES,
                    MobCategory.CREATURE,
                    ModEntities.STEGONAUT.get(),
                    SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.get(),
                    SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.get(),
                    SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.get());
        }

        if (SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.get() > 0) {
            registerSpawn(HAS_CINDERVANE,
                    MobCategory.CREATURE,
                    ModEntities.CINDERVANE.get(),
                    SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.get(),
                    SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.get(),
                    SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.get());
            registerAdditionalBiomes(SaintsDragonsConfig.CINDERVANE_ADDITIONAL_BIOMES,
                    MobCategory.CREATURE,
                    ModEntities.CINDERVANE.get(),
                    SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.get(),
                    SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.get(),
                    SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.get());
        }

        registerCindervaneEggs();
        registerNulljawEggs();

        if (SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.get() > 0) {
            registerSpawn(HAS_NULLJAW,
                    MobCategory.CREATURE,
                    ModEntities.NULLJAW.get(),
                    SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.get(),
                    SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.get(),
                    SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.get());
            registerAdditionalBiomes(SaintsDragonsConfig.NULLJAW_ADDITIONAL_BIOMES,
                    MobCategory.CREATURE,
                    ModEntities.NULLJAW.get(),
                    SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.get(),
                    SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.get(),
                    SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.get());
        }

        if (SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT.get() > 0) {
            registerSpawn(HAS_IGNIVORUS,
                    MobCategory.CREATURE,
                    ModEntities.IGNIVORUS.get(),
                    SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT.get(),
                    SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE.get(),
                    SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE.get());
            registerAdditionalBiomes(SaintsDragonsConfig.IGNIVORUS_ADDITIONAL_BIOMES,
                    MobCategory.CREATURE,
                    ModEntities.IGNIVORUS.get(),
                    SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT.get(),
                    SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE.get(),
                    SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE.get());
        }
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

    private static void registerSpawn(TagKey<net.minecraft.world.level.biome.Biome> biomeTag,
                                      MobCategory category,
                                      net.minecraft.world.entity.EntityType<?> entityType,
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
                BiomeSelectors.tag(biomeTag),
                category,
                entityType,
                weight,
                minGroupSize,
                maxGroupSize
        );
    }

    /**
     * Register spawns in additional biomes specified via config
     */
    private static void registerAdditionalBiomes(com.leon.saintsdragons.platform.ConfigHelper.ListValue configList,
                                                  MobCategory category,
                                                  net.minecraft.world.entity.EntityType<?> entityType,
                                                  int weight,
                                                  int minGroupSize,
                                                  int maxGroupSize) {
        if (weight <= 0 || minGroupSize <= 0 || maxGroupSize <= 0) {
            return;
        }
        if (minGroupSize > maxGroupSize) {
            minGroupSize = maxGroupSize;
        }

        try {
            java.util.List<String> biomes = configList.get();
            if (biomes.isEmpty()) {
                return;
            }

            // Parse biome IDs and register spawns
            for (String biomeIdStr : biomes) {
                try {
                    net.minecraft.resources.ResourceLocation biomeId = new net.minecraft.resources.ResourceLocation(biomeIdStr);
                    net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biomeKey =
                            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, biomeId);

                    BiomeModifications.addSpawn(
                            BiomeSelectors.includeByKey(biomeKey),
                            category,
                            entityType,
                            weight,
                            minGroupSize,
                            maxGroupSize
                    );
                } catch (Exception e) {
                    SaintsDragonsCommon.LOGGER.warn("Invalid biome ID in config: {}", biomeIdStr);
                }
            }
        } catch (Exception e) {
            // Config not loaded or error, skip
        }
    }

    private static void registerAdditionalFeatures(com.leon.saintsdragons.platform.ConfigHelper.ListValue configList,
                                                   ResourceKey<PlacedFeature> featureKey) {
        try {
            java.util.List<String> biomes = configList.get();
            if (biomes.isEmpty()) {
                return;
            }

            for (String biomeIdStr : biomes) {
                try {
                    net.minecraft.resources.ResourceLocation biomeId = new net.minecraft.resources.ResourceLocation(biomeIdStr);
                    net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biomeKey =
                            net.minecraft.resources.ResourceKey.create(Registries.BIOME, biomeId);

                    BiomeModifications.addFeature(
                            BiomeSelectors.includeByKey(biomeKey),
                            GenerationStep.Decoration.VEGETAL_DECORATION,
                            featureKey
                    );
                } catch (Exception e) {
                    SaintsDragonsCommon.LOGGER.warn("Invalid biome ID in config: {}", biomeIdStr);
                }
            }
        } catch (Exception e) {
            // Config not loaded or error, skip
        }
    }
}
