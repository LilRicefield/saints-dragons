package com.leon.saintsdragons.client.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class SaintsDragonsBrewingEmiRecipe implements EmiRecipe {
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("minecraft", "textures/gui/container/brewing_stand.png");
    private static final EmiStack BLAZE_POWDER = EmiStack.of(Items.BLAZE_POWDER);

    private final EmiStack input;
    private final EmiIngredient ingredient;
    private final EmiStack output;
    private final EmiStack inputBatch;
    private final EmiStack outputBatch;
    private final ResourceLocation id;

    SaintsDragonsBrewingEmiRecipe(EmiStack input, EmiIngredient ingredient,
                                  EmiStack output, ResourceLocation id) {
        this.input = input;
        this.ingredient = ingredient;
        this.output = output;
        this.inputBatch = input.copy().setAmount(3);
        this.outputBatch = output.copy().setAmount(3);
        this.id = id;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return VanillaEmiRecipeCategories.BREWING;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return this.id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of(this.inputBatch, this.ingredient);
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(this.outputBatch);
    }

    @Override
    public int getDisplayWidth() {
        return 120;
    }

    @Override
    public int getDisplayHeight() {
        return 61;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(BACKGROUND, 0, 0, 103, 61, 16, 14);
        widgets.addAnimatedTexture(
                BACKGROUND, 81, 2, 9, 28, 176, 0,
                20_000, false, false, false);
        widgets.addAnimatedTexture(
                BACKGROUND, 47, 0, 12, 29, 185, 0,
                700, false, true, false);
        widgets.addTexture(BACKGROUND, 44, 30, 18, 4, 176, 29);
        widgets.addSlot(BLAZE_POWDER, 0, 2).drawBack(false);
        widgets.addSlot(this.input, 39, 36).drawBack(false);
        widgets.addSlot(this.ingredient, 62, 2).drawBack(false);
        widgets.addSlot(this.output, 85, 36)
                .drawBack(false)
                .recipeContext(this);
    }
}
