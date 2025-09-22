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
    private final DragonHealthBar healthBar;
    private final DragonSpeedIndicator speedIndicator;
    private final DragonControlGuide controlGuide;
    private int cachedScreenWidth = -1;
    private int cachedScreenHeight = -1;

    private boolean visible = false;
    private DragonEntity currentDragon = null;

    public DragonStatusUI() {
        this.minecraft = Minecraft.getInstance();
        this.healthBar = new DragonHealthBar(0, 0);
        this.speedIndicator = new DragonSpeedIndicator(0, 0);
        this.controlGuide = new DragonControlGuide(0, 0);

        elements.add(healthBar);
        elements.add(speedIndicator);
        elements.add(controlGuide);

        updateElementPositions();
        loadPositions();
    }

    /**
     * Update UI element positions based on screen size and scaling
     */
    private void updateElementPositions() {
        if (minecraft == null || minecraft.getWindow() == null) {
            applyFallbackLayout();
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        if (screenWidth <= 0 || screenHeight <= 0) {
            applyFallbackLayout();
            return;
        }

        if (screenWidth == cachedScreenWidth && screenHeight == cachedScreenHeight) {
            return;
        }

        cachedScreenWidth = screenWidth;
        cachedScreenHeight = screenHeight;

        int leftMargin = Math.max(10, (int) (screenWidth * 0.02f));
        int rightMargin = Math.max(10, (int) (screenWidth * 0.02f));
        int topMargin = Math.max(10, (int) (screenHeight * 0.02f));

        // Right aligned health bar using actual width
        int healthBarX = screenWidth - rightMargin - healthBar.getWidth();
        int healthBarY = topMargin;

        // Left column placement with responsive spacing
        int baseSpacing = Math.max(12, (int) (screenHeight * 0.03f));
        int columnSpacing = Math.max(6, (int) (screenHeight * 0.015f));
        int desiredSpeedY = topMargin + healthBar.getHeight() + baseSpacing;

        int maxColumnStart = screenHeight - topMargin - speedIndicator.getHeight() - columnSpacing - controlGuide.getHeight();
        int speedY = Math.min(desiredSpeedY, Math.max(topMargin, maxColumnStart));
        int controlsY = speedY + speedIndicator.getHeight() + columnSpacing;

        healthBar.setPosition(healthBarX, healthBarY);
        speedIndicator.setPosition(leftMargin, speedY);
        controlGuide.setPosition(leftMargin, controlsY);
    }

    private void applyFallbackLayout() {
        healthBar.setPosition(380, 10);
        speedIndicator.setPosition(10, 130);
        controlGuide.setPosition(10, 160);
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
            updateElementPositions();
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
            updateElementPositions();
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
        cachedScreenWidth = -1;
        cachedScreenHeight = -1;
        updateElementPositions();
        savePositions();
    }

    /**
     * Handle window resize - recalculate positions for new screen size
     */
    public void onWindowResize() {
        updateElementPositions();
        if (visible) {
            savePositions();
        }
    }
}
