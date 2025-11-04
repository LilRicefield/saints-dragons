package com.leon.saintsdragons.fabric.world;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;

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
    private static final TagKey<net.minecraft.world.level.biome.Biome> HAS_NULLJAW_LAND =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_nulljaw_land"));
    private static final TagKey<net.minecraft.world.level.biome.Biome> HAS_NULLJAW_WATER =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_nulljaw_water"));

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
        }

        if (SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.get() > 0) {
            registerSpawn(HAS_STEGONAUT,
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
        }

        if (SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.get() > 0) {
            registerSpawn(HAS_NULLJAW_LAND,
                    MobCategory.CREATURE,
                    ModEntities.NULLJAW.get(),
                    SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.get(),
                    SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.get(),
                    SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.get());
            registerSpawn(HAS_NULLJAW_WATER,
                    MobCategory.CREATURE,
                    ModEntities.NULLJAW.get(),
                    SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.get(),
                    SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.get(),
                    SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.get());
        }
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
}
