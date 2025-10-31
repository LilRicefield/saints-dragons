package com.leon.saintsdragons.common.world;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Naturalist-style BiomeModifier that adds dragon spawns based on config values.
 * Triggered by a single JSON file: data/saintsdragons/forge/biome_modifier/add_dragons.json
 */
public class AddDragonsBiomeModifier implements BiomeModifier {

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD) {
            // Raevyx (Lightning Dragon)
            if (SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.get() > 0) {
                addDragonSpawn(builder, biome, "saintsdragons:has_raevyx",
                        MobCategory.CREATURE, ModEntities.RAEVYX.get(),
                        SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.get());
            }

            // Stegonaut (Primitive Drake)
            if (SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.get() > 0) {
                addDragonSpawn(builder, biome, "saintsdragons:has_stegonaut",
                        MobCategory.CREATURE, ModEntities.STEGONAUT.get(),
                        SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.get());
            }

            // Cindervane (Amphithere)
            if (SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.get() > 0) {
                addDragonSpawn(builder, biome, "saintsdragons:has_cindervane",
                        MobCategory.CREATURE, ModEntities.CINDERVANE.get(),
                        SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.get());
            }

            // Nulljaw (Rift Drake) - Semi-aquatic, uses CREATURE category with ON_GROUND placement
            if (SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.get() > 0) {
                addDragonSpawn(builder, biome, "saintsdragons:has_nulljaw",
                        MobCategory.CREATURE, ModEntities.NULLJAW.get(),
                        SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.get(),
                        SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.get(),
                        SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.get());
            }
        }
    }

    /**
     * Helper method to add dragon spawns if the biome has the specified tag
     */
    private void addDragonSpawn(ModifiableBiomeInfo.BiomeInfo.Builder builder, Holder<Biome> biome,
                                 String tagName, MobCategory category, EntityType<?> entityType,
                                 int weight, int minGroupSize, int maxGroupSize) {
        // Validate spawn parameters to prevent crashes
        if (weight <= 0 || minGroupSize <= 0 || maxGroupSize <= 0) {
            return; // Invalid parameters, skip
        }

        // Ensure minGroupSize doesn't exceed maxGroupSize
        if (minGroupSize > maxGroupSize) {
            minGroupSize = maxGroupSize;
        }

        // Check if this biome has the dragon's spawn tag
        if (biome.getTagKeys().anyMatch(tag -> tag.location().toString().equals(tagName))) {
            builder.getMobSpawnSettings().addSpawn(category,
                    new MobSpawnSettings.SpawnerData(entityType, weight, minGroupSize, maxGroupSize));
        }
    }

    @Override
    public @NotNull Codec<? extends BiomeModifier> codec() {
        return SDWorldRegistry.ADD_DRAGONS_CODEC.get();
    }
}
