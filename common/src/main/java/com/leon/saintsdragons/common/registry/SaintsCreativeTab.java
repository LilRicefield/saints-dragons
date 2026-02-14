package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;

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
                                output.accept(ModItems.DRACONIC_CODEX.get());
                                output.accept(ModItems.DRAGON_BRUSH.get());
                                output.accept(ModItems.GOLDEN_DRAGON_BRUSH.get());
                                output.accept(ModItems.RAEVYX_SCALE.get());
                                output.accept(ModItems.CINDERVANE_SCALE.get());
                                output.accept(ModItems.IGNIVORUS_SCALE.get());
                                output.accept(ModItems.IGNIVORUS_HEART.get());
                                output.accept(ModItems.IGNIVORUS_TOOTH.get());
                                output.accept(ModItems.NULLJAW_SCALE.get());
                                output.accept(ModItems.STEGONAUT_SCALE.get());
                                output.accept(PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotions.NULLJAW_TIDEGUARD.get()));
                                output.accept(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), ModPotions.NULLJAW_TIDEGUARD.get()));
                                output.accept(PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), ModPotions.NULLJAW_TIDEGUARD.get()));
                                output.accept(PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotions.SEARING.get()));
                                output.accept(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), ModPotions.SEARING.get()));
                                output.accept(PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), ModPotions.SEARING.get()));
                                output.accept(ModItems.BLEEDING_BOLT_MUSIC_DISC.get());
                                output.accept(ModItems.HEARTY_DRAGON_MEAL.get());
                                output.accept(ModItems.RAEVYX_EGG.get());
                                output.accept(ModItems.IGNIVORUS_EGG.get());
                                output.accept(ModItems.CINDERVANE_EGG.get());
                                output.accept(ModItems.NULLJAW_EGG.get());
                                output.accept(ModItems.STEGONAUT_EGG.get());
                                output.accept(ModItems.CINDERVANE_SPAWN_EGG.get());
                                output.accept(ModItems.IGNIVORUS_SPAWN_EGG.get());
                                output.accept(ModItems.RAEVYX_SPAWN_EGG.get());
                                output.accept(ModItems.NULLJAW_SPAWN_EGG.get());
                                output.accept(ModItems.STEGONAUT_SPAWN_EGG.get());
                                output.accept(ModItems.IVY_THE_MERCHANT_SPAWN_EGG.get());

                                })
                            .build());

    public static void register() {
        REGISTER.register();
    }
}
