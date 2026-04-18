package com.leon.saintsdragons.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Displays a temporary notification when the player switches melee modes (X key).
 * Shows which attack mode is now active (e.g., "Bite Mode" or "Horn Gore Mode").
 */
public class MeleeModeNotification {
    private static final long DISPLAY_DURATION_MS = 3000L; // 3 seconds
    private static final long SLIDE_IN_DURATION_MS = 200L; // 0.2 seconds
    private static final long SLIDE_OUT_DURATION_MS = 250L; // 0.25 seconds
    private static final long TOTAL_DURATION_MS =
            DISPLAY_DURATION_MS + SLIDE_IN_DURATION_MS + SLIDE_OUT_DURATION_MS;

    private Component message = null;
    private int meleeMode = -1;
    private long showTime = 0L;
    private boolean visible = false;

    /**
     * Show a notification for the given melee mode
     * @param mode 0 = primary, 1 = secondary, 2 = mining
     */
    public void showNotification(int mode) {
        this.meleeMode = mode;
        this.showTime = System.currentTimeMillis();
        this.visible = true;

        // Set message based on mode
        if (mode == 0) {
            this.message = Component.translatable("ui.saintsdragons.melee_mode.primary");
        } else if (mode == 1) {
            this.message = Component.translatable("ui.saintsdragons.melee_mode.secondary");
        } else if (mode == 2) {
            this.message = Component.translatable("ui.saintsdragons.melee_mode.mining");
        } else {
            this.message = Component.literal("Mode " + mode);
        }
    }

    /**
     * Tick the notification (check if it should hide)
     */
    public void tick() {
        if (visible) {
            long elapsed = System.currentTimeMillis() - showTime;
            if (elapsed >= TOTAL_DURATION_MS) {
                visible = false;
                message = null;
            }
        }
    }

    /**
     * Render the notification on screen
     */
    public void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (!visible || message == null) {
            return;
        }

        long elapsed = System.currentTimeMillis() - showTime;
        if (elapsed < 0L) {
            elapsed = 0L;
        }
        if (elapsed >= TOTAL_DURATION_MS) {
            visible = false;
            message = null;
            return;
        }

        int textWidth = net.minecraft.client.Minecraft.getInstance().font.width(message);
        int padding = 6;
        int boxWidth = textWidth + padding * 2;
        int boxHeight = 14;

        // Position: right side of screen, slightly above center
        int targetX = screenWidth - boxWidth - 10; // 10px from right edge
        int y = screenHeight / 2 - 40;

        // Calculate slide animation progress
        float slideProgress;
        float fadeProgress;
        if (elapsed < SLIDE_IN_DURATION_MS) {
            float normalized = (float) elapsed / SLIDE_IN_DURATION_MS;
            slideProgress = easeOutCubic(normalized);
            fadeProgress = slideProgress;
        } else if (elapsed < SLIDE_IN_DURATION_MS + DISPLAY_DURATION_MS) {
            slideProgress = 1.0f;
            fadeProgress = 1.0f;
        } else {
            long slideOutElapsed = elapsed - SLIDE_IN_DURATION_MS - DISPLAY_DURATION_MS;
            float normalized = 1.0f - (float) slideOutElapsed / SLIDE_OUT_DURATION_MS;
            normalized = Math.max(0.0f, Math.min(1.0f, normalized));
            slideProgress = easeInCubic(normalized);
            fadeProgress = slideProgress;
        }
        slideProgress = Math.max(0.0f, Math.min(1.0f, slideProgress));
        fadeProgress = Math.max(0.0f, Math.min(1.0f, fadeProgress));

        // Calculate X position with slide offset
        int slideOffset = (int) ((1.0f - slideProgress) * (boxWidth + 20));
        int x = targetX + slideOffset;

        int bgAlpha = Math.round(0x90 * fadeProgress);
        int borderAlpha = Math.round(0xFF * fadeProgress);
        int textAlpha = Math.round(0xFF * fadeProgress);

        int bgColor = (bgAlpha << 24) | 0x000000;
        int borderColor = (borderAlpha << 24) | 0x00555555;
        int textColor = (textAlpha << 24) | 0x00FFFFFF;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Draw background (slightly transparent)
        guiGraphics.fill(x, y - 2, x + boxWidth, y + boxHeight - 2, bgColor);

        // Draw border
        guiGraphics.fill(x, y - 2, x + boxWidth, y - 1, borderColor); // Top
        guiGraphics.fill(x, y + boxHeight - 3, x + boxWidth, y + boxHeight - 2, borderColor); // Bottom
        guiGraphics.fill(x, y - 2, x + 1, y + boxHeight - 2, borderColor); // Left
        guiGraphics.fill(x + boxWidth - 1, y - 2, x + boxWidth, y + boxHeight - 2, borderColor); // Right

        // Draw text
        guiGraphics.drawString(
            net.minecraft.client.Minecraft.getInstance().font,
            message,
            x + padding,
            y,
            textColor,
            false
        );

        RenderSystem.disableBlend();
    }

    /**
     * Ease out cubic function for smooth slide-in
     */
    private float easeOutCubic(float t) {
        float f = 1.0f - t;
        return 1.0f - f * f * f;
    }

    /**
     * Ease in cubic function for smooth slide-out
     */
    private float easeInCubic(float t) {
        return t * t * t;
    }

    /**
     * Check if notification is currently visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Get current melee mode being displayed
     */
    public int getMeleeMode() {
        return meleeMode;
    }
}
