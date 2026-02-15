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
            String emptyText = Component.translatable("saintsdragons.gui.draconic_codex.empty").getString();
            String[] words = emptyText.split(" ");
            if (words.length >= 3) {
                String line1 = words[0] + " " + words[1];
                StringBuilder line2 = new StringBuilder();
                for (int i = 2; i < words.length; i++) {
                    if (line2.length() > 0) {
                        line2.append(" ");
                    }
                    line2.append(words[i]);
                }
                guiGraphics.drawString(font, line1, left, top, CodexLayout.TEXT_COLOR, false);
                guiGraphics.drawString(font, line2.toString(), left, top + 10, CodexLayout.TEXT_COLOR, false);
            } else {
                int maxWidth = Math.max(10, right - left - 4);
                int y = top;
                int maxLines = 2;
                var wrapped = font.split(Component.literal(emptyText), maxWidth);
                for (int i = 0; i < Math.min(maxLines, wrapped.size()); i++) {
                    guiGraphics.drawString(font, wrapped.get(i), left, y, CodexLayout.TEXT_COLOR, false);
                    y += 10;
                }
            }
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
