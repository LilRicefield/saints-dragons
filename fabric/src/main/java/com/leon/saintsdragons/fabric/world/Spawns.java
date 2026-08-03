package com.leon.saintsdragons.fabric.world;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.common.world.DragonBiomeMatcher;
import com.leon.saintsdragons.common.world.DragonSpawnRegistry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;


public final class Spawns {
    private static final ResourceKey<PlacedFeature> DEEPSLATE_WORLDROOT_ORE =
            ResourceKey.create(Registries.PLACED_FEATURE, SaintsDragonsCommon.rl("deepslate_worldroot_ore"));
    private static final ResourceKey<PlacedFeature> DRAGONHEART_ORE =
            ResourceKey.create(Registries.PLACED_FEATURE, SaintsDragonsCommon.rl("dragonheart_ore"));

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

        registerDeepslateWorldrootOre();
        registerDragonheartOre();
        registerMoopSpawn();
        registerMossbackSpawn();
    }

    private static void registerMoopSpawn() {
        registerSpawn(
                ModTags.Biomes.HAS_MOOP,
                MobCategory.WATER_AMBIENT,
                ModEntities.MOOP.get(),
                SaintsDragonsConfig.MOOP_SPAWN_WEIGHT.get(),
                SaintsDragonsConfig.MOOP_MIN_GROUP_SIZE.get(),
                SaintsDragonsConfig.MOOP_MAX_GROUP_SIZE.get()
        );
    }

    private static void registerMossbackSpawn() {
        registerSpawn(
                ModTags.Biomes.HAS_MOSSBACK,
                MobCategory.CREATURE,
                ModEntities.MOSSBACK.get(),
                SaintsDragonsConfig.MOSSBACK_SPAWN_WEIGHT.get(),
                SaintsDragonsConfig.MOSSBACK_MIN_GROUP_SIZE.get(),
                SaintsDragonsConfig.MOSSBACK_MAX_GROUP_SIZE.get()
        );
    }

    private static void registerDeepslateWorldrootOre() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                DEEPSLATE_WORLDROOT_ORE
        );
    }

    private static void registerDragonheartOre() {
        BiomeModifications.addFeature(
                context -> context.hasTag(ModTags.Biomes.HAS_DRAGONHEART_ORE),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                DRAGONHEART_ORE
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
