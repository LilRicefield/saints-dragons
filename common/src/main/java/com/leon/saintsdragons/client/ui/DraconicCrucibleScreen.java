package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.menu.DraconicCrucibleMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class DraconicCrucibleScreen extends AbstractContainerScreen<DraconicCrucibleMenu> {
    private static final int BUTTON_X = 145;
    private static final int BUTTON_Y = 99;
    private static final int BUTTON_WIDTH = 24;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_U = 232;
    private static final int BUTTON_DEFAULT_V = 0;
    private static final int BUTTON_HIGHLIGHTED_V = 14;
    private static final int BUTTON_CLICKED_V = 28;
    private static final int PANEL_X = 58;
    private static final int PANEL_Y = 56;
    private static final int PANEL_U = 176;
    private static final int PANEL_V = 87;
    private static final int PANEL_WIDTH = 70;
    private static final int PANEL_HEIGHT = 60;
    private static final int BAR_U = 245;
    private static final int BAR_V = 92;
    private static final int BAR_WIDTH = 5;
    private static final int BAR_HEIGHT = 40;
    private static final int BAR_LEFT_SHIFT = 9;
    private static final int BAR_X = BAR_U - PANEL_U - BAR_LEFT_SHIFT;
    private static final int BAR_Y = BAR_V - PANEL_V;
    private static final int ARROW_X = 80;
    private static final int ARROW_Y = 46;
    private static final int ARROW_U = 180;
    private static final int ARROW_V = 3;
    private static final int ARROW_WIDTH = 17;
    private static final int ARROW_HEIGHT = 9;
    private static final int GAUGE_X = 35;
    private static final int GAUGE_Y = 28;
    private static final int GAUGE_U = 182;
    private static final int GAUGE_V = 25;
    private static final int GAUGE_WIDTH = 7;
    private static final int GAUGE_HEIGHT = 56;
    private static final int GAUGE_LEVEL_1_HEIGHT = 21;
    private static final int GAUGE_LEVEL_2_HEIGHT = 42;
    private static final int GAUGE_LEVEL_3_HEIGHT = 56;

    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/gui/draconic_crucible_gui.png");

    private CrucibleButton crucibleButton;

    public DraconicCrucibleScreen(DraconicCrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 208;
    }

    @Override
    protected void init() {
        super.init();
        this.crucibleButton = this.addRenderableWidget(new CrucibleButton(
                this.leftPos + BUTTON_X,
                this.topPos + BUTTON_Y,
                this::pressCrucibleButton));
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight, 256, 256);
        int arrowFillHeight = getArrowFillHeight();
        if (arrowFillHeight > 0) {
            int arrowOffset = ARROW_HEIGHT - arrowFillHeight;
            guiGraphics.blit(TEXTURE,
                    this.leftPos + ARROW_X,
                    this.topPos + ARROW_Y + arrowOffset,
                    ARROW_U,
                    ARROW_V + arrowOffset,
                    ARROW_WIDTH,
                    arrowFillHeight,
                    256,
                    256);
        }
        int gaugeFillHeight = getGaugeFillHeight();
        if (gaugeFillHeight > 0) {
            int gaugeOffset = GAUGE_HEIGHT - gaugeFillHeight;
            guiGraphics.blit(TEXTURE,
                    this.leftPos + GAUGE_X,
                    this.topPos + GAUGE_Y + gaugeOffset,
                    GAUGE_U,
                    GAUGE_V + gaugeOffset,
                    GAUGE_WIDTH,
                    gaugeFillHeight,
                    256,
                    256);
        }
        if (this.menu.isProcessing()) {
            int panelX = this.leftPos + PANEL_X;
            int panelY = this.topPos + PANEL_Y;
            guiGraphics.blit(TEXTURE, panelX, panelY, PANEL_U, PANEL_V,
                    PANEL_WIDTH, PANEL_HEIGHT, 256, 256);
            int barFillHeight = getBarFillHeight();
            if (barFillHeight > 0) {
                int barOffset = BAR_HEIGHT - barFillHeight;
                guiGraphics.blit(TEXTURE,
                        panelX + BAR_X,
                        panelY + BAR_Y + barOffset,
                        BAR_U,
                        BAR_V + barOffset,
                        BAR_WIDTH,
                        barFillHeight,
                        256,
                        256);
            }
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    private int getGaugeFillHeight() {
        int charge = this.menu.getBurnTime();
        if (charge <= 0) {
            return 0;
        }

        int level1Charge = this.menu.getTierChargeCapacity(1);
        int level2Charge = this.menu.getTierChargeCapacity(2);
        int level3Charge = this.menu.getTierChargeCapacity(3);
        if (charge <= level1Charge) {
            return scaleGaugeSegment(charge, 0, level1Charge, 0, GAUGE_LEVEL_1_HEIGHT);
        }
        if (charge <= level2Charge) {
            return scaleGaugeSegment(charge, level1Charge, level2Charge,
                    GAUGE_LEVEL_1_HEIGHT, GAUGE_LEVEL_2_HEIGHT);
        }
        return scaleGaugeSegment(Math.min(charge, level3Charge), level2Charge, level3Charge,
                GAUGE_LEVEL_2_HEIGHT, GAUGE_LEVEL_3_HEIGHT);
    }

    private static int scaleGaugeSegment(int value, int valueStart, int valueEnd,
                                         int pixelStart, int pixelEnd) {
        int valueRange = Math.max(1, valueEnd - valueStart);
        int pixelRange = pixelEnd - pixelStart;
        int progress = Math.max(0, value - valueStart);
        return Math.min(pixelEnd, pixelStart + (pixelRange * progress + valueRange - 1) / valueRange);
    }

    private int getArrowFillHeight() {
        int total = this.menu.getProcessingTimeTotal();
        if (!this.menu.isProcessing() || total <= 0) {
            return 0;
        }
        return Math.min(ARROW_HEIGHT,
                (ARROW_HEIGHT * this.menu.getProcessingProgress() + total - 1) / total);
    }

    private int getBarFillHeight() {
        int total = this.menu.getProcessingTimeTotal();
        if (!this.menu.isProcessing() || total <= 0) {
            return 0;
        }
        return Math.min(BAR_HEIGHT,
                (BAR_HEIGHT * this.menu.getProcessingProgress() + total - 1) / total);
    }

    private void pressCrucibleButton() {
        if (this.menu.canStartProcessing() && this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.crucibleButton != null) {
            this.crucibleButton.active = this.menu.canStartProcessing();
        }
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static final class CrucibleButton extends AbstractWidget {
        private final Runnable onPress;
        private boolean pressed;

        private CrucibleButton(int x, int y, Runnable onPress) {
            super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                    Component.translatable("gui.saintsdragons.draconic_crucible.button"));
            this.onPress = onPress;
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int v = !this.active
                    ? BUTTON_DEFAULT_V
                    : (this.pressed
                    ? BUTTON_CLICKED_V
                    : (this.isHovered() ? BUTTON_HIGHLIGHTED_V : BUTTON_DEFAULT_V));
            if (!this.active) {
                RenderSystem.setShaderColor(0.45F, 0.45F, 0.45F, 1.0F);
            }
            guiGraphics.blit(TEXTURE, this.getX(), this.getY(), BUTTON_U, v,
                    BUTTON_WIDTH, BUTTON_HEIGHT, 256, 256);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.pressed = true;
            this.onPress.run();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            this.pressed = false;
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationOutput) {
            this.defaultButtonNarrationText(narrationOutput);
        }
    }
}
