package com.leon.saintsdragons.client.compat.jei;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.block.crucible.DraconicCrucibleFuelTier;
import com.leon.saintsdragons.common.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Locale;

import static com.leon.saintsdragons.common.block.crucible.DraconicCrucibleUiLayout.*;

abstract class DraconicCrucibleJeiCategory<T> implements IRecipeCategory<T> {
    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/gui/draconic_crucible_jei.png");
    private final RecipeType<T> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable button;

    protected DraconicCrucibleJeiCategory(IGuiHelper guiHelper, RecipeType<T> recipeType,
                                          String titleKey) {
        this.recipeType = recipeType;
        this.title = Component.translatable(titleKey);
        this.icon = guiHelper.createDrawableItemLike(ModItems.DRACONIC_CRUCIBLE.get());
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, WIDTH, RECIPE_VIEW_HEIGHT);
        this.button = guiHelper.createDrawable(
                TEXTURE, BUTTON_U, BUTTON_DEFAULT_V, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    @Override
    public final RecipeType<T> getRecipeType() {
        return this.recipeType;
    }

    @Override
    public final Component getTitle() {
        return this.title;
    }

    @Override
    public final int getWidth() {
        return WIDTH;
    }

    @Override
    public final int getHeight() {
        return RECIPE_VIEW_HEIGHT;
    }

    @Override
    public final IDrawable getIcon() {
        return this.icon;
    }

    protected final void addFuelSlot(IRecipeLayoutBuilder builder, int requiredHeatLevel) {
        var fuelSlot = builder.addSlot(RecipeIngredientRole.CATALYST, FUEL_SLOT_X, FUEL_SLOT_Y)
                .setSlotName("fuel");
        for (DraconicCrucibleFuelTier tier : DraconicCrucibleFuelTier.values()) {
            if (tier.canProcess(requiredHeatLevel) && tier.tag() != null) {
                fuelSlot.addIngredients(Ingredient.of(tier.tag()));
            }
        }
        fuelSlot.addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(
                Component.translatable("gui.saintsdragons.draconic_crucible.valid_fuel",
                        requiredHeatLevel)));
    }

    @Override
    public final void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                           double mouseX, double mouseY) {
        this.background.draw(guiGraphics);

        int heatLevel = Math.max(1, Math.min(3, requiredHeatLevel(recipe)));
        double progress = getLoopProgress(processingTime(recipe));
        GaugeAnimation gauge = gaugeAnimationForHeatLevel(heatLevel);
        int gaugeHeight = gauge.finalHeight() + (int) Math.round(
                (gauge.initialHeight() - gauge.finalHeight()) * (1.0D - progress));
        drawGauge(guiGraphics, gaugeHeight);
        drawBottomFill(guiGraphics,
                ARROW_X, ARROW_Y, ARROW_U, ARROW_V,
                ARROW_WIDTH, ARROW_HEIGHT, progress);
        drawBottomFill(guiGraphics,
                PANEL_X + BAR_X, PANEL_Y + BAR_Y, BAR_U, BAR_V,
                BAR_WIDTH, BAR_HEIGHT, progress);
        this.button.draw(guiGraphics, BUTTON_X, BUTTON_Y);
    }

    @Override
    public final void getTooltip(ITooltipBuilder tooltip, T recipe, IRecipeSlotsView recipeSlotsView,
                                 double mouseX, double mouseY) {
        if (isWithin(mouseX, mouseY, GAUGE_X, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT)) {
            tooltip.add(Component.translatable("gui.saintsdragons.draconic_crucible.heat_level",
                    requiredHeatLevel(recipe)));
        }
        if (isWithin(mouseX, mouseY, ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT)) {
            tooltip.add(Component.translatable("gui.saintsdragons.draconic_crucible.processing_time",
                    formatSeconds(processingTime(recipe))));
        }
    }

    protected abstract int requiredHeatLevel(T recipe);

    protected abstract int processingTime(T recipe);

    private static double getLoopProgress(int processingTicks) {
        long durationMillis = Math.max(50L, processingTicks * 50L);
        return (System.currentTimeMillis() % durationMillis) / (double) durationMillis;
    }

    private static void drawBottomFill(GuiGraphics guiGraphics,
                                       int x, int y, int u, int v,
                                       int width, int height, double progress) {
        int fillHeight = Math.min(height, (int) Math.ceil(height * progress));
        if (fillHeight <= 0) {
            return;
        }
        int offset = height - fillHeight;
        guiGraphics.blit(TEXTURE,
                x, y + offset, u, v + offset,
                width, fillHeight, 256, 256);
    }

    private static void drawGauge(GuiGraphics guiGraphics, int fillHeight) {
        if (fillHeight <= 0) {
            return;
        }
        int clampedHeight = Math.min(GAUGE_HEIGHT, fillHeight);
        int offset = GAUGE_HEIGHT - clampedHeight;
        guiGraphics.blit(TEXTURE,
                GAUGE_X, GAUGE_Y + offset, GAUGE_U, GAUGE_V + offset,
                GAUGE_WIDTH, clampedHeight, 256, 256);
    }

    private static boolean isWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String formatSeconds(int ticks) {
        if (ticks % 20 == 0) {
            return Integer.toString(ticks / 20);
        }
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0D);
    }
}
