package com.leon.saintsdragons.forge.world;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
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
    private static final TagKey<Biome> HAS_NULLJAW_LAND =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_nulljaw_land"));
    private static final TagKey<Biome> HAS_NULLJAW_WATER =
            TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl("has_nulljaw_water"));

    private AddDragonsBiomeModifier() {
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.MODIFY) {
            return;
        }

        try {
            if (biome.is(HAS_RAEVYX)) {
                addSpawn(builder,
                        MobCategory.CREATURE,
                        ModEntities.RAEVYX.get(),
                        SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.get());
            }

            if (biome.is(HAS_STEGONAUT)) {
                addSpawn(builder,
                        MobCategory.CREATURE,
                        ModEntities.STEGONAUT.get(),
                        SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.get());
            }

            if (biome.is(HAS_CINDERVANE)) {
                addSpawn(builder,
                        MobCategory.CREATURE,
                        ModEntities.CINDERVANE.get(),
                        SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.get());
            }

            if (biome.is(HAS_NULLJAW_LAND) || biome.is(HAS_NULLJAW_WATER)) {
                addSpawn(builder,
                        MobCategory.CREATURE,
                        ModEntities.NULLJAW.get(),
                        SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.get());
            }
        } catch (IllegalStateException e) {
            // Config not loaded yet during datagen or early worldgen, skip spawn modification
        }
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

