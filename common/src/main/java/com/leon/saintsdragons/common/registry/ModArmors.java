package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.item.DragonlordArmorItem;
import com.leon.saintsdragons.common.item.BloodTempestArmorItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

public final class ModArmors {
    private ModArmors() {}

    public static final Supplier<Item> BLOOD_TEMPEST_HELMET =
            ModItems.REGISTER.register("blood_tempest_helmet",
                    () -> new BloodTempestArmorItem(
                            ArmorMaterials.DIAMOND,
                            ArmorItem.Type.HELMET,
                            new Item.Properties().rarity(Rarity.RARE)
                    ));

    public static final Supplier<Item> BLOOD_TEMPEST_CHESTPLATE =
            ModItems.REGISTER.register("blood_tempest_chestplate",
                    () -> new BloodTempestArmorItem(
                            ArmorMaterials.DIAMOND,
                            ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().rarity(Rarity.RARE)
                    ));

    public static final Supplier<Item> BLOOD_TEMPEST_LEGGINGS =
            ModItems.REGISTER.register("blood_tempest_leggings",
                    () -> new BloodTempestArmorItem(
                            ArmorMaterials.DIAMOND,
                            ArmorItem.Type.LEGGINGS,
                            new Item.Properties().rarity(Rarity.RARE)
                    ));

    public static final Supplier<Item> BLOOD_TEMPEST_BOOTS =
            ModItems.REGISTER.register("blood_tempest_boots",
                    () -> new BloodTempestArmorItem(
                            ArmorMaterials.DIAMOND,
                            ArmorItem.Type.BOOTS,
                            new Item.Properties().rarity(Rarity.RARE)
                    ));

    public static final Supplier<Item> DRAGONLORD_HELMET =
            ModItems.REGISTER.register("dragonlord_helmet",
                    () -> new DragonlordArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET,
                            new Item.Properties().rarity(Rarity.RARE)));

    public static final Supplier<Item> DRAGONLORD_CHESTPLATE =
            ModItems.REGISTER.register("dragonlord_chestplate",
                    () -> new DragonlordArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().rarity(Rarity.RARE)));

    public static final Supplier<Item> DRAGONLORD_LEGGINGS =
            ModItems.REGISTER.register("dragonlord_leggings",
                    () -> new DragonlordArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.LEGGINGS,
                            new Item.Properties().rarity(Rarity.RARE)));

    public static final Supplier<Item> DRAGONLORD_BOOTS =
            ModItems.REGISTER.register("dragonlord_boots",
                    () -> new DragonlordArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.BOOTS,
                            new Item.Properties().rarity(Rarity.RARE)));

    public static void init() {}
}
