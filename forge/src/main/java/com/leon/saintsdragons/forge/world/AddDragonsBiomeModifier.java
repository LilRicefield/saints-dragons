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
            if (biome.is(HAS_RAEVYX) || isInConfigBiomes(biome, SaintsDragonsConfig.RAEVYX_ADDITIONAL_BIOMES)) {
                addSpawn(builder,
                        MobCategory.CREATURE,
                        ModEntities.RAEVYX.get(),
                        SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.get());
            }

            if (biome.is(HAS_STEGONAUT) || isInConfigBiomes(biome, SaintsDragonsConfig.STEGONAUT_ADDITIONAL_BIOMES)) {
                addSpawn(builder,
                        MobCategory.CREATURE,
                        ModEntities.STEGONAUT.get(),
                        SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.get());
            }

            if (biome.is(HAS_CINDERVANE) || isInConfigBiomes(biome, SaintsDragonsConfig.CINDERVANE_ADDITIONAL_BIOMES)) {
                addSpawn(builder,
                        MobCategory.CREATURE,
                        ModEntities.CINDERVANE.get(),
                        SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.get());
            }

            if (biome.is(HAS_NULLJAW) || isInConfigBiomes(biome, SaintsDragonsConfig.NULLJAW_ADDITIONAL_BIOMES)) {
                addSpawn(builder,
                        MobCategory.CREATURE,
                        ModEntities.NULLJAW.get(),
                        SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.get());
            }

            if (biome.is(HAS_IGNIVORUS) || isInConfigBiomes(biome, SaintsDragonsConfig.IGNIVORUS_ADDITIONAL_BIOMES)) {
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
            net.minecraft.resources.ResourceLocation biomeId = biome.unwrapKey()
                    .map(net.minecraft.resources.ResourceKey::location)
                    .orElse(null);
            if (biomeId == null) {
                return false;
            }

            String biomeIdStr = biomeId.toString();
            return configList.get().stream()
                    .anyMatch(s -> s.equals(biomeIdStr));
        } catch (Exception e) {
            return false;
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

