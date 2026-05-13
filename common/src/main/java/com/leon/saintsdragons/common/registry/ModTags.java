package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
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
        public static final TagKey<Item> DRAGON_SPAWN_EGGS = tag("dragon_spawn_eggs");
        public static final TagKey<Item> CINDERVANE_FOODS = tag("foods/cindervane");
        public static final TagKey<Item> IGNIVORUS_FOODS = tag("foods/ignivorus");
        public static final TagKey<Item> NULLJAW_FOODS = tag("foods/nulljaw");
        public static final TagKey<Item> RAEVYX_FOODS = tag("foods/raevyx");
        public static final TagKey<Item> STEGONAUT_FOODS = tag("foods/stegonaut");
        public static final TagKey<Item> VARASUCHUS_FOODS = tag("foods/varasuchus");
        public static final TagKey<Item> VOLITANS_FOODS = tag("foods/volitans");

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
}
