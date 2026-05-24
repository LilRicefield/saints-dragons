package com.leon.saintsdragons.fabric.world;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.common.world.DragonBiomeMatcher;
import com.leon.saintsdragons.common.world.DragonSpawnRegistry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;


public final class Spawns {
    private static final ResourceKey<PlacedFeature> CINDERVANE_EGG_PATCH =
            ResourceKey.create(Registries.PLACED_FEATURE, SaintsDragonsCommon.rl("cindervane_egg_patch"));
    private static final ResourceKey<PlacedFeature> VARASUCHUS_EGG_PATCH =
            ResourceKey.create(Registries.PLACED_FEATURE, SaintsDragonsCommon.rl("varasuchus_egg_patch"));

    private Spawns() {
    }

    public static void register() {
        for (DragonSpawnRegistry.DragonSpawnEntry entry : DragonSpawnRegistry.getAll()) {
            int weight = entry.weight().getAsInt();
            int minGroupSize = entry.minGroupSize().getAsInt();
            int maxGroupSize = entry.maxGroupSize().getAsInt();
            EntityType<?> entityType = entry.entityType().get();

            if (weight <= 0) {
                continue;
            }

            registerSpawn(
                    entry.biomeTag(),
                    entry.category(),
                    entityType,
                    weight,
                    minGroupSize,
                    maxGroupSize
            );
        }

        registerCindervaneEggs();
        registerVarasuchusEggs();
        registerMoopSpawn();
        registerMossbackSpawn();
    }

    private static void registerMoopSpawn() {
        registerSpawn(
                ModTags.Biomes.HAS_MOOP,
                MobCategory.WATER_AMBIENT,
                ModEntities.MOOP.get(),
                12,
                2,
                2
        );
    }

    private static void registerMossbackSpawn() {
        registerSpawn(
                ModTags.Biomes.HAS_MOSSBACK,
                MobCategory.CREATURE,
                ModEntities.MOSSBACK.get(),
                10,
                1,
                2
        );
    }

    private static void registerCindervaneEggs() {
        if (!SaintsDragonsConfig.CINDERVANE_EGG_BLOCK_WORLDGEN.get()) {
            return;
        }
        BiomeModifications.addFeature(
                context -> DragonBiomeMatcher.isAllowed(context::hasTag, ModTags.Biomes.HAS_CINDERVANE),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                CINDERVANE_EGG_PATCH
        );
    }

    private static void registerVarasuchusEggs() {
        if (!SaintsDragonsConfig.VARASUCHUS_EGG_BLOCK_WORLDGEN.get()) {
            return;
        }
        BiomeModifications.addFeature(
                context -> DragonBiomeMatcher.isAllowed(context::hasTag, ModTags.Biomes.HAS_VARASUCHUS_EGGS),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                VARASUCHUS_EGG_PATCH
        );
    }

    private static void registerSpawn(TagKey<Biome> biomeTag,
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
                context -> DragonBiomeMatcher.isAllowed(context::hasTag, biomeTag),
                category,
                entityType,
                weight,
                minGroupSize,
                maxGroupSize
        );
    }
}
