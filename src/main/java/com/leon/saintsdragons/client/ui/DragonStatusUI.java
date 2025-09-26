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
    private static final long SLIDE_DURATION_MS = 250L;

    private final Minecraft minecraft;
    private final List<DragonUIElement> elements = new ArrayList<>();
    private final DragonHealthBar healthBar;
    private final DragonSpeedIndicator speedIndicator;
    private final DragonControlGuide controlGuide;
    private int cachedScreenWidth = -1;
    private int cachedScreenHeight = -1;

    private boolean visible = false;
    private DragonEntity currentDragon = null;

    private boolean animationActive = false;
    private boolean animatingIn = false;
    private long animationStartTime = 0L;
    private int healthBarSlideDistance = 0;
    private int speedIndicatorSlideDistance = 0;
    private int controlGuideSlideDistance = 0;

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

        int leftMargin = Math.max(12, (int) (screenWidth * 0.02f));
        int rightMargin = Math.max(12, (int) (screenWidth * 0.02f));
        int topMargin = Math.max(14, (int) (screenHeight * 0.025f));
        int bottomMargin = Math.max(48, (int) (screenHeight * 0.06f));

        // Right aligned health bar with allowance for health text so it stays on-screen

        int healthBarX = Math.max(leftMargin, screenWidth - rightMargin - healthBar.getWidth());
        int healthBarY = topMargin;

        // Left column placement with responsive spacing
        int baseSpacing = Math.max(14, (int) (screenHeight * 0.03f));
        int columnSpacing = Math.max(8, (int) (screenHeight * 0.018f));
        int desiredSpeedY = topMargin + healthBar.getHeight() + baseSpacing;

        int maxColumnStart = Math.max(topMargin, screenHeight - bottomMargin - speedIndicator.getHeight() - columnSpacing - controlGuide.getHeight());
        int speedY = Math.min(desiredSpeedY, maxColumnStart);
        int controlsY = Math.min(speedY + speedIndicator.getHeight() + columnSpacing, screenHeight - bottomMargin - controlGuide.getHeight());
        controlsY = Math.max(topMargin, controlsY);

        healthBar.setPosition(healthBarX, healthBarY);
        speedIndicator.setPosition(leftMargin, speedY);
        controlGuide.setPosition(leftMargin, controlsY);

        updateSlideDistances();
    }

    private void applyFallbackLayout() {
        healthBar.setPosition(320, 10);
        speedIndicator.setPosition(10, 130);
        controlGuide.setPosition(10, 160);
        updateSlideDistances();
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

        updateElementPositions();
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

        boolean animating = animationActive && animatingIn;
        float animationProgress = 1.0f;
        if (animating) {
            long elapsed = System.currentTimeMillis() - animationStartTime;
            animationProgress = Math.min(1.0f, Math.max(0.0f, elapsed / (float) SLIDE_DURATION_MS));
        }

        float easedProgress = animating ? easeOutCubic(animationProgress) : 1.0f;
        int screenWidth = getCurrentScreenWidth();
        if (screenWidth <= 0) {
            screenWidth = cachedScreenWidth > 0 ? cachedScreenWidth : 400;
        }

        int healthOffsetX = 0;
        int speedOffsetX = 0;
        int controlOffsetX = 0;

        if (animating) {
            int healthMagnitude = Math.round((1.0f - easedProgress) * healthBarSlideDistance);
            int speedMagnitude = Math.round((1.0f - easedProgress) * speedIndicatorSlideDistance);
            int controlMagnitude = Math.round((1.0f - easedProgress) * controlGuideSlideDistance);

            healthOffsetX = drawsFromRight(healthBar, screenWidth) ? healthMagnitude : -healthMagnitude;
            speedOffsetX = drawsFromRight(speedIndicator, screenWidth) ? speedMagnitude : -speedMagnitude;
            controlOffsetX = drawsFromRight(controlGuide, screenWidth) ? controlMagnitude : -controlMagnitude;
        }

        for (DragonUIElement element : elements) {
            if (element == healthBar) {
                healthBar.renderWithOffset(guiGraphics, mouseX, mouseY, partialTicks, healthOffsetX, 0);
            } else if (element == speedIndicator) {
                speedIndicator.renderWithOffset(guiGraphics, mouseX, mouseY, partialTicks, speedOffsetX, 0);
            } else if (element == controlGuide) {
                controlGuide.renderWithOffset(guiGraphics, mouseX, mouseY, partialTicks, controlOffsetX, 0);
            } else {
                element.render(guiGraphics, mouseX, mouseY, partialTicks);
            }
        }

        if (animating && animationProgress >= 1.0f) {
            animationActive = false;
            animatingIn = false;
        }
    }

    /**
     * Handle mouse click events
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || isAnimating()) return false;

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
        if (!visible || isAnimating()) return false;

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
        if (!visible || isAnimating()) return false;

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
        setVisible(!visible);
    }

    /**
     * Set UI visibility
     */
    public void setVisible(boolean visible) {
        if (this.visible == visible) {
            if (visible && !isAnimating()) {
                startEnterAnimation();
            }
            return;
        }

        this.visible = visible;
        if (visible) {
            updateElementPositions();
            startEnterAnimation();
            updateDragon(currentDragon);
        } else {
            animationActive = false;
            animatingIn = false;
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

    private boolean isAnimating() {
        return animationActive && animatingIn;
    }

    private void startEnterAnimation() {
        updateSlideDistances();
        if (healthBarSlideDistance == 0 && speedIndicatorSlideDistance == 0 && controlGuideSlideDistance == 0) {
            animationActive = false;
            animatingIn = false;
            return;
        }

        animationActive = true;
        animatingIn = true;
        animationStartTime = System.currentTimeMillis();
    }

    private void updateSlideDistances() {
        int screenWidth = getCurrentScreenWidth();
        if (screenWidth <= 0) {
            screenWidth = cachedScreenWidth > 0 ? cachedScreenWidth : 400;
        }

        healthBarSlideDistance = computeSlideDistance(healthBar, screenWidth);
        speedIndicatorSlideDistance = computeSlideDistance(speedIndicator, screenWidth);
        controlGuideSlideDistance = computeSlideDistance(controlGuide, screenWidth);
    }

    private int computeSlideDistance(DragonUIElement element, int screenWidth) {
        if (element == null) {
            return 0;
        }

        if (drawsFromRight(element, screenWidth)) {
            return Math.max(0, screenWidth - element.getX() + element.getWidth());
        }
        return Math.max(0, element.getX() + element.getWidth());
    }

    private boolean drawsFromRight(DragonUIElement element, int screenWidth) {
        return element.getX() + (element.getWidth() / 2) >= screenWidth / 2;
    }

    private int getCurrentScreenWidth() {
        if (minecraft != null && minecraft.getWindow() != null) {
            return minecraft.getWindow().getGuiScaledWidth();
        }
        return cachedScreenWidth;
    }

    private static float easeOutCubic(float t) {
        float inverted = 1.0f - t;
        return 1.0f - inverted * inverted * inverted;
    }
}



