package com.leon.saintsdragons.server.entity.base;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.RandomSource;

public final class DragonVariantSet {
    private final DragonVariant[] variants;
    private final Map<String, Integer> nameMap;
    private final int maxId;
    private final int totalWeight;

    private DragonVariantSet(DragonVariant[] variants) {
        if (variants.length == 0) {
            throw new IllegalArgumentException("Dragon variant set must not be empty");
        }

        Map<String, Integer> names = new LinkedHashMap<>();
        Set<Integer> ids = new HashSet<>();
        int highestId = 0;
        int weightSum = 0;

        for (DragonVariant variant : variants) {
            if (names.put(variant.name(), variant.id()) != null) {
                throw new IllegalArgumentException("Duplicate dragon variant name: " + variant.name());
            }
            if (!ids.add(variant.id())) {
                throw new IllegalArgumentException("Duplicate dragon variant id: " + variant.id());
            }
            highestId = Math.max(highestId, variant.id());
            weightSum += variant.weight();
        }

        if (!names.containsKey("default")) {
            throw new IllegalArgumentException("Dragon variant set must include a default variant");
        }
        if (weightSum <= 0) {
            throw new IllegalArgumentException("Dragon variant set must have at least one weighted variant");
        }

        this.variants = variants;
        this.nameMap = Collections.unmodifiableMap(new LinkedHashMap<>(names));
        this.maxId = highestId;
        this.totalWeight = weightSum;
    }

    public static DragonVariantSet of(DragonVariant... variants) {
        return new DragonVariantSet(variants.clone());
    }

    public int maxId() {
        return maxId;
    }

    public Map<String, Integer> nameMap() {
        return nameMap;
    }

    public int roll(RandomSource random) {
        int roll = random.nextInt(totalWeight);
        for (DragonVariant variant : variants) {
            roll -= variant.weight();
            if (roll < 0) {
                return variant.id();
            }
        }
        return nameMap.get("default");
    }
}
