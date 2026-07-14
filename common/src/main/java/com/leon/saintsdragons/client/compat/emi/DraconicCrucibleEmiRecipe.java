package com.leon.saintsdragons.client.compat.emi;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.block.crucible.DraconicCrucibleFuelTier;
import com.leon.saintsdragons.common.recipe.DraconicCrucibleShapedRecipe;
import com.leon.saintsdragons.common.recipe.DraconicCrucibleSmeltingRecipe;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.leon.saintsdragons.common.block.crucible.DraconicCrucibleUiLayout.*;

final class DraconicCrucibleEmiRecipe extends BasicEmiRecipe {
    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/gui/draconic_crucible_jei.png");

    private final List<PositionedIngredient> positionedInputs;
    private final EmiIngredient fuel;
    private final EmiStack output;
    private final int requiredHeatLevel;
    private final int processingTime;

    private DraconicCrucibleEmiRecipe(EmiRecipeCategory category, ResourceLocation id,
                                      List<PositionedIngredient> positionedInputs,
                                      EmiStack output, int requiredHeatLevel, int processingTime) {
        super(category, id, WIDTH, RECIPE_VIEW_HEIGHT);
        this.positionedInputs = positionedInputs;
        this.output = output;
        this.requiredHeatLevel = requiredHeatLevel;
        this.processingTime = processingTime;
        this.fuel = createFuelIngredient(requiredHeatLevel);
        this.inputs.addAll(positionedInputs.stream().map(PositionedIngredient::ingredient).toList());
        this.catalysts.add(this.fuel);
        this.outputs.add(output);
    }

    static DraconicCrucibleEmiRecipe crafting(DraconicCrucibleShapedRecipe recipe) {
        List<PositionedIngredient> inputs = new ArrayList<>();
        int offsetX = (3 - recipe.width()) / 2;
        int offsetY = (3 - recipe.height()) / 2;
        for (int row = 0; row < recipe.height(); row++) {
            for (int column = 0; column < recipe.width(); column++) {
                Ingredient ingredient = recipe.getIngredients().get(column + row * recipe.width());
                if (!ingredient.isEmpty()) {
                    inputs.add(new PositionedIngredient(
                            EmiIngredient.of(ingredient),
                            INPUT_GRID_X + (offsetX + column) * SLOT_SPACING,
                            INPUT_GRID_Y + (offsetY + row) * SLOT_SPACING));
                }
            }
        }
        return new DraconicCrucibleEmiRecipe(
                SaintsDragonsEmiPlugin.CRUCIBLE_CRAFTING,
                recipe.getId(), inputs, EmiStack.of(recipe.result()),
                recipe.requiredHeatLevel(), recipe.processingTime());
    }

    static DraconicCrucibleEmiRecipe smelting(DraconicCrucibleSmeltingRecipe recipe) {
        List<PositionedIngredient> inputs = List.of(new PositionedIngredient(
                EmiIngredient.of(recipe.ingredient()),
                INPUT_GRID_X + SLOT_SPACING,
                INPUT_GRID_Y + SLOT_SPACING));
        return new DraconicCrucibleEmiRecipe(
                SaintsDragonsEmiPlugin.CRUCIBLE_SMELTING,
                recipe.id(), inputs, EmiStack.of(recipe.result()),
                recipe.requiredHeatLevel(), recipe.processingTime());
    }

    private static EmiIngredient createFuelIngredient(int requiredHeatLevel) {
        List<EmiIngredient> fuels = new ArrayList<>();
        for (DraconicCrucibleFuelTier tier : DraconicCrucibleFuelTier.values()) {
            TagKey<Item> tag = tier.tag();
            if (tier.canProcess(requiredHeatLevel) && tag != null) {
                fuels.add(EmiIngredient.of(tag));
            }
        }
        return EmiIngredient.of(fuels);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(TEXTURE, 0, 0, WIDTH, RECIPE_VIEW_HEIGHT, 0, 0);

        for (PositionedIngredient input : this.positionedInputs) {
            widgets.addSlot(input.ingredient(), input.x() - 1, input.y() - 1)
                    .drawBack(false);
        }
        widgets.addSlot(this.fuel, FUEL_SLOT_X - 1, FUEL_SLOT_Y - 1)
                .drawBack(false)
                .catalyst(true)
                .appendTooltip(Component.translatable(
                        "gui.saintsdragons.draconic_crucible.valid_fuel", this.requiredHeatLevel));
        widgets.addSlot(this.output, OUTPUT_SLOT_X - 5, OUTPUT_SLOT_Y - 5)
                .drawBack(false)
                .large(true)
                .recipeContext(this);

        int animationMillis = getAnimationMillis();
        GaugeAnimation gauge = gaugeAnimationForHeatLevel(this.requiredHeatLevel);
        int finalGaugeOffset = GAUGE_HEIGHT - gauge.finalHeight();
        if (gauge.finalHeight() > 0) {
            widgets.addTexture(TEXTURE,
                    GAUGE_X, GAUGE_Y + finalGaugeOffset, GAUGE_WIDTH, gauge.finalHeight(),
                    GAUGE_U, GAUGE_V + finalGaugeOffset);
        }
        int consumedGaugeHeight = gauge.initialHeight() - gauge.finalHeight();
        if (consumedGaugeHeight > 0) {
            int initialGaugeOffset = GAUGE_HEIGHT - gauge.initialHeight();
            widgets.addAnimatedTexture(TEXTURE,
                    GAUGE_X, GAUGE_Y + initialGaugeOffset, GAUGE_WIDTH, consumedGaugeHeight,
                    GAUGE_U, GAUGE_V + initialGaugeOffset, animationMillis,
                    false, true, true);
        }
        widgets.addAnimatedTexture(TEXTURE,
                ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT, ARROW_U, ARROW_V,
                animationMillis, false, true, false);
        widgets.addAnimatedTexture(TEXTURE,
                PANEL_X + BAR_X, PANEL_Y + BAR_Y, BAR_WIDTH, BAR_HEIGHT, BAR_U, BAR_V,
                animationMillis, false, true, false);
        widgets.addTexture(TEXTURE,
                BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_U, BUTTON_DEFAULT_V);

        widgets.addTooltipText(List.of(Component.translatable(
                        "gui.saintsdragons.draconic_crucible.heat_level", this.requiredHeatLevel)),
                GAUGE_X, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT);
        widgets.addTooltipText(List.of(Component.translatable(
                        "gui.saintsdragons.draconic_crucible.processing_time",
                        formatSeconds(this.processingTime))),
                ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT);
    }

    private int getAnimationMillis() {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(50L, this.processingTime * 50L));
    }

    private static String formatSeconds(int ticks) {
        if (ticks % 20 == 0) {
            return Integer.toString(ticks / 20);
        }
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0D);
    }

    private record PositionedIngredient(EmiIngredient ingredient, int x, int y) {
    }
}
