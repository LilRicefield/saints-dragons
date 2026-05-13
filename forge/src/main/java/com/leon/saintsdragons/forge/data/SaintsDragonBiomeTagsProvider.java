package com.leon.saintsdragons.forge.data;

import com.leon.saintsdragons.common.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
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
                .addTag(BiomeTags.IS_MOUNTAIN)
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
                .addTag(BiomeTags.IS_MOUNTAIN)
                .addTag(BiomeTags.IS_HILL)
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
                .addTag(BiomeTags.IS_FOREST);

        tag(ModTags.Biomes.HAS_NULLJAW)
                .add(Biomes.END_BARRENS);

        tag(ModTags.Biomes.HAS_RAEVYX)
                .addTag(BiomeTags.IS_MOUNTAIN)
                .addTag(BiomeTags.IS_HILL)
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
                .addTag(BiomeTags.IS_OCEAN)
                .addOptionalTag(rl("c", "ocean"))
                .addOptionalTag(rl("c", "is_ocean"))
                .addOptionalTag(rl("forge", "is_ocean"));
    }

    private void addVarasuchusBiomes(net.minecraft.tags.TagKey<Biome> tag) {
        tag(tag)
                .addTag(BiomeTags.IS_BEACH)
                .addOptionalTag(rl("c", "beach"))
                .addOptionalTag(rl("c", "stony_shores"))
                .addOptionalTag(rl("c", "is_beach"))
                .addOptionalTag(rl("c", "is_stony_shores"))
                .addOptionalTag(rl("forge", "is_beach"))
                .add(Biomes.STONY_SHORE);
    }

    private static ResourceLocation rl(String namespace, String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse(namespace + ":" + path));
    }
}
