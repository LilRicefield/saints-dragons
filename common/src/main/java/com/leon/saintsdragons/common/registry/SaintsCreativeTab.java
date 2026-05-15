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
                                output.accept(ModItems.VARASUCHUS_BINDER.get());
                                output.accept(ModItems.STEGONAUT_BINDER.get());
                                output.accept(ModItems.VOLITANS_BINDER.get());
                                output.accept(ModItems.DRACONIC_CODEX.get());
                                output.accept(ModItems.DRAGON_BRUSH.get());
                                output.accept(ModItems.GOLDEN_DRAGON_BRUSH.get());
                                output.accept(ModItems.RAW_MOOP.get());
                                output.accept(ModItems.COOKED_MOOP.get());
                                output.accept(ModItems.RAEVYX_SCALE.get());
                                output.accept(ModItems.CINDERVANE_SCALE.get());
                                output.accept(ModItems.IGNIVORUS_SCALE.get());
                                output.accept(ModItems.VARASUCHUS_SCALE.get());
                                output.accept(ModItems.STEGONAUT_SCALE.get());
                                output.accept(ModItems.VOLITANS_SCALE.get());
                                output.accept(ModItems.IGNIVORUS_HEART.get());
                                output.accept(ModItems.IGNIVORUS_TOOTH.get());
                                output.accept(ModItems.VOLITANS_SPINE.get());
                                output.accept(ModItems.POTION_OF_TIDEGUARD.get());
                                output.accept(ModItems.POTION_OF_SEARING.get());
                                output.accept(ModItems.BLEEDING_BOLT_MUSIC_DISC.get());
                                output.accept(ModItems.HEARTY_DRAGON_MEAL.get());
                                output.accept(ModItems.RAEVYX_EGG.get());
                                output.accept(ModItems.IGNIVORUS_EGG.get());
                                output.accept(ModItems.CINDERVANE_EGG.get());
                                output.accept(ModItems.VARASUCHUS_EGG.get());
                                output.accept(ModItems.STEGONAUT_EGG.get());
                                output.accept(ModItems.VOLITANS_EGG.get());
                                output.accept(ModItems.CINDERVANE_SPAWN_EGG.get());
                                output.accept(ModItems.IGNIVORUS_SPAWN_EGG.get());
                                output.accept(ModItems.RAEVYX_SPAWN_EGG.get());
                                output.accept(ModItems.VARASUCHUS_SPAWN_EGG.get());
                                output.accept(ModItems.STEGONAUT_SPAWN_EGG.get());
                                output.accept(ModItems.VOLITANS_SPAWN_EGG.get());
                                output.accept(ModItems.NULLJAW_SPAWN_EGG.get());
                                output.accept(ModItems.MOOP_SPAWN_EGG.get());
                                output.accept(ModItems.IVY_THE_MERCHANT_SPAWN_EGG.get());

                                })
                            .build());

    public static void register() {
        REGISTER.register();
    }
}
