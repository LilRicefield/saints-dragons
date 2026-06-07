package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.item.FixedPotionItem;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class ModPotionItems {
    private ModPotionItems() {}

    public static final Supplier<Item> POTION_OF_TIDEGUARD =
            ModItems.REGISTER.register("potion_of_tideguard",
                    () -> new FixedPotionItem(
                            new Item.Properties().stacksTo(1),
                            ModPotions.TIDEGUARD
                    ));

    public static final Supplier<Item> POTION_OF_SEARING =
            ModItems.REGISTER.register("potion_of_searing",
                    () -> new FixedPotionItem(
                            new Item.Properties().stacksTo(1),
                            ModPotions.SEARING
                    ));

    public static void init() {}
}
