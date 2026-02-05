package com.leon.saintsdragons.client.ui.codex;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CodexDragonListPanel {
    public void draw(GuiGraphics guiGraphics, Font font, int left, int top, int right, int bottom,
                     int mouseX, int mouseY, List<CodexDragonEntry> dragonEntries, int listScrollOffset,
                     java.util.UUID selectedDragonId) {
        int visibleCount = Math.min(CodexLayout.MAX_VISIBLE_DRAGONS, dragonEntries.size() - listScrollOffset);
        if (visibleCount < 0) {
            visibleCount = 0;
        }

        for (int i = 0; i < visibleCount; i++) {
            int index = i + listScrollOffset;
            if (index >= dragonEntries.size()) {
                break;
            }
            CodexDragonEntry entry = dragonEntries.get(index);
            int y = top + (i * 13);
            boolean isSelected = entry.entityId() != null && entry.entityId().equals(selectedDragonId);
            int nameColor = isSelected ? 0xFF8B0000 : CodexLayout.TEXT_COLOR;
            guiGraphics.drawString(font, entry.displayName(), left, y, nameColor, false);
        }

        if (dragonEntries.isEmpty()) {
            guiGraphics.drawString(font,
                    Component.translatable("saintsdragons.gui.draconic_codex.empty").getString(),
                    left, top, CodexLayout.TEXT_COLOR, false);
        }

        if (dragonEntries.size() > CodexLayout.MAX_VISIBLE_DRAGONS) {
            if (listScrollOffset > 0) {
                guiGraphics.drawString(font, "↑", right - 60, top, CodexLayout.TEXT_COLOR, false);
            }
            if (listScrollOffset + CodexLayout.MAX_VISIBLE_DRAGONS < dragonEntries.size()) {
                guiGraphics.drawString(font, "↓", right - 60, bottom - 15, CodexLayout.TEXT_COLOR, false);
            }
        }
    }

    public java.util.UUID handleClick(double mouseX, double mouseY, int left, int top, int right,
                                      List<CodexDragonEntry> dragonEntries, int listScrollOffset) {
        int visibleCount = Math.min(CodexLayout.MAX_VISIBLE_DRAGONS, dragonEntries.size() - listScrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            int index = i + listScrollOffset;
            if (index >= dragonEntries.size()) {
                break;
            }
            int y = top + (i * 13);
            if (mouseX >= left && mouseX <= right && mouseY >= y && mouseY < y + 12) {
                CodexDragonEntry clickedEntry = dragonEntries.get(index);
                return clickedEntry.entityId();
            }
        }
        return null;
    }
}
