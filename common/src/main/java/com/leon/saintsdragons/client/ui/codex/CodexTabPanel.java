package com.leon.saintsdragons.client.ui.codex;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class CodexTabPanel {
    public void drawTabs(GuiGraphics guiGraphics, int leftPos, int topPos, CodexTab activeTab,
                         ResourceLocation physiology, ResourceLocation physiologyClosed,
                         ResourceLocation ecology, ResourceLocation ecologyClosed,
                         ResourceLocation ally, ResourceLocation allyClosed) {
        drawTab(guiGraphics, leftPos, topPos, CodexTab.PHYSIOLOGY, activeTab, physiology, physiologyClosed, 0);
        drawTab(guiGraphics, leftPos, topPos, CodexTab.ECOLOGY, activeTab, ecology, ecologyClosed, 1);
        drawTab(guiGraphics, leftPos, topPos, CodexTab.ALLY, activeTab, ally, allyClosed, 2);
    }

    public CodexTab handleClick(double mouseX, double mouseY, int leftPos, int topPos) {
        if (mouseY < CodexLayout.getTabY(topPos, 0)
                || mouseY > CodexLayout.getTabY(topPos, 2) + CodexLayout.TAB_HEIGHT) {
            return null;
        }
        if (isWithinTab(mouseX, mouseY, leftPos, topPos, 0)) {
            return CodexTab.PHYSIOLOGY;
        }
        if (isWithinTab(mouseX, mouseY, leftPos, topPos, 1)) {
            return CodexTab.ECOLOGY;
        }
        if (isWithinTab(mouseX, mouseY, leftPos, topPos, 2)) {
            return CodexTab.ALLY;
        }
        return null;
    }

    private void drawTab(GuiGraphics guiGraphics, int leftPos, int topPos, CodexTab tab, CodexTab activeTab,
                         ResourceLocation activeTexture, ResourceLocation inactiveTexture, int index) {
        boolean isActive = tab == activeTab;
        int x = CodexLayout.getActiveTabX(leftPos);
        int y = CodexLayout.getTabY(topPos, index);
        if (isActive) {
            guiGraphics.blit(activeTexture, x, y, 0, 0,
                    CodexLayout.TAB_WIDTH, CodexLayout.TAB_HEIGHT,
                    CodexLayout.TAB_WIDTH, CodexLayout.TAB_HEIGHT);
        } else {
            guiGraphics.blit(inactiveTexture, x, y, 0, 0,
                    CodexLayout.TAB_CLOSED_WIDTH, CodexLayout.TAB_CLOSED_HEIGHT,
                    CodexLayout.TAB_CLOSED_WIDTH, CodexLayout.TAB_CLOSED_HEIGHT);
        }
    }

    private boolean isWithinTab(double mouseX, double mouseY, int leftPos, int topPos, int index) {
        int y = CodexLayout.getTabY(topPos, index);
        int x = CodexLayout.getActiveTabX(leftPos);
        return mouseX >= x && mouseX <= x + CodexLayout.TAB_WIDTH
                && mouseY >= y && mouseY <= y + CodexLayout.TAB_HEIGHT;
    }
}