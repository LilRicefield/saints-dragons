package com.leon.saintsdragons.fabric.world;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.world.DragonBiomeMatcher;
import com.leon.saintsdragons.common.world.DragonSpawnRegistry;
import com.leon.saintsdragons.platform.ConfigHelper;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.function.Supplier;


public final class FabricDragonSpawns {
    private static final TagKey<Biome> HAS_CINDERVANE =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_cindervane"));
    private static final TagKey<Biome> HAS_VARASUCHUS_EGGS =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_varasuchus_eggs"));
    private static final ResourceKey<PlacedFeature> CINDERVANE_EGG_PATCH =
            ResourceKey.create(Registries.PLACED_FEATURE, SaintsDragonsCommon.rl("cindervane_egg_patch"));
    private static final ResourceKey<PlacedFeature> VARASUCHUS_EGG_PATCH =
            ResourceKey.create(Registries.PLACED_FEATURE, SaintsDragonsCommon.rl("varasuchus_egg_patch"));

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
                    additionalBiomes,
                    excludedBiomes,
                    entry.category(),
                    entityType,
                    weight,
                    minGroupSize,
                    maxGroupSize
            );
        }

        registerCindervaneEggs();
        registerVarasuchusEggs();
    }

    private static void registerCindervaneEggs() {
        if (!SaintsDragonsConfig.CINDERVANE_EGG_BLOCK_WORLDGEN.get()) {
            return;
        }
        BiomeModifications.addFeature(
                context -> DragonBiomeMatcher.isAllowed(
                        context.getBiomeKey().location(),
                        context::hasTag,
                        HAS_CINDERVANE,
                        SaintsDragonsConfig.CINDERVANE_ADDITIONAL_BIOMES,
                        SaintsDragonsConfig.CINDERVANE_EXCLUDED_BIOMES
                ),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                CINDERVANE_EGG_PATCH
        );
    }

    private static void registerVarasuchusEggs() {
        if (!SaintsDragonsConfig.VARASUCHUS_EGG_BLOCK_WORLDGEN.get()) {
            return;
        }
        BiomeModifications.addFeature(
                context -> DragonBiomeMatcher.isAllowed(
                        context.getBiomeKey().location(),
                        context::hasTag,
                        HAS_VARASUCHUS_EGGS,
                        SaintsDragonsConfig.VARASUCHUS_ADDITIONAL_BIOMES,
                        SaintsDragonsConfig.VARASUCHUS_EXCLUDED_BIOMES
                ),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                VARASUCHUS_EGG_PATCH
        );
    }

    private static void registerSpawn(TagKey<Biome> biomeTag,
                                      ConfigHelper.ListValue additionalBiomes,
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
                context -> DragonBiomeMatcher.isAllowed(
                        context.getBiomeKey().location(),
                        context::hasTag,
                        biomeTag,
                        additionalBiomes,
                        excludedBiomes
                ),
                category,
                entityType,
                weight,
                minGroupSize,
                maxGroupSize
        );
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
