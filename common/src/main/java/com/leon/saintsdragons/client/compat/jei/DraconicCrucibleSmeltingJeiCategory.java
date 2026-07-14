package com.leon.saintsdragons.client.compat.jei;

import com.leon.saintsdragons.common.recipe.DraconicCrucibleSmeltingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;

import static com.leon.saintsdragons.common.block.crucible.DraconicCrucibleUiLayout.*;

final class DraconicCrucibleSmeltingJeiCategory
        extends DraconicCrucibleJeiCategory<DraconicCrucibleSmeltingRecipe> {

    DraconicCrucibleSmeltingJeiCategory(IGuiHelper guiHelper,
                                        RecipeType<DraconicCrucibleSmeltingRecipe> recipeType) {
        super(guiHelper, recipeType, "jei.saintsdragons.draconic_crucible.smelting");
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DraconicCrucibleSmeltingRecipe recipe,
                          IFocusGroup focuses) {
        builder.addInputSlot(INPUT_GRID_X + SLOT_SPACING, INPUT_GRID_Y + SLOT_SPACING)
                .addIngredients(recipe.ingredient());
        builder.addOutputSlot(OUTPUT_SLOT_X, OUTPUT_SLOT_Y).addItemStack(recipe.result());
        addFuelSlot(builder, recipe.requiredHeatLevel());
    }

    @Override
    protected int requiredHeatLevel(DraconicCrucibleSmeltingRecipe recipe) {
        return recipe.requiredHeatLevel();
    }

    @Override
    protected int processingTime(DraconicCrucibleSmeltingRecipe recipe) {
        return recipe.processingTime();
    }
}
