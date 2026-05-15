package com.leon.saintsdragons.common.world;

import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.function.Predicate;

public final class DragonBiomeMatcher {
    private DragonBiomeMatcher() {
    }

    public static boolean isAllowed(Holder<Biome> biome, TagKey<Biome> biomeTag) {
        return biome != null && biomeTag != null && biome.is(biomeTag);
    }

    public static boolean isAllowed(Predicate<TagKey<Biome>> hasTag, TagKey<Biome> biomeTag) {
        return hasTag != null && biomeTag != null && hasTag.test(biomeTag);
    }
}
