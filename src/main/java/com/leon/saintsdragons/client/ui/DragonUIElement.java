package com.leon.saintsdragons.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for all wyvern UI elements.
 * Provides common functionality for moveable, draggable UI components.
 */
public abstract class DragonUIElement {
    protected final Minecraft minecraft;
    protected int x, y;
    protected int width, height;
    protected boolean visible = true;
    protected boolean dragging = false;
    protected int dragOffsetX, dragOffsetY;
    
    public DragonUIElement(int x, int y, int width, int height) {
        this.minecraft = Minecraft.getInstance();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Render this UI element
     */
    public abstract void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks);

    /**
     * Render the UI element with a temporary positional offset.
     * The element state is restored after rendering so interactions keep using the base coordinates.
     */
    public void renderWithOffset(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, int offsetX, int offsetY) {
        if (offsetX == 0 && offsetY == 0) {
            render(guiGraphics, mouseX, mouseY, partialTicks);
            return;
        }

        int originalX = this.x;
        int originalY = this.y;
        this.x = originalX + offsetX;
        this.y = originalY + offsetY;

        try {
            render(guiGraphics, mouseX, mouseY, partialTicks);
        } finally {
            this.x = originalX;
            this.y = originalY;
        }
    }
    
    /**
     * Handle mouse click events
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            startDragging(mouseX, mouseY);
            return true;
        }
        return false;
    }
    
    /**
     * Handle mouse release events
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }
    
    /**
     * Handle mouse drag events
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            x = (int) (mouseX - dragOffsetX);
            y = (int) (mouseY - dragOffsetY);
            clampToScreen();
            return true;
        }
        return false;
    }
    
    /**
     * Check if mouse is over this element
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    /**
     * Start dragging this element
     */
    protected void startDragging(double mouseX, double mouseY) {
        dragging = true;
        dragOffsetX = (int) (mouseX - x);
        dragOffsetY = (int) (mouseY - y);
    }
    
    /**
     * Clamp element position to screen boundaries
     */
    protected void clampToScreen() {
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        x = Math.max(0, Math.min(x, screenWidth - width));
        y = Math.max(0, Math.min(y, screenHeight - height));
    }
    
    /**
     * Get the position as a string for saving
     */
    public String getPositionString() {
        return x + "," + y;
    }
    
    /**
     * Set position from string (for loading)
     */
    public void setPositionFromString(String position) {
        try {
            String[] parts = position.split(",");
            if (parts.length == 2) {
                x = Integer.parseInt(parts[0]);
                y = Integer.parseInt(parts[1]);
                clampToScreen();
            }
        } catch (NumberFormatException e) {
            // Invalid position string, keep default position
        }
    }
    
    // Getters and setters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public void setPosition(int x, int y) { 
        this.x = x; 
        this.y = y; 
        clampToScreen(); 
    }
}
