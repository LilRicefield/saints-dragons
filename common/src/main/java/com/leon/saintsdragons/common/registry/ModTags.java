package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    private ModTags() {
    }

    public static final class EntityTypes {
        public static final TagKey<EntityType<?>> DRAGONS = tag("dragons");
        public static final TagKey<EntityType<?>> RIDEABLE_DRAGONS = tag("rideable_dragons");
        public static final TagKey<EntityType<?>> TAMEABLE_DRAGONS = tag("tameable_dragons");
        public static final TagKey<EntityType<?>> FLYING_DRAGONS = tag("flying_dragons");
        public static final TagKey<EntityType<?>> GROUNDED_DRAGONS = tag("grounded_dragons");
        public static final TagKey<EntityType<?>> SWIMMING_DRAGONS = tag("swimming_dragons");
        public static final TagKey<EntityType<?>> CINDERVANE_TARGETS = tag("targets/cindervane");
        public static final TagKey<EntityType<?>> IGNIVORUS_TARGETS = tag("targets/ignivorus");
        public static final TagKey<EntityType<?>> RAEVYX_TARGETS = tag("targets/raevyx");
        public static final TagKey<EntityType<?>> STEGONAUT_TARGETS = tag("targets/stegonaut");
        public static final TagKey<EntityType<?>> VARASUCHUS_TARGETS = tag("targets/varasuchus");
        public static final TagKey<EntityType<?>> VOLITANS_TARGETS = tag("targets/volitans");
        public static final TagKey<EntityType<?>> IMMUNE_TO_ELECTRICITY = tag("immune_to/electricity");
        public static final TagKey<EntityType<?>> IMMUNE_TO_FIRE = tag("immune_to/fire");
        public static final TagKey<EntityType<?>> IMMUNE_TO_POISON = tag("immune_to/poison");

        private EntityTypes() {
        }

        private static TagKey<EntityType<?>> tag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, SaintsDragonsCommon.rl(name));
        }
    }

    public static final class Items {
        public static final TagKey<Item> DRAGON_FOODS = tag("dragon_foods");
        public static final TagKey<Item> DRAGON_EGGS = tag("dragon_eggs");
        public static final TagKey<Item> DRAGON_BINDERS = tag("dragon_binders");
        public static final TagKey<Item> DRAGON_BRUSHES = tag("dragon_brushes");
        public static final TagKey<Item> DRAGON_SCALES = tag("dragon_scales");
        public static final TagKey<Item> DRAGON_TEETH = tag("dragon_teeth");
        public static final TagKey<Item> DRAGON_HEARTS = tag("dragon_hearts");
        public static final TagKey<Item> DRAGON_SPINES = tag("dragon_spines");
        public static final TagKey<Item> DRAGON_PARTS = tag("dragon_parts");
        public static final TagKey<Item> DRAGON_DROPS = tag("dragon_drops");
        public static final TagKey<Item> DRAGON_SPAWN_EGGS = tag("dragon_spawn_eggs");
        public static final TagKey<Item> CINDERVANE_FOODS = tag("foods/cindervane");
        public static final TagKey<Item> IGNIVORUS_FOODS = tag("foods/ignivorus");
        public static final TagKey<Item> NULLJAW_FOODS = tag("foods/nulljaw");
        public static final TagKey<Item> RAEVYX_FOODS = tag("foods/raevyx");
        public static final TagKey<Item> STEGONAUT_FOODS = tag("foods/stegonaut");
        public static final TagKey<Item> VARASUCHUS_FOODS = tag("foods/varasuchus");
        public static final TagKey<Item> VOLITANS_FOODS = tag("foods/volitans");
        public static final TagKey<Item> DRACONIC_CRUCIBLE_FUEL_LEVEL_1 =
                tag("draconic_crucible/fuels/level_1");
        public static final TagKey<Item> DRACONIC_CRUCIBLE_FUEL_LEVEL_2 =
                tag("draconic_crucible/fuels/level_2");
        public static final TagKey<Item> DRACONIC_CRUCIBLE_FUEL_LEVEL_3 =
                tag("draconic_crucible/fuels/level_3");

        private Items() {
        }

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, SaintsDragonsCommon.rl(name));
        }
    }

    public static final class Blocks {
        public static final TagKey<Block> DRAGON_EGGS = tag("dragon_eggs");
        public static final TagKey<Block> CINDERVANE_EGGS = tag("eggs/cindervane");
        public static final TagKey<Block> IGNIVORUS_EGGS = tag("eggs/ignivorus");
        public static final TagKey<Block> RAEVYX_EGGS = tag("eggs/raevyx");
        public static final TagKey<Block> STEGONAUT_EGGS = tag("eggs/stegonaut");
        public static final TagKey<Block> VARASUCHUS_EGGS = tag("eggs/varasuchus");
        public static final TagKey<Block> VOLITANS_EGGS = tag("eggs/volitans");

        private Blocks() {
        }

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, SaintsDragonsCommon.rl(name));
        }
    }

    public static final class Biomes {
        public static final TagKey<Biome> HAS_CINDERVANE = tag("has_cindervane");
        public static final TagKey<Biome> HAS_IGNIVORUS = tag("has_ignivorus");
        public static final TagKey<Biome> HAS_IVY_HOUSE = tag("has_ivy_house");
        public static final TagKey<Biome> HAS_NULLJAW = tag("has_nulljaw");
        public static final TagKey<Biome> HAS_RAEVYX = tag("has_raevyx");
        public static final TagKey<Biome> HAS_STEGONAUT = tag("has_stegonaut");
        public static final TagKey<Biome> HAS_STEGONAUT_FALLBACK = tag("has_stegonaut_fallback");
        public static final TagKey<Biome> HAS_VARASUCHUS = tag("has_varasuchus");
        public static final TagKey<Biome> HAS_VARASUCHUS_EGGS = tag("has_varasuchus_eggs");
        public static final TagKey<Biome> HAS_VOLITANS = tag("has_volitans");
        public static final TagKey<Biome> HAS_VOLITANS_FALLBACK = tag("has_volitans_fallback");
        public static final TagKey<Biome> HAS_MOOP = tag("has_moop");
        public static final TagKey<Biome> HAS_MOSSBACK = tag("has_mossback");

        private Biomes() {
        }

        private static TagKey<Biome> tag(String name) {
            return TagKey.create(Registries.BIOME, SaintsDragonsCommon.rl(name));
        }
    }
}
