package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Dragon Status UI manager.
 * Handles rendering and interaction for all dragon UI elements.
 */
public class DragonStatusUI {
    private final Minecraft minecraft;
    private final List<DragonUIElement> elements = new ArrayList<>();
    private DragonHealthBar healthBar;
    private DragonSpeedIndicator speedIndicator;
    private DragonControlGuide controlGuide;
    
    private boolean visible = false;
    private DragonEntity currentDragon = null;
    
    public DragonStatusUI() {
        this.minecraft = Minecraft.getInstance();
        initializeElements();
    }
    
    private void initializeElements() {
        // Create UI elements with responsive positions
        updateElementPositions();
        
        elements.add(healthBar);
        elements.add(speedIndicator);
        elements.add(controlGuide);
        
        // Load saved positions
        loadPositions();
    }
    
    /**
     * Update UI element positions based on screen size and scaling
     */
    private void updateElementPositions() {
        if (minecraft == null || minecraft.getWindow() == null) {
            // Fallback to default positions if window not available
            healthBar = new DragonHealthBar(380, 10);
            speedIndicator = new DragonSpeedIndicator(10, 130);
            controlGuide = new DragonControlGuide(10, 160);
            return;
        }
        
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        // Calculate responsive positions
        int leftMargin = Math.max(10, screenWidth / 50); // Minimum 10px, or 2% of screen width
        int rightMargin = Math.max(10, screenWidth / 50); // Same margin for right side
        int topMargin = Math.max(10, screenHeight / 50); // Minimum 10px, or 2% of screen height
        
        // Health bar on right side (responsive to screen width)
        int healthBarX = screenWidth - rightMargin - 20; // 20px for health bar width
        int healthBarY = topMargin;
        
        // Speed and controls on left side
        int leftX = leftMargin;
        int speedY = topMargin + 120; // 120px below health bar
        int controlsY = speedY + 40; // 40px below speed indicator
        
        // Create elements with responsive positions
        healthBar = new DragonHealthBar(healthBarX, healthBarY);
        speedIndicator = new DragonSpeedIndicator(leftX, speedY);
        controlGuide = new DragonControlGuide(leftX, controlsY);
    }
    
    /**
     * Update the UI with current dragon
     */
    public void updateDragon(DragonEntity dragon) {
        this.currentDragon = dragon;
        
        if (dragon != null) {
            healthBar.setDragon(dragon);
            speedIndicator.setDragon(dragon);
            controlGuide.setDragon(dragon);
        }
    }
    
    /**
     * Render the UI
     */
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible || currentDragon == null) {
            return;
        }
        
        // Check if player is riding the dragon
        Player player = minecraft.player;
        if (player == null || player.getVehicle() != currentDragon) {
            return;
        }
        
        // Render all elements
        for (DragonUIElement element : elements) {
            element.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }
    
    /**
     * Handle mouse click events
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        
        // Check elements in reverse order (top to bottom)
        for (int i = elements.size() - 1; i >= 0; i--) {
            DragonUIElement element = elements.get(i);
            if (element.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handle mouse release events
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        
        boolean handled = false;
        for (DragonUIElement element : elements) {
            if (element.mouseReleased(mouseX, mouseY, button)) {
                handled = true;
            }
        }
        
        // Save positions if any element was moved
        if (handled) {
            savePositions();
        }
        
        return handled;
    }
    
    /**
     * Handle mouse drag events
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!visible) return false;
        
        boolean handled = false;
        for (DragonUIElement element : elements) {
            if (element.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                handled = true;
            }
        }
        return handled;
    }
    
    /**
     * Toggle UI visibility
     */
    public void toggleVisibility() {
        visible = !visible;
        if (visible) {
            // Update dragon when showing UI
            updateDragon(currentDragon);
        }
    }
    
    /**
     * Set UI visibility
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) {
            updateDragon(currentDragon);
        }
    }
    
    /**
     * Check if UI is visible
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Save UI element positions to config
     */
    public void savePositions() {
        // TODO: Implement config saving
        // For now, we'll use a simple approach
        System.out.println("Saving UI positions:");
        for (DragonUIElement element : elements) {
            System.out.println(element.getClass().getSimpleName() + ": " + element.getPositionString());
        }
    }
    
    /**
     * Load UI element positions from config
     */
    private void loadPositions() {
        // TODO: Implement config loading
        // For now, use default positions
        System.out.println("Loading UI positions (using defaults)");
    }
    
    /**
     * Reset all UI elements to default positions
     */
    public void resetPositions() {
        // Update positions based on current screen size
        updateElementPositions();
        savePositions();
    }
    
    /**
     * Handle window resize - recalculate positions for new screen size
     */
    public void onWindowResize() {
        // Only update if UI is visible to avoid unnecessary calculations
        if (visible) {
            updateElementPositions();
            savePositions();
        }
    }
}
