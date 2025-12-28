package com.leon.saintsdragons.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Displays a charge bar near the crosshair when the player is charging a fireball.
 * Shows 3 segments that fill up as charge increases.
 */
public class FireballChargeIndicator {
    private static final int BAR_WIDTH = 62;
    private static final int BAR_HEIGHT = 6;
    private static final int SEGMENT_COUNT = 3;
    private static final int SEGMENT_GAP = 2;

    // Colors for charge levels (ARGB format)
    private static final int COLOR_EMPTY = 0x60000000;      // Dark transparent
    private static final int COLOR_BORDER = 0xFF333333;     // Dark gray border
    private static final int COLOR_LEVEL_1 = 0xFFFF8C00;    // Orange
    private static final int COLOR_LEVEL_2 = 0xFFFF4500;    // Red-orange
    private static final int COLOR_LEVEL_3 = 0xFFFFFFFF;    // White (max charge)

    private int chargeLevel = 0;
    private float animatedFill = 0f;

    public FireballChargeIndicator() {
    }

    /**
     * Update the charge level (0 = not charging, 1-3 = charge level)
     */
    public void setChargeLevel(int level) {
        this.chargeLevel = Math.max(0, Math.min(3, level));
    }

    /**
     * Tick animation for smooth fill
     */
    public void tick() {
        float targetFill = chargeLevel / 3.0f;
        // Smoothly animate toward target
        if (animatedFill < targetFill) {
            animatedFill = Math.min(targetFill, animatedFill + 0.08f);
        } else if (animatedFill > targetFill) {
            animatedFill = Math.max(targetFill, animatedFill - 0.15f);
        }
    }

    /**
     * Render the charge indicator near the crosshair
     */
    public void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (chargeLevel <= 0 && animatedFill <= 0.01f) {
            return; // Don't render if not charging
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Position: centered horizontally, below crosshair
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = (screenHeight / 2) + 20; // 20 pixels below center

        // Draw background/border
        guiGraphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, COLOR_BORDER);
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, COLOR_EMPTY);

        // Calculate segment dimensions
        int totalGapWidth = SEGMENT_GAP * (SEGMENT_COUNT - 1);
        int segmentWidth = (BAR_WIDTH - totalGapWidth) / SEGMENT_COUNT;

        // Draw each segment
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            int segmentX = x + (segmentWidth + SEGMENT_GAP) * i;
            int segmentLevel = i + 1;

            // Determine fill amount for this segment
            float segmentFill = 0f;
            if (chargeLevel >= segmentLevel) {
                segmentFill = 1f;
            } else if (chargeLevel == segmentLevel - 1 && animatedFill > (segmentLevel - 1) / 3.0f) {
                // Partially filling this segment
                float segmentStart = (segmentLevel - 1) / 3.0f;
                float segmentEnd = segmentLevel / 3.0f;
                segmentFill = (animatedFill - segmentStart) / (segmentEnd - segmentStart);
                segmentFill = Math.max(0f, Math.min(1f, segmentFill));
            }

            if (segmentFill > 0f) {
                int fillWidth = (int) (segmentWidth * segmentFill);
                int color = getSegmentColor(segmentLevel);

                // Add pulsing effect for max charge
                if (chargeLevel == 3 && segmentLevel == 3) {
                    float pulse = (float) (Math.sin(System.currentTimeMillis() / 100.0) * 0.3 + 0.7);
                    color = applyBrightness(color, pulse);
                }

                guiGraphics.fill(segmentX, y, segmentX + fillWidth, y + BAR_HEIGHT, color);
            }

            // Draw segment separator (except for last segment)
            if (i < SEGMENT_COUNT - 1) {
                int separatorX = segmentX + segmentWidth;
                guiGraphics.fill(separatorX, y, separatorX + SEGMENT_GAP, y + BAR_HEIGHT, COLOR_EMPTY);
            }
        }

        RenderSystem.disableBlend();
    }

    private int getSegmentColor(int segmentLevel) {
        return switch (segmentLevel) {
            case 1 -> COLOR_LEVEL_1;
            case 2 -> COLOR_LEVEL_2;
            case 3 -> COLOR_LEVEL_3;
            default -> COLOR_LEVEL_1;
        };
    }

    private int applyBrightness(int color, float brightness) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * brightness);
        int g = (int) (((color >> 8) & 0xFF) * brightness);
        int b = (int) ((color & 0xFF) * brightness);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Check if the indicator should be rendered
     */
    public boolean shouldRender() {
        return chargeLevel > 0 || animatedFill > 0.01f;
    }

    /**
     * Get the current charge level
     */
    public int getChargeLevel() {
        return chargeLevel;
    }
}
