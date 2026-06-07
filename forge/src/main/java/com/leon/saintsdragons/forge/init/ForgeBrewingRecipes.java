package com.leon.saintsdragons.forge.init;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModPotionItems;
import com.leon.saintsdragons.common.registry.ModPotions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.crafting.StrictNBTIngredient;

public final class ForgeBrewingRecipes {
    private ForgeBrewingRecipes() {
    }

    public static void register() {
        ItemStack awkwardPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD);
        ItemStack tideguardPotion = PotionUtils.setPotion(
                new ItemStack(ModPotionItems.POTION_OF_TIDEGUARD.get()),
                ModPotions.TIDEGUARD.get()
        );
        ItemStack searingPotion = PotionUtils.setPotion(
                new ItemStack(ModPotionItems.POTION_OF_SEARING.get()),
                ModPotions.SEARING.get()
        );

        BrewingRecipeRegistry.addRecipe(
                StrictNBTIngredient.of(awkwardPotion),
                Ingredient.of(ModItems.VARASUCHUS_SCALE.get()),
                tideguardPotion
        );
        BrewingRecipeRegistry.addRecipe(
                StrictNBTIngredient.of(awkwardPotion),
                Ingredient.of(ModItems.IGNIVORUS_TOOTH.get()),
                searingPotion
        );
    }
}
