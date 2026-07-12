package com.leon.saintsdragons.forge.world;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

import java.util.Optional;


public final class AddConditionalFeaturesBiomeModifier implements BiomeModifier {
    public static final Codec<AddConditionalFeaturesBiomeModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(m -> m.biomes),
                    PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(m -> m.features),
                    GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(m -> m.step),
                    Codec.STRING.fieldOf("config_key").forGetter(m -> m.configKey),
                    ResourceLocation.CODEC.optionalFieldOf("default_biome_tag").forGetter(m ->
                            Optional.ofNullable(m.defaultBiomeTag).map(TagKey::location))
            ).apply(instance, AddConditionalFeaturesBiomeModifier::new)
    );

    private final HolderSet<Biome> biomes;
    private final HolderSet<PlacedFeature> features;
    private final GenerationStep.Decoration step;
    private final String configKey;
    private final TagKey<Biome> defaultBiomeTag;

    public AddConditionalFeaturesBiomeModifier(HolderSet<Biome> biomes,
                                               HolderSet<PlacedFeature> features,
                                               GenerationStep.Decoration step,
                                               String configKey,
                                               Optional<ResourceLocation> defaultBiomeTag) {
        this.biomes = biomes;
        this.features = features;
        this.step = step;
        this.configKey = configKey;
        this.defaultBiomeTag = defaultBiomeTag
                .map(id -> TagKey.create(Registries.BIOME, id))
                .orElse(null);
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.MODIFY || !isEnabled(configKey) || !isBiomeAllowed(biome)) {
            return;
        }
        var generation = builder.getGenerationSettings();
        for (Holder<PlacedFeature> feature : features) {
            generation.addFeature(step, feature);
        }
    }

    private static boolean isEnabled(String configKey) {
        return switch (configKey) {
            default -> true;
        };
    }

    private boolean isBiomeAllowed(Holder<Biome> biome) {
        return biomes.contains(biome) || (defaultBiomeTag != null && biome.is(defaultBiomeTag));
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
