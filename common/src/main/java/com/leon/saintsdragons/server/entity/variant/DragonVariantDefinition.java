package com.leon.saintsdragons.server.entity.variant;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record DragonVariantDefinition(
        ResourceLocation id,
        ResourceLocation dragon,
        String name,
        int weight,
        int legacyId,
        @Nullable BiomeRestrictions allowedBiomes,
        @Nullable BiomeRestrictions bannedBiomes,
        AltitudeRestriction altitudeRestriction
) {
    public static final int NO_LEGACY_ID = -1;

    public DragonVariantDefinition {
        if (id == null) {
            throw new IllegalArgumentException("Dragon variant id must not be null");
        }
        if (dragon == null) {
            throw new IllegalArgumentException("Dragon variant dragon id must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Dragon variant name must not be blank");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("Dragon variant weight must be >= 0");
        }
        if (altitudeRestriction == null) {
            altitudeRestriction = AltitudeRestriction.ANY;
        }
    }

    public boolean hasLegacyId() {
        return legacyId >= 0;
    }

    public boolean hasAllowedBiomes() {
        return allowedBiomes != null && allowedBiomes.hasAny();
    }

    public boolean hasBannedBiomes() {
        return bannedBiomes != null && bannedBiomes.hasAny();
    }

    public String translationKey() {
        if (SaintsDragonVariantRegistry.SAINTS_DRAGONS.equals(id.getNamespace())) {
            return "saintsdragons.variant." + id.getPath().replace('/', '.');
        }
        return "saintsdragons.variant." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public record BiomeRestrictions(List<ResourceLocation> biomesById, List<ResourceLocation> biomesByTag) {
        public boolean hasBiomesByIdList() {
            return biomesById != null && !biomesById.isEmpty();
        }

        public boolean hasBiomesByTagList() {
            return biomesByTag != null && !biomesByTag.isEmpty();
        }

        public boolean hasAny() {
            return hasBiomesByIdList() || hasBiomesByTagList();
        }
    }

    public record AltitudeRestriction(int min, int max) {
        public static final AltitudeRestriction ANY = new AltitudeRestriction(Integer.MIN_VALUE, Integer.MAX_VALUE);

        public boolean allows(int y) {
            return y >= min && y <= max;
        }
    }
}
