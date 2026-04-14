package com.leon.saintsdragons.fabric.compat.emi;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModPotions;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.recipe.EmiBrewingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;

public final class SaintsDragonsEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        ItemStack awkwardPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD);

        ItemStack tideguardPotion = new ItemStack(ModItems.POTION_OF_TIDEGUARD.get());
        PotionUtils.setPotion(tideguardPotion, ModPotions.VARASUCHUS_TIDEGUARD.get());
        registry.addRecipe(new EmiBrewingRecipe(
                EmiStack.of(awkwardPotion),
                EmiIngredient.of(Ingredient.of(ModItems.VARASUCHUS_SCALE.get())),
                EmiStack.of(tideguardPotion),
                SaintsDragonsCommon.rl("emi/brewing/tideguard")
        ));

        ItemStack searingPotion = new ItemStack(ModItems.POTION_OF_SEARING.get());
        PotionUtils.setPotion(searingPotion, ModPotions.SEARING.get());
        registry.addRecipe(new EmiBrewingRecipe(
                EmiStack.of(awkwardPotion),
                EmiIngredient.of(Ingredient.of(ModItems.IGNIVORUS_TOOTH.get())),
                EmiStack.of(searingPotion),
                SaintsDragonsCommon.rl("emi/brewing/searing")
        ));
    }
}
