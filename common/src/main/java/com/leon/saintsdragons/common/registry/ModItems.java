package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.item.CindervaneBinderItem;
import com.leon.saintsdragons.common.item.DragonAllyBookItem;
import com.leon.saintsdragons.common.item.NulljawBinderItem;
import com.leon.saintsdragons.common.item.RaevyxBinderItem;
import com.leon.saintsdragons.common.item.StegonautBinderItem;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public final class ModItems {
    private static final RegistryHelper.RegistryWrapper<Item> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.ITEM, () -> BuiltInRegistries.ITEM, SaintsDragonsCommon.MOD_ID);

    public static final Supplier<Item> RAEVYX_SPAWN_EGG =
            REGISTER.register("raevyx_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.RAEVYX,
                            0x000000, 0x8B0000,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> STEGONAUT_SPAWN_EGG =
            REGISTER.register("stegonaut_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.STEGONAUT,
                            0x8B4513, 0xCD853F,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> CINDERVANE_SPAWN_EGG =
            REGISTER.register("cindervane_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.CINDERVANE,
                            0x5E5E5E, 0xA7490D,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> NULLJAW_SPAWN_EGG =
            REGISTER.register("nulljaw_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.NULLJAW,
                            0x2C3E50, 0x16A085,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> IGNIVORUS_SPAWN_EGG =
            REGISTER.register("ignivorus_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.IGNIVORUS,
                            0x0A0A0A, 0x5A5A5A,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> DRAGON_ALLY_BOOK =
            REGISTER.register("dragon_ally_book",
                    () -> new DragonAllyBookItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));

    public static final Supplier<Item> STEGONAUT_BINDER =
            REGISTER.register("stegonaut_binder",
                    () -> new StegonautBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));

    public static final Supplier<Item> RAEVYX_BINDER =
            REGISTER.register("raevyx_binder",
                    () -> new RaevyxBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));

    public static final Supplier<Item> CINDERVANE_BINDER =
            REGISTER.register("cindervane_binder",
                    () -> new CindervaneBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));

    public static final Supplier<Item> NULLJAW_BINDER =
            REGISTER.register("nulljaw_binder",
                    () -> new NulljawBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));

    private ModItems() {
    }

    public static void register() {
        REGISTER.register();
    }
}
