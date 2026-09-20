package com.leon.saintsdragons.client.ui.codex;

import com.leon.saintsdragons.common.codex.DragonCodexRegistry;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class CodexPortraitRegistry {
    private static final Map<ResourceLocation, Definition> ENTRIES = new ConcurrentHashMap<>();

    private CodexPortraitRegistry() {
    }

    public static void register(ResourceLocation entityTypeId, Definition definition) {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        Objects.requireNonNull(definition, "definition");
        if (ENTRIES.putIfAbsent(entityTypeId, definition) != null) {
            throw new IllegalArgumentException("Duplicate Codex portrait: " + entityTypeId);
        }
    }

    @Nullable
    public static Definition get(String savedType) {
        return ENTRIES.get(DragonCodexRegistry.speciesId(savedType));
    }

    public record Frame(int scale, int offsetX, int offsetY) {
        public Frame {
            if (scale <= 0) {
                throw new IllegalArgumentException("Portrait scale must be positive");
            }
        }
    }

    public record Definition(Function<CodexDragonEntry, ResourceLocation> model,
                             Function<CodexDragonEntry, ResourceLocation> texture,
                             Frame adultFrame, Frame babyFrame) {
        public Definition {
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(texture, "texture");
            Objects.requireNonNull(adultFrame, "adultFrame");
            Objects.requireNonNull(babyFrame, "babyFrame");
        }

        public Frame frame(boolean baby) {
            return baby ? babyFrame : adultFrame;
        }
    }
}
