package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class DragonSpeciesRegistry {
    private static final Map<ResourceLocation, Definition> ENTRIES = new LinkedHashMap<>();

    static {
        for (Dragons dragon : Dragons.values()) {
            register(dragon.getConfigId(), dragon.getConfigId(), dragon.getEntityType());
        }
    }

    private DragonSpeciesRegistry() {
    }

    public static Definition register(ResourceLocation id,
                                      Supplier<? extends EntityType<? extends DragonEntity>> entityType) {
        return register(id, id, entityType);
    }

    public static synchronized Definition register(ResourceLocation id, ResourceLocation attributesId,
                                                   Supplier<? extends EntityType<? extends DragonEntity>> entityType) {
        Definition definition = new Definition(id, attributesId, entityType);
        if (ENTRIES.putIfAbsent(id, definition) != null) {
            throw new IllegalArgumentException("Duplicate dragon species: " + id);
        }
        return definition;
    }

    @Nullable
    public static synchronized Definition get(ResourceLocation id) {
        return ENTRIES.get(id);
    }

    public static synchronized List<Definition> getAll() {
        return List.copyOf(ENTRIES.values());
    }

    public static ResourceLocation speciesId(DragonEntity dragon) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(dragon.getType());
    }

    public record Definition(ResourceLocation id, ResourceLocation attributesId,
                             Supplier<? extends EntityType<? extends DragonEntity>> entityType) {
        public Definition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(attributesId, "attributesId");
            Objects.requireNonNull(entityType, "entityType");
        }
    }
}
