package com.leon.saintsdragons.client.compat.jei;

import com.leon.saintsdragons.common.recipe.DraconicCrucibleShapedRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.crafting.Ingredient;

import static com.leon.saintsdragons.common.block.crucible.DraconicCrucibleUiLayout.*;

final class DraconicCrucibleCraftingJeiCategory
        extends DraconicCrucibleJeiCategory<DraconicCrucibleShapedRecipe> {

    DraconicCrucibleCraftingJeiCategory(IGuiHelper guiHelper,
                                        RecipeType<DraconicCrucibleShapedRecipe> recipeType) {
        super(guiHelper, recipeType, "jei.saintsdragons.draconic_crucible.crafting");
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DraconicCrucibleShapedRecipe recipe,
                          IFocusGroup focuses) {
        int offsetX = (3 - recipe.width()) / 2;
        int offsetY = (3 - recipe.height()) / 2;
        for (int row = 0; row < recipe.height(); row++) {
            for (int column = 0; column < recipe.width(); column++) {
                Ingredient ingredient = recipe.getIngredients().get(column + row * recipe.width());
                if (!ingredient.isEmpty()) {
                    builder.addInputSlot(
                                    INPUT_GRID_X + (offsetX + column) * SLOT_SPACING,
                                    INPUT_GRID_Y + (offsetY + row) * SLOT_SPACING)
                            .addIngredients(ingredient);
                }
            }
        }
        builder.addOutputSlot(OUTPUT_SLOT_X, OUTPUT_SLOT_Y).addItemStack(recipe.result());
        addFuelSlot(builder, recipe.requiredHeatLevel());
    }

    @Override
    protected int requiredHeatLevel(DraconicCrucibleShapedRecipe recipe) {
        return recipe.requiredHeatLevel();
    }

    @Override
    protected int processingTime(DraconicCrucibleShapedRecipe recipe) {
        return recipe.processingTime();
    }
}
