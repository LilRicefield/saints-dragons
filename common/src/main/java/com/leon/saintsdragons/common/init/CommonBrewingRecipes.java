package com.leon.saintsdragons.common.init;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModPotions;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;

public final class CommonBrewingRecipes {
    private CommonBrewingRecipes() {
    }

    public static void register() {
        PotionBrewing.addMix(Potions.AWKWARD, ModItems.VARASUCHUS_SCALE.get(), ModPotions.TIDEGUARD.get());
        PotionBrewing.addMix(Potions.AWKWARD, ModItems.IGNIVORUS_TOOTH.get(), ModPotions.SEARING.get());
    }
}
