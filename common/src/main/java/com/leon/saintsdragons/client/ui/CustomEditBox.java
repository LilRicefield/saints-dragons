package com.leon.saintsdragons.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Environment(EnvType.CLIENT)
public class CustomEditBox extends EditBox {
    private int textColor = 0xFFFFFF;
    private static Field displayPosField;
    private static Field highlightPosField;
    private static Field fontField;
    private static Method renderHighlightMethod;
    private static Method getMaxLengthMethod;

    static {
        try {
            // Access private fields using reflection
            displayPosField = EditBox.class.getDeclaredField("displayPos");
            displayPosField.setAccessible(true);

            highlightPosField = EditBox.class.getDeclaredField("highlightPos");
            highlightPosField.setAccessible(true);

            fontField = EditBox.class.getDeclaredField("font");
            fontField.setAccessible(true);

            renderHighlightMethod = EditBox.class.getDeclaredMethod("renderHighlight", GuiGraphics.class, int.class, int.class, int.class, int.class);
            renderHighlightMethod.setAccessible(true);

            getMaxLengthMethod = EditBox.class.getDeclaredMethod("getMaxLength");
            getMaxLengthMethod.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CustomEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Override
    public void setTextColor(int color) {
        this.textColor = color;
        super.setTextColor(color);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.isVisible()) {
            return;
        }

        try {
            int displayPos = (int) displayPosField.get(this);
            int cursorPos = this.getCursorPosition();
            int highlightPos = (int) highlightPosField.get(this);
            Font font = (Font) fontField.get(this);
            String value = this.getValue();
            String displayedText = font.plainSubstrByWidth(value.substring(displayPos), this.getWidth());
            int cursorPosInDisplayed = cursorPos - displayPos;
            int highlightPosInDisplayed = Mth.clamp(highlightPos - displayPos, 0, displayedText.length());
            boolean cursorInView = cursorPosInDisplayed >= 0 && cursorPosInDisplayed <= displayedText.length();
            boolean shouldDrawCursor = this.isFocused() && cursorInView && (System.currentTimeMillis() / 300L % 2L == 0L);

            int textX = this.getX() + 4;
            int textY = this.getY() + (this.height - 8) / 2;
            int textEndX = textX;

            if (highlightPosInDisplayed > displayedText.length()) {
                highlightPosInDisplayed = displayedText.length();
            }

            if (!displayedText.isEmpty()) {
                String beforeCursor = cursorInView ? displayedText.substring(0, cursorPosInDisplayed) : displayedText;
                textEndX = guiGraphics.drawString(font, beforeCursor, textX, textY, textColor, false);
            }

            int maxLength = (int) getMaxLengthMethod.invoke(this);
            boolean cursorAtEnd = cursorPos < value.length() || value.length() >= maxLength;
            int cursorX = textEndX;
            if (!cursorInView) {
                cursorX = cursorPosInDisplayed > 0 ? textX + this.width : textX;
            } else if (cursorAtEnd) {
                cursorX = textEndX - 1;
                textEndX--;
            }

            if (!displayedText.isEmpty() && cursorInView && cursorPosInDisplayed < displayedText.length()) {
                guiGraphics.drawString(font, displayedText.substring(cursorPosInDisplayed), textEndX, textY, textColor, false);
            }

            if (shouldDrawCursor) {
                if (cursorAtEnd) {
                    guiGraphics.fill(cursorX, textY - 1, cursorX + 1, textY + 1 + 9, -3092272);
                } else {
                    guiGraphics.drawString(font, "_", cursorX, textY, textColor, false);
                }
            }

            if (highlightPosInDisplayed != cursorPosInDisplayed) {
                int selectionStartX = textX + font.width(displayedText.substring(0, highlightPosInDisplayed));
                renderHighlightMethod.invoke(this, guiGraphics, cursorX, textY - 1, selectionStartX - 1, textY + 1 + 9);
            }
        } catch (Exception e) {
            // Fallback to default rendering if reflection fails
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
