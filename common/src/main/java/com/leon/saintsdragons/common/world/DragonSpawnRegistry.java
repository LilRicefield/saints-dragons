package com.leon.saintsdragons.common.world;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModTags;
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
                ModTags.Biomes.HAS_RAEVYX,
                MobCategory.CREATURE,
                () -> SaintsDragonsConfig.isRaevyxCustomSpawningEnabled() ? 0 : SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.get(),
                () -> SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.get());

        add(entries,
                SaintsDragonsCommon.rl("stegonaut"),
                ModEntities.STEGONAUT,
                ModTags.Biomes.HAS_STEGONAUT_FALLBACK,
                MobCategory.CREATURE,
                () -> SaintsDragonsConfig.isStegonautCustomSpawningEnabled() ? 0 : SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.get(),
                () -> SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.get());

        add(entries,
                SaintsDragonsCommon.rl("cindervane"),
                ModEntities.CINDERVANE,
                ModTags.Biomes.HAS_CINDERVANE,
                MobCategory.CREATURE,
                () -> SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.get(),
                () -> SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.get());

        add(entries,
                SaintsDragonsCommon.rl("varasuchus"),
                ModEntities.VARASUCHUS,
                ModTags.Biomes.HAS_VARASUCHUS,
                MobCategory.CREATURE,
                () -> SaintsDragonsConfig.VARASUCHUS_SPAWN_WEIGHT.get(),
                () -> SaintsDragonsConfig.VARASUCHUS_MIN_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.VARASUCHUS_MAX_GROUP_SIZE.get());

        add(entries,
                SaintsDragonsCommon.rl("ignivorus"),
                ModEntities.IGNIVORUS,
                ModTags.Biomes.HAS_IGNIVORUS,
                MobCategory.CREATURE,
                () -> SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT.get(),
                () -> SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE.get());

        add(entries,
                SaintsDragonsCommon.rl("volitans"),
                ModEntities.VOLITANS,
                ModTags.Biomes.HAS_VOLITANS_FALLBACK,
                MobCategory.CREATURE,
                () -> SaintsDragonsConfig.isVolitansCustomSpawningEnabled() ? 0 : SaintsDragonsConfig.VOLITANS_SPAWN_WEIGHT.get(),
                () -> SaintsDragonsConfig.VOLITANS_MIN_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.VOLITANS_MAX_GROUP_SIZE.get());

        add(entries,
                SaintsDragonsCommon.rl("nulljaw"),
                ModEntities.NULLJAW,
                ModTags.Biomes.HAS_NULLJAW,
                MobCategory.MONSTER,
                () -> SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.get(),
                () -> SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.get(),
                () -> SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.get());

        return List.copyOf(entries);
    }

    private static void add(List<DragonSpawnEntry> entries,
                            ResourceLocation id,
                            Supplier<? extends EntityType<?>> entityType,
                            TagKey<Biome> biomeTag,
                            MobCategory category,
                            IntSupplier weight,
                            IntSupplier minGroupSize,
                            IntSupplier maxGroupSize) {
        entries.add(new DragonSpawnEntry(
                id,
                entityType,
                biomeTag,
                category,
                weight,
                minGroupSize,
                maxGroupSize
        ));
    }

    public record DragonSpawnEntry(
            ResourceLocation id,
            Supplier<? extends EntityType<?>> entityType,
            TagKey<Biome> biomeTag,
            MobCategory category,
            IntSupplier weight,
            IntSupplier minGroupSize,
            IntSupplier maxGroupSize
    ) {
    }
}
