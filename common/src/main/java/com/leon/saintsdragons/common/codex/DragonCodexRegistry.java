package com.leon.saintsdragons.common.codex;

import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class DragonCodexRegistry {
    private static final Map<ResourceLocation, Definition> ENTRIES = new ConcurrentHashMap<>();

    static {
        register(new ResourceLocation("saintsdragons", "ignivorus"), new Definition(
                new ResourceLocation("saintsdragons", "ecology/ignivorus.txt"),
                "saintsdragons.gui.draconic_codex.ecology.ignivorus.page1",
                ModTags.Items.IGNIVORUS_FOODS, List.of(
                    new ResourceLocation("saintsdragons", "ignivorus_scale"),
                    new ResourceLocation("saintsdragons", "ignivorus_tooth"),
                    new ResourceLocation("saintsdragons", "ignivorus_heart"),
                    new ResourceLocation("saintsdragons", "ignivorus_egg"),
                    new ResourceLocation("saintsdragons", "ignivorus_wing_hide")
                ),
                new Care(true, true, true), () -> ModSounds.IGNIVORUS_GRUMBLE_1.get()));
        register(new ResourceLocation("saintsdragons", "atroxiia"), new Definition(
                new ResourceLocation("saintsdragons", "ecology/atroxiia.txt"),
                "saintsdragons.gui.draconic_codex.ecology.atroxiia.page1",
                ModTags.Items.ATROXIIA_FOODS, List.of(
                    new ResourceLocation("saintsdragons", "atroxiia_scale"),
                    new ResourceLocation("saintsdragons", "atroxiia_egg")
                ),
                new Care(true, true, true), () -> ModSounds.ATROXIIA_GRUMBLE_1.get()));
        register(new ResourceLocation("saintsdragons", "raevyx"), new Definition(
                new ResourceLocation("saintsdragons", "ecology/raevyx.txt"),
                "saintsdragons.gui.draconic_codex.ecology.raevyx.page1",
                ModTags.Items.RAEVYX_FOODS, List.of(
                    new ResourceLocation("saintsdragons", "raevyx_scale"),
                    new ResourceLocation("saintsdragons", "raevyx_egg"),
                    new ResourceLocation("saintsdragons", "raevyx_wing_hide"),
                    new ResourceLocation("saintsdragons", "raevyx_wingtalon")
                ),
                new Care(true, true, true), () -> ModSounds.RAEVYX_GRUMBLE_1.get()));
        register(new ResourceLocation("saintsdragons", "varasuchus"), new Definition(
                new ResourceLocation("saintsdragons", "ecology/varasuchus.txt"),
                "saintsdragons.gui.draconic_codex.ecology.varasuchus.page1",
                ModTags.Items.VARASUCHUS_FOODS, List.of(
                    new ResourceLocation("saintsdragons", "varasuchus_scale"),
                    new ResourceLocation("saintsdragons", "varasuchus_egg")
                ),
                new Care(true, true, true), () -> ModSounds.VARASUCHUS_GRUMBLE_1.get()));
        register(new ResourceLocation("saintsdragons", "cindervane"), new Definition(
                new ResourceLocation("saintsdragons", "ecology/cindervane.txt"),
                "saintsdragons.gui.draconic_codex.ecology.cindervane.page1",
                ModTags.Items.CINDERVANE_FOODS, List.of(
                    new ResourceLocation("saintsdragons", "cindervane_scale"),
                    new ResourceLocation("saintsdragons", "cindervane_egg")
                ),
                new Care(true, true, true), () -> ModSounds.CINDERVANE_GRUMBLE_1.get()));
        register(new ResourceLocation("saintsdragons", "stegonaut"), new Definition(
                new ResourceLocation("saintsdragons", "ecology/stegonaut.txt"),
                "saintsdragons.gui.draconic_codex.ecology.stegonaut.page1",
                ModTags.Items.STEGONAUT_FOODS, List.of(
                    new ResourceLocation("saintsdragons", "stegonaut_scale"),
                    new ResourceLocation("saintsdragons", "stegonaut_egg")
                ),
                new Care(true, true, true), () -> ModSounds.STEGONAUT_GRUMBLE_1.get()));
        register(new ResourceLocation("saintsdragons", "volitans"), new Definition(
                new ResourceLocation("saintsdragons", "ecology/volitans.txt"),
                "saintsdragons.gui.draconic_codex.ecology.volitans.page1",
                ModTags.Items.VOLITANS_FOODS, List.of(
                    new ResourceLocation("saintsdragons", "volitans_scale"),
                    new ResourceLocation("saintsdragons", "volitans_spine"),
                    new ResourceLocation("saintsdragons", "volitans_egg"),
                    new ResourceLocation("minecraft", "salmon"),
                    new ResourceLocation("minecraft", "cod"),
                    new ResourceLocation("minecraft", "tropical_fish"),
                    new ResourceLocation("minecraft", "pufferfish")
                ),
                new Care(true, true, true), () -> ModSounds.VOLITANS_GRUMBLE_3.get()));
        register(new ResourceLocation("saintsdragons", "nulljaw"), new Definition(
                new ResourceLocation("saintsdragons", "ecology/nulljaw.txt"),
                "saintsdragons.gui.draconic_codex.ecology.nulljaw.page1",
                null, List.of(),
                new Care(true, true, false), () -> ModSounds.NULLJAW_GRUMBLE_1.get()));
    }

    private DragonCodexRegistry() {
    }

    public static void register(ResourceLocation entityTypeId, Definition definition) {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        Objects.requireNonNull(definition, "definition");
        if (ENTRIES.putIfAbsent(entityTypeId, definition) != null) {
            throw new IllegalArgumentException("Duplicate Codex species: " + entityTypeId);
        }
    }

    public static ResourceLocation speciesId(String savedType) {
        ResourceLocation id = savedType == null ? null : ResourceLocation.tryParse(
                savedType.contains(":") ? savedType : "saintsdragons:" + savedType);
        return id == null ? new ResourceLocation("saintsdragons", "unknown") : id;
    }

    public static String speciesKey(DragonEntity dragon) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(dragon.getType());
        return id.getNamespace().equals("saintsdragons") ? id.getPath() : id.toString();
    }

    @Nullable
    public static Definition get(String savedType) {
        return ENTRIES.get(speciesId(savedType));
    }

    public record Care(boolean hunger, boolean happiness, boolean brushing) {
        public static final Care NONE = new Care(false, false, false);
    }

    public record Definition(ResourceLocation ecologyText, String ecologyTranslationKey,
                             @Nullable TagKey<Item> favoriteFoods, List<ResourceLocation> drops,
                             Care care, @Nullable Supplier<SoundEvent> selectionSound) {
        public Definition {
            Objects.requireNonNull(ecologyText, "ecologyText");
            Objects.requireNonNull(ecologyTranslationKey, "ecologyTranslationKey");
            drops = List.copyOf(drops);
            Objects.requireNonNull(care, "care");
        }
    }
}
