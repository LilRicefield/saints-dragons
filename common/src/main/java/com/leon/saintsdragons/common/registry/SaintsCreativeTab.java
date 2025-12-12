package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class SaintsCreativeTab {
    public static final RegistryHelper.RegistryWrapper<CreativeModeTab> REGISTER = Services.PLATFORM.getRegistryHelper()
            .create((Registries.CREATIVE_MODE_TAB), () -> BuiltInRegistries.CREATIVE_MODE_TAB, SaintsDragonsCommon.MOD_ID);


    public static final Supplier<CreativeModeTab> SD_CREATIVE_TAB =
            REGISTER.register("saintsdragons_tab",
                    () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                            .title(Component.translatable("itemGroup.saintsdragons"))
                            .icon(() -> new ItemStack(ModItems.RAEVYX_BINDER.get()))
                            .displayItems((itemDisplayParameters, output) -> {
                                output.accept(ModItems.CINDERVANE_BINDER.get());
                                output.accept(ModItems.IGNIVORUS_BINDER.get());
                                output.accept(ModItems.RAEVYX_BINDER.get());
                                output.accept(ModItems.NULLJAW_BINDER.get());
                                output.accept(ModItems.STEGONAUT_BINDER.get());
                                output.accept(ModItems.HEARTY_DRAGON_MEAL.get());
                                output.accept(ModItems.CINDERVANE_SPAWN_EGG.get());
                                output.accept(ModItems.IGNIVORUS_SPAWN_EGG.get());
                                output.accept(ModItems.RAEVYX_SPAWN_EGG.get());
                                output.accept(ModItems.NULLJAW_SPAWN_EGG.get());
                                output.accept(ModItems.STEGONAUT_SPAWN_EGG.get());

                                })
                            .build());

    public static void register() {
        REGISTER.register();
    }
}