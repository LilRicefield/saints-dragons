package com.leon.saintsdragons.forge.world;

import com.leon.saintsdragons.common.world.DragonBiomeMatcher;
import com.leon.saintsdragons.common.world.DragonSpawnRegistry;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModTags;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;


public final class AddDragonsBiomeModifier implements BiomeModifier {
    public static final Codec<AddDragonsBiomeModifier> CODEC = Codec.unit(AddDragonsBiomeModifier::new);

    private AddDragonsBiomeModifier() {
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.MODIFY) {
            return;
        }

        try {
            for (DragonSpawnRegistry.DragonSpawnEntry entry : DragonSpawnRegistry.getAll()) {
                int weight = entry.weight().getAsInt();
                int minGroupSize = entry.minGroupSize().getAsInt();
                int maxGroupSize = entry.maxGroupSize().getAsInt();

                if (weight <= 0) {
                    continue;
                }

                if (DragonBiomeMatcher.isAllowed(biome, entry.biomeTag())) {
                    addSpawn(
                            builder,
                            entry.category(),
                            entry.entityType().get(),
                            weight,
                            minGroupSize,
                            maxGroupSize
                    );
                }
            }
            if (DragonBiomeMatcher.isAllowed(biome, ModTags.Biomes.HAS_MOOP)) {
                addSpawn(
                        builder,
                        MobCategory.WATER_AMBIENT,
                        ModEntities.MOOP.get(),
                        4,
                        1,
                        1
                );
            }
            if (DragonBiomeMatcher.isAllowed(biome, ModTags.Biomes.HAS_MOSSBACK)) {
                addSpawn(
                        builder,
                        MobCategory.CREATURE,
                        ModEntities.MOSSBACK.get(),
                        10,
                        1,
                        2
                );
            }
        } catch (IllegalStateException e) {
            // Config not loaded yet during datagen or early worldgen, skip spawn modification
        }
    }

    private static void addSpawn(ModifiableBiomeInfo.BiomeInfo.Builder builder,
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

        @SuppressWarnings("unchecked")
        EntityType<? extends Mob> mobType = (EntityType<? extends Mob>) entityType;
        MobSpawnSettings.SpawnerData spawnerData = new MobSpawnSettings.SpawnerData(mobType, weight, minGroupSize, maxGroupSize);

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
