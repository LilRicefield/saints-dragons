package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.item.RaevyxArmorItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

public final class ModArmors {
    private ModArmors() {}

    public static final Supplier<Item> RAEVYX_ARMOR_HELMET =
            ModItems.REGISTER.register("raevyx_armor_helmet",
                    () -> new RaevyxArmorItem(
                            ArmorMaterials.DIAMOND,
                            ArmorItem.Type.HELMET,
                            new Item.Properties().rarity(Rarity.RARE)
                    ));

    public static final Supplier<Item> RAEVYX_ARMOR_CHESTPLATE =
            ModItems.REGISTER.register("raevyx_armor_chestplate",
                    () -> new RaevyxArmorItem(
                            ArmorMaterials.DIAMOND,
                            ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().rarity(Rarity.RARE)
                    ));

    public static final Supplier<Item> RAEVYX_ARMOR_LEGGINGS =
            ModItems.REGISTER.register("raevyx_armor_leggings",
                    () -> new RaevyxArmorItem(
                            ArmorMaterials.DIAMOND,
                            ArmorItem.Type.LEGGINGS,
                            new Item.Properties().rarity(Rarity.RARE)
                    ));

    public static final Supplier<Item> RAEVYX_ARMOR_BOOTS =
            ModItems.REGISTER.register("raevyx_armor_boots",
                    () -> new RaevyxArmorItem(
                            ArmorMaterials.DIAMOND,
                            ArmorItem.Type.BOOTS,
                            new Item.Properties().rarity(Rarity.RARE)
                    ));

    public static void init() {}
}
