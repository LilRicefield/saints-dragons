package com.leon.saintsdragons.forge.data;

import com.leon.saintsdragons.common.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class SaintsDragonBiomeTagsProvider extends BiomeTagsProvider {
    public SaintsDragonBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        tag(ModTags.Biomes.HAS_CINDERVANE)
                .addOptionalTag(rl("c", "mountain"))
                .addOptionalTag(rl("c", "mountain_peak"))
                .addOptionalTag(rl("c", "mountain_slope"))
                .addOptionalTag(rl("c", "is_mountain"))
                .addOptionalTag(rl("c", "is_mountain/peak"))
                .addOptionalTag(rl("forge", "is_mountain"))
                .addOptionalTag(rl("forge", "is_peak"))
                .addOptionalTag(rl("forge", "is_slope"))
                .addOptionalTag(rl("terralith", "skylands"))
                .addOptionalTag(rl("terralith", "volcanic"))
                .addOptionalTag(rl("biomeswevegone", "mountain"))
                .add(Biomes.STONY_PEAKS)
                .add(Biomes.JAGGED_PEAKS)
                .add(Biomes.FROZEN_PEAKS)
                .add(Biomes.SNOWY_SLOPES)
                .add(Biomes.GROVE)
                .add(Biomes.MEADOW);

        tag(ModTags.Biomes.HAS_IGNIVORUS)
                .addOptionalTag(rl("c", "climate_hot"))
                .addOptionalTag(rl("c", "is_hot/overworld"))
                .addOptionalTag(rl("c", "is_plains"))
                .addOptionalTag(rl("c", "plains"))
                .addOptionalTag(rl("forge", "is_plains"))
                .addOptionalTag(rl("forge", "is_hot/overworld"))
                .addOptionalTag(rl("c", "mountain"))
                .addOptionalTag(rl("c", "mountain_peak"))
                .addOptionalTag(rl("c", "mountain_slope"))
                .addOptionalTag(rl("c", "is_mountain"))
                .addOptionalTag(rl("forge", "is_mountain"))
                .addOptionalTag(rl("forge", "is_peak"))
                .addOptionalTag(rl("forge", "is_slope"))
                .addOptionalTag(rl("terralith", "skylands"))
                .addOptionalTag(rl("terralith", "volcanic"))
                .addOptionalTag(rl("terralith", "shrublands"))
                .addOptionalTag(rl("biomeswevegone", "plains"))
                .addOptionalTag(rl("biomeswevegone", "mountain"))
                .add(Biomes.PLAINS)
                .add(Biomes.MEADOW)
                .add(Biomes.WINDSWEPT_HILLS)
                .add(Biomes.SUNFLOWER_PLAINS);

        tag(ModTags.Biomes.HAS_IVY_HOUSE)
                .addOptionalTag(rl("c", "forest"))
                .addOptionalTag(rl("c", "is_forest"))
                .addOptionalTag(rl("forge", "is_forest"))
                .add(Biomes.FOREST)
                .add(Biomes.BIRCH_FOREST)
                .add(Biomes.OLD_GROWTH_BIRCH_FOREST)
                .add(Biomes.DARK_FOREST)
                .add(Biomes.FLOWER_FOREST)
                .add(Biomes.TAIGA)
                .add(Biomes.OLD_GROWTH_PINE_TAIGA)
                .add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
                .add(Biomes.SNOWY_TAIGA);

        tag(ModTags.Biomes.HAS_NULLJAW)
                .add(Biomes.END_BARRENS);

        tag(ModTags.Biomes.HAS_RAEVYX)
                .addOptionalTag(rl("c", "mountain"))
                .addOptionalTag(rl("c", "mountain_peak"))
                .addOptionalTag(rl("c", "mountain_slope"))
                .addOptionalTag(rl("c", "is_mountain"))
                .addOptionalTag(rl("c", "is_plains"))
                .addOptionalTag(rl("c", "plains"))
                .addOptionalTag(rl("c", "climate_temperate"))
                .addOptionalTag(rl("forge", "is_plains"))
                .addOptionalTag(rl("forge", "is_mountain"))
                .addOptionalTag(rl("forge", "is_peak"))
                .addOptionalTag(rl("forge", "is_slope"))
                .addOptionalTag(rl("c", "is_temperate/overworld"))
                .addOptionalTag(rl("terralith", "skylands"))
                .addOptionalTag(rl("terralith", "shrublands"))
                .addOptionalTag(rl("biomeswevegone", "plains"))
                .addOptionalTag(rl("biomeswevegone", "mountain"))
                .add(Biomes.PLAINS)
                .add(Biomes.MEADOW)
                .add(Biomes.WINDSWEPT_HILLS)
                .add(Biomes.SUNFLOWER_PLAINS)
                .add(Biomes.CHERRY_GROVE)
                .add(Biomes.WINDSWEPT_FOREST)
                .add(Biomes.STONY_PEAKS)
                .add(Biomes.JAGGED_PEAKS);

        tag(ModTags.Biomes.HAS_STEGONAUT)
                .add(Biomes.LUSH_CAVES);

        addVarasuchusBiomes(ModTags.Biomes.HAS_VARASUCHUS);
        addVarasuchusBiomes(ModTags.Biomes.HAS_VARASUCHUS_EGGS);

        tag(ModTags.Biomes.HAS_VOLITANS)
                .addOptionalTag(rl("c", "ocean"))
                .addOptionalTag(rl("c", "is_ocean"))
                .addOptionalTag(rl("forge", "is_ocean"))
                .add(Biomes.OCEAN)
                .add(Biomes.DEEP_OCEAN)
                .add(Biomes.COLD_OCEAN)
                .add(Biomes.DEEP_COLD_OCEAN)
                .add(Biomes.FROZEN_OCEAN)
                .add(Biomes.DEEP_FROZEN_OCEAN)
                .add(Biomes.LUKEWARM_OCEAN)
                .add(Biomes.DEEP_LUKEWARM_OCEAN)
                .add(Biomes.WARM_OCEAN);

        tag(ModTags.Biomes.HAS_MOOP)
                .addOptionalTag(rl("c", "river"))
                .addOptionalTag(rl("c", "is_river"))
                .addOptionalTag(rl("c", "ocean"))
                .addOptionalTag(rl("c", "is_ocean"))
                .addOptionalTag(rl("forge", "is_river"))
                .addOptionalTag(rl("forge", "is_ocean"))
                .add(Biomes.RIVER)
                .add(Biomes.FROZEN_RIVER)
                .add(Biomes.OCEAN)
                .add(Biomes.DEEP_OCEAN)
                .add(Biomes.COLD_OCEAN)
                .add(Biomes.DEEP_COLD_OCEAN)
                .add(Biomes.FROZEN_OCEAN)
                .add(Biomes.DEEP_FROZEN_OCEAN)
                .add(Biomes.LUKEWARM_OCEAN)
                .add(Biomes.DEEP_LUKEWARM_OCEAN)
                .add(Biomes.WARM_OCEAN)
                .add(Biomes.SWAMP)
                .add(Biomes.MANGROVE_SWAMP);
    }

    private void addVarasuchusBiomes(net.minecraft.tags.TagKey<Biome> tag) {
        tag(tag)
                .addOptionalTag(rl("c", "beach"))
                .addOptionalTag(rl("c", "stony_shores"))
                .addOptionalTag(rl("c", "is_beach"))
                .addOptionalTag(rl("c", "is_stony_shores"))
                .addOptionalTag(rl("forge", "is_beach"))
                .add(Biomes.BEACH)
                .add(Biomes.SNOWY_BEACH)
                .add(Biomes.STONY_SHORE);
    }

    private static ResourceLocation rl(String namespace, String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse(namespace + ":" + path));
    }
}
