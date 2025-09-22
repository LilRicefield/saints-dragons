package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Control guide UI element for dragons.
 * Shows current keybinds and their functions, updates dynamically.
 */
public class DragonControlGuide extends DragonUIElement {
    private DragonEntity dragon;
    private final List<ControlEntry> controls = new ArrayList<>();
    
    public DragonControlGuide(int x, int y) {
        super(x, y, 150, 80); // Smaller, more compact size
    }
    
    public void setDragon(DragonEntity dragon) {
        this.dragon = dragon;
        updateControls();
    }
    
    private void updateControls() {
        controls.clear();
        if (dragon == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        
        // Melee attack (Left Click) - Special mouse icon
        controls.add(new ControlEntry(
            Component.literal("L-Click"), // Custom text for mouse
            Component.literal("Melee Attack"),
            0xFFFF0000, // Red for mouse click
            true // Is mouse icon
        ));
        
        // Ascend (Spacebar)
        controls.add(new ControlEntry(
            Component.literal("Spacebar"),
            Component.literal("Ascend"),
            0xFF00FFFF // Cyan
        ));
        
        // Descend (Left Alt)
        controls.add(new ControlEntry(
            Component.literal("L-Alt"),
            Component.literal("Descend"),
            0xFF00FFFF // Cyan
        ));
        
        // Roar (R)
        controls.add(new ControlEntry(
            Component.literal("R"),
            Component.literal("Ability 1"),
            0xFFFF8000 // Orange
        ));
        
        // Lightning Beam (G)
        controls.add(new ControlEntry(
            Component.literal("G"),
            Component.literal("Ability 2"),
            0xFFFFFF00 // Yellow
        ));
        
        // Summon Storm (H)
        controls.add(new ControlEntry(
            Component.literal("H"),
            Component.literal("Ability 3"),
            0xFF8000FF // Purple
        ));
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible || dragon == null || dragon.isDeadOrDying()) {
            return;
        }
        
        Font font = Minecraft.getInstance().font;
        
        // No background - transparent UI
        
        // Title (smaller)
        String title = "Dragon Controls";
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, x + (width - titleWidth) / 2, y + 2, 0xFFFFFFFF);
        
        // Render controls (more compact)
        int startY = y + 12;
        int lineHeight = 10;
        
        for (int i = 0; i < controls.size(); i++) {
            ControlEntry control = controls.get(i);
            int currentY = startY + i * lineHeight;
            
            if (control.isMouseIcon) {
                // Simple text-based mouse icon (no texture complications)
                guiGraphics.drawString(font, control.keyText, x + 2, currentY + 1, 0xFFFFFFFF);
                
                // Function text
                guiGraphics.drawString(font, control.functionText, x + 2 + font.width(control.keyText) + 4, currentY + 1, control.color);
            } else {
                // Regular keybind background (smaller)
                int keyWidth = font.width(control.keyText) + 4;
                guiGraphics.fill(x + 2, currentY, x + 2 + keyWidth, currentY + 8, 0xFF333333);
                guiGraphics.fill(x + 3, currentY + 1, x + 1 + keyWidth, currentY + 7, 0xFF666666);
                
                // Keybind text
                guiGraphics.drawString(font, control.keyText, x + 4, currentY + 1, 0xFFFFFFFF);
                
                // Function text
                guiGraphics.drawString(font, control.functionText, x + 2 + keyWidth + 4, currentY + 1, control.color);
            }
        }
        
        // Render drag handle when hovering
        if (isMouseOver(mouseX, mouseY)) {
            renderDragHandle(guiGraphics);
        }
    }
    
    private void renderDragHandle(GuiGraphics guiGraphics) {
        // Simple drag handle indicator
        int handleSize = 4;
        int handleX = x + width - handleSize - 2;
        int handleY = y + 2;
        
        guiGraphics.fill(handleX, handleY, handleX + handleSize, handleY + handleSize, 0xFFFFFFFF);
        guiGraphics.fill(handleX + 1, handleY + 1, handleX + handleSize - 1, handleY + handleSize - 1, 0xFF000000);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Only allow dragging from the drag handle area
        int handleSize = 4;
        int handleX = x + width - handleSize - 2;
        int handleY = y + 2;
        
        if (button == 0 && mouseX >= handleX && mouseX <= handleX + handleSize && 
            mouseY >= handleY && mouseY <= handleY + handleSize) {
            startDragging(mouseX, mouseY);
            return true;
        }
        return false;
    }
    
    private static class ControlEntry {
        final Component keyText;
        final Component functionText;
        final int color;
        final boolean isMouseIcon;
        
        ControlEntry(Component keyText, Component functionText, int color) {
            this.keyText = keyText;
            this.functionText = functionText;
            this.color = color;
            this.isMouseIcon = false;
        }
        
        ControlEntry(Component keyText, Component functionText, int color, boolean isMouseIcon) {
            this.keyText = keyText;
            this.functionText = functionText;
            this.color = color;
            this.isMouseIcon = isMouseIcon;
        }
    }
}
