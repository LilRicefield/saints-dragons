package com.leon.saintsdragons.common.world;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.platform.ConfigHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class DragonSpawnRegistry {
    private static final List<DragonSpawnEntry> ENTRIES = createEntries();

    private DragonSpawnRegistry() {
    }

    public static List<DragonSpawnEntry> getAll() {
        return ENTRIES;
    }

    private static List<DragonSpawnEntry> createEntries() {
        List<DragonSpawnEntry> entries = new ArrayList<>();

        add(entries,
                SaintsDragonsCommon.rl("raevyx"),
                ModEntities.RAEVYX,
                "has_raevyx",
                MobCategory.CREATURE,
                () -> SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.get(),
                () -> SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.RAEVYX_ADDITIONAL_BIOMES,
                () -> SaintsDragonsConfig.RAEVYX_EXCLUDED_BIOMES);

        add(entries,
                SaintsDragonsCommon.rl("stegonaut"),
                ModEntities.STEGONAUT,
                "has_stegonaut",
                MobCategory.CREATURE,
                () -> SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.get(),
                () -> SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.STEGONAUT_ADDITIONAL_BIOMES,
                () -> SaintsDragonsConfig.STEGONAUT_EXCLUDED_BIOMES);

        add(entries,
                SaintsDragonsCommon.rl("cindervane"),
                ModEntities.CINDERVANE,
                "has_cindervane",
                MobCategory.CREATURE,
                () -> SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.get(),
                () -> SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.CINDERVANE_ADDITIONAL_BIOMES,
                () -> SaintsDragonsConfig.CINDERVANE_EXCLUDED_BIOMES);

        add(entries,
                SaintsDragonsCommon.rl("nulljaw"),
                ModEntities.NULLJAW,
                "has_nulljaw",
                MobCategory.CREATURE,
                () -> SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.get(),
                () -> SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.NULLJAW_ADDITIONAL_BIOMES,
                () -> SaintsDragonsConfig.NULLJAW_EXCLUDED_BIOMES);

        add(entries,
                SaintsDragonsCommon.rl("ignivorus"),
                ModEntities.IGNIVORUS,
                "has_ignivorus",
                MobCategory.CREATURE,
                () -> SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT.get(),
                () -> SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.IGNIVORUS_ADDITIONAL_BIOMES,
                () -> SaintsDragonsConfig.IGNIVORUS_EXCLUDED_BIOMES);

        return List.copyOf(entries);
    }

    private static void add(List<DragonSpawnEntry> entries,
                            ResourceLocation id,
                            Supplier<? extends EntityType<?>> entityType,
                            String biomeTagPath,
                            MobCategory category,
                            IntSupplier weight,
                            IntSupplier minGroupSize,
                            IntSupplier maxGroupSize,
                            Supplier<ConfigHelper.ListValue> additionalBiomes,
                            Supplier<ConfigHelper.ListValue> excludedBiomes) {
        entries.add(new DragonSpawnEntry(
                id,
                entityType,
                TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl(biomeTagPath)),
                category,
                weight,
                minGroupSize,
                maxGroupSize,
                additionalBiomes,
                excludedBiomes
        ));
    }

    public record DragonSpawnEntry(
            ResourceLocation id,
            Supplier<? extends EntityType<?>> entityType,
            TagKey<Biome> biomeTag,
            MobCategory category,
            IntSupplier weight,
            IntSupplier minGroupSize,
            IntSupplier maxGroupSize,
            Supplier<ConfigHelper.ListValue> additionalBiomes,
            Supplier<ConfigHelper.ListValue> excludedBiomes
    ) {
    }
}
