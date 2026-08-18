package com.leon.saintsdragons.common.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public final class ConventionalTags {
    private static final String COMMON_NAMESPACE = "c";

    private ConventionalTags() {
    }

    public static final class Blocks {
        public static final TagKey<Block> NEEDS_NETHERITE_TOOL = tag("needs_netherite_tool");

        private Blocks() {
        }

        private static TagKey<Block> tag(String path) {
            return TagKey.create(Registries.BLOCK, id(path));
        }
    }

    public static final class Items {
        public static final TagKey<Item> ARMORS = tag("armors");
        public static final TagKey<Item> ARMOR_BOOTS = tag("armors/boots");
        public static final TagKey<Item> ARMOR_CHESTPLATES = tag("armors/chestplates");
        public static final TagKey<Item> ARMOR_HELMETS = tag("armors/helmets");
        public static final TagKey<Item> ARMOR_LEGGINGS = tag("armors/leggings");

        private Items() {
        }

        private static TagKey<Item> tag(String path) {
            return TagKey.create(Registries.ITEM, id(path));
        }
    }

    public static final class Biomes {
        public static final TagKey<Biome> JUNGLE = tag("jungle");
        public static final TagKey<Biome> IS_JUNGLE = tag("is_jungle");

        private Biomes() {
        }

        private static TagKey<Biome> tag(String path) {
            return TagKey.create(Registries.BIOME, id(path));
        }
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(COMMON_NAMESPACE, path);
    }
}
