package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.common.item.DragonAllyBookItem;
import com.leon.saintsdragons.common.item.DrakeBinderItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> REGISTER =
            DeferredRegister.create(ForgeRegistries.ITEMS, SaintsDragons.MOD_ID);

    public static final RegistryObject<Item> LIGHTNING_DRAGON_SPAWN_EGG =
            REGISTER.register("lightning_dragon_spawn_egg",
                    () -> new ForgeSpawnEggItem(
                            ModEntities.LIGHTNING_DRAGON,
                            0x000000, 0x8B0000, // base=black, spots=dark red
                            new Item.Properties()
                    )
            );

    public static final RegistryObject<Item> PRIMITIVE_DRAKE_SPAWN_EGG =
            REGISTER.register("primitive_drake_spawn_egg",
                    () -> new ForgeSpawnEggItem(
                            ModEntities.PRIMITIVE_DRAKE,
                            0x8B4513, 0xCD853F, // base=brown, spots=sandy brown
                            new Item.Properties()
                    )
            );

    public static final RegistryObject<Item> AMPHITHERE_SPAWN_EGG =
            REGISTER.register("amphithere_spawn_egg",
                    () -> new ForgeSpawnEggItem(
                            ModEntities.AMPHITHERE,
                            0x4A9D8F, 0xE3D99F, // base=teal plume, spots=sandy underside
                            new Item.Properties()
                    )
            );

    public static final RegistryObject<Item> DRAGON_ALLY_BOOK =
            REGISTER.register("dragon_ally_book",
                    () -> new DragonAllyBookItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0) // Unbreakable
                    )
            );

    public static final RegistryObject<Item> DRAKE_BINDER =
            REGISTER.register("drake_binder",
                    () -> new DrakeBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0) // Unbreakable
                    )
            );
}
