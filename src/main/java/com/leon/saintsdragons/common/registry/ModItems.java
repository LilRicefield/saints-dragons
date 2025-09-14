package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.SaintsDragons;
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
}
