package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.common.item.CindervaneBinderItem;
import com.leon.saintsdragons.common.item.DragonAllyBookItem;
import com.leon.saintsdragons.common.item.StegonautBinderItem;
import com.leon.saintsdragons.common.item.RaevyxBinderItem;
import com.leon.saintsdragons.common.item.NulljawBinderItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> REGISTER =
            DeferredRegister.create(ForgeRegistries.ITEMS, SaintsDragons.MOD_ID);

    public static final RegistryObject<Item> RAEVYX_SPAWN_EGG =
            REGISTER.register("raevyx_spawn_egg",
                    () -> new ForgeSpawnEggItem(
                            ModEntities.RAEVYX,
                            0x000000, 0x8B0000, // base=black, spots=dark red
                            new Item.Properties()
                    )
            );

    public static final RegistryObject<Item> STEGONAUT_SPAWN_EGG =
            REGISTER.register("stegonaut_spawn_egg",
                    () -> new ForgeSpawnEggItem(
                            ModEntities.STEGONAUT,
                            0x8B4513, 0xCD853F, // base=brown, spots=sandy brown
                            new Item.Properties()
                    )
            );

    public static final RegistryObject<Item> CINDERVANE_SPAWN_EGG =
            REGISTER.register("cindervane_spawn_egg",
                    () -> new ForgeSpawnEggItem(
                            ModEntities.CINDERVANE,
                            0x5E5E5E, 0xA7490D, // base=teal plume, spots=sandy underside
                            new Item.Properties()
                    )
            );

    public static final RegistryObject<Item> NULLJAW_SPAWN_EGG =
            REGISTER.register("nulljaw_spawn_egg",
                    () -> new ForgeSpawnEggItem(
                            ModEntities.NULLJAW,
                            0x2C3E50, 0x16A085,
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

    public static final RegistryObject<Item> STEGONAUT_BINDER =
            REGISTER.register("stegonaut_binder",
                    () -> new StegonautBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0) // Unbreakable
                    )
            );

    public static final RegistryObject<Item> RAEVYX_BINDER =
            REGISTER.register("raevyx_binder",
                    () -> new RaevyxBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0) // Unbreakable
                    )
            );

    public static final RegistryObject<Item> CINDERVANE_BINDER =
            REGISTER.register("cindervane_binder",
                    () -> new CindervaneBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0) // Unbreakable
                    )
            );

    public static final RegistryObject<Item> NULLJAW_BINDER =
            REGISTER.register("nulljaw_binder",
                    () -> new NulljawBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0) // Unbreakable
                    )
            );
}
