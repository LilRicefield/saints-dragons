package com.leon.saintsdragons.client.compat.emi;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.recipe.DraconicCrucibleShapedRecipe;
import com.leon.saintsdragons.common.recipe.DraconicCrucibleSmeltingRecipe;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModPotionItems;
import com.leon.saintsdragons.common.registry.ModPotions;
import com.leon.saintsdragons.common.registry.ModRecipes;
import com.leon.saintsdragons.platform.Services;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;

@EmiEntrypoint
public final class SaintsDragonsEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory CRUCIBLE_CRAFTING = new EmiRecipeCategory(
            SaintsDragonsCommon.rl("draconic_crucible"),
            EmiStack.of(ModItems.DRACONIC_CRUCIBLE.get()));
    public static final EmiRecipeCategory CRUCIBLE_SMELTING = new EmiRecipeCategory(
            SaintsDragonsCommon.rl("draconic_crucible_smelting"),
            EmiStack.of(ModItems.DRACONIC_CRUCIBLE.get()));

    @Override
    public void register(EmiRegistry registry) {
        if ("fabric".equals(Services.PLATFORM.getPlatformId())) {
            registerPotionRecipes(registry);
        }

        EmiStack crucible = EmiStack.of(ModItems.DRACONIC_CRUCIBLE.get());
        registry.addCategory(CRUCIBLE_CRAFTING);
        registry.addCategory(CRUCIBLE_SMELTING);
        registry.addWorkstation(CRUCIBLE_CRAFTING, crucible);
        registry.addWorkstation(CRUCIBLE_SMELTING, crucible);
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, crucible);

        for (DraconicCrucibleShapedRecipe recipe : registry.getRecipeManager().getAllRecipesFor(
                ModRecipes.DRACONIC_CRUCIBLE_SHAPED_TYPE.get())) {
            registry.addRecipe(DraconicCrucibleEmiRecipe.crafting(recipe));
        }
        for (DraconicCrucibleSmeltingRecipe recipe : registry.getRecipeManager().getAllRecipesFor(
                ModRecipes.DRACONIC_CRUCIBLE_SMELTING_TYPE.get())) {
            registry.addRecipe(DraconicCrucibleEmiRecipe.smelting(recipe));
        }
    }

    private static void registerPotionRecipes(EmiRegistry registry) {
        ItemStack awkwardPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD);

        ItemStack tideguardPotion = new ItemStack(ModPotionItems.POTION_OF_TIDEGUARD.get());
        PotionUtils.setPotion(tideguardPotion, ModPotions.TIDEGUARD.get());
        registry.addRecipe(new SaintsDragonsBrewingEmiRecipe(
                EmiStack.of(awkwardPotion),
                EmiIngredient.of(Ingredient.of(ModItems.VARASUCHUS_SCALE.get())),
                EmiStack.of(tideguardPotion),
                SaintsDragonsCommon.rl("emi/brewing/tideguard")
        ));

        ItemStack searingPotion = new ItemStack(ModPotionItems.POTION_OF_SEARING.get());
        PotionUtils.setPotion(searingPotion, ModPotions.SEARING.get());
        registry.addRecipe(new SaintsDragonsBrewingEmiRecipe(
                EmiStack.of(awkwardPotion),
                EmiIngredient.of(Ingredient.of(ModItems.IGNIVORUS_TOOTH.get())),
                EmiStack.of(searingPotion),
                SaintsDragonsCommon.rl("emi/brewing/searing")
        ));
    }
}
