package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Displays a charge bar near the crosshair when the player is charging a fireball.
 * Shows 3 charge levels with custom textures.
 */
public class FireballChargeIndicator {
    private static final ResourceLocation CHARGE_BAR = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/ignivorus/fireball_charge_bar.png");
    private static final ResourceLocation CHARGE_BAR_FLASH = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/ignivorus/fireball_charge_bar_flashes.png");
    private static final ResourceLocation CHARGE_LEVEL_1 = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/ignivorus/fireball_first_charge.png");
    private static final ResourceLocation CHARGE_LEVEL_2 = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/ignivorus/fireball_second_charge.png");
    private static final ResourceLocation CHARGE_LEVEL_3 = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/ignivorus/fireball_third_charge.png");

    // Texture dimensions
    private static final int BAR_WIDTH = 128;
    private static final int BAR_HEIGHT = 32;
    private static final int FLASH_WIDTH = 130;
    private static final int FLASH_HEIGHT = 32;

    // Animation constants
    private static final float FILL_RISE_SPEED = 0.08f;
    private static final float FILL_FALL_SPEED = 0.15f;
    private static final float FADE_SPEED = 0.08f;
    private static final float FLASH_DURATION_TICKS = 20.0f;

    private int chargeLevel = 0;
    private float previousAnimatedFill = 0f;
    private float animatedFill = 0f;

    // Fade out animation
    private float previousFadeAlpha = 1.0f;
    private float fadeAlpha = 1.0f;
    private boolean isFadingOut = false;
    private boolean wasFull = false;
    private boolean flashActive = false;
    private float previousFlashTimer = 0f;
    private float flashTimer = 0f;

    public FireballChargeIndicator() {
    }

    /**
     * Update the charge level (0 = not charging, 1-3 = charge level)
     */
    public void setChargeLevel(int level) {
        int newLevel = Math.max(0, Math.min(3, level));

        // Detect when charge is released (goes from charging to 0)
        if (this.chargeLevel > 0 && newLevel == 0) {
            isFadingOut = true;
        }

        // Reset if starting a new charge
        if (this.chargeLevel == 0 && newLevel > 0) {
            isFadingOut = false;
            fadeAlpha = 1.0f;
            animatedFill = 0f;
            previousAnimatedFill = 0f;
            previousFadeAlpha = 1.0f;
            wasFull = false;
            flashActive = false;
            previousFlashTimer = 0f;
            flashTimer = 0f;
        }

        this.chargeLevel = newLevel;
    }

    /**
     * Tick animations
     */
    public void tick() {
        previousAnimatedFill = animatedFill;
        previousFadeAlpha = fadeAlpha;
        previousFlashTimer = flashTimer;

        float targetFill = chargeLevel / 3.0f;
        // Smoothly animate toward target
        if (animatedFill < targetFill) {
            animatedFill = Math.min(targetFill, animatedFill + FILL_RISE_SPEED);
        } else if (animatedFill > targetFill) {
            animatedFill = Math.max(targetFill, animatedFill - FILL_FALL_SPEED);
        }

        // Animate fade out
        if (isFadingOut) {
            fadeAlpha = Math.max(0f, fadeAlpha - FADE_SPEED);
            if (fadeAlpha <= 0f) {
                isFadingOut = false;
                animatedFill = 0f;
            }
        } else if (chargeLevel > 0) {
            fadeAlpha = Math.min(1.0f, fadeAlpha + FADE_SPEED);
        } else {
            fadeAlpha = Math.max(0f, fadeAlpha - FADE_SPEED);
        }

        boolean nowFull = chargeLevel >= 3 && !isFadingOut && animatedFill >= 0.999f;
        if (nowFull && !wasFull) {
            flashActive = true;
            flashTimer = 0f;
            previousFlashTimer = 0f;
        }
        wasFull = nowFull;

        if (flashActive) {
            flashTimer = Math.min(FLASH_DURATION_TICKS, flashTimer + 1.0f);
            if (flashTimer >= FLASH_DURATION_TICKS) {
                flashActive = false;
                flashTimer = 0f;
                previousFlashTimer = 0f;
            }
        }
    }

    /**
     * Render the charge indicator near the crosshair
     */
    public void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTicks) {
        if (chargeLevel <= 0 && animatedFill <= 0.01f && fadeAlpha <= 0.01f) {
            return; // Don't render if not charging and fade is complete
        }

        float clampedPartial = clamp(partialTicks, 0.0f, 1.0f);
        float smoothFill = lerp(previousAnimatedFill, animatedFill, clampedPartial);
        float smoothAlpha = lerp(previousFadeAlpha, fadeAlpha, clampedPartial);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Position: centered horizontally, above hotbar
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - 86;

        // Apply fade alpha to all rendering
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, smoothAlpha);

        // Render the background bar (always full size)
        guiGraphics.blit(CHARGE_BAR, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

        int fillWidth = Math.max(0, Math.min(BAR_WIDTH, Math.round(BAR_WIDTH * smoothFill)));
        if (fillWidth > 0) {
            renderChargeLevel(guiGraphics, x, y, 1, CHARGE_LEVEL_1, fillWidth, smoothFill, smoothAlpha);
            renderChargeLevel(guiGraphics, x, y, 2, CHARGE_LEVEL_2, fillWidth, smoothFill, smoothAlpha);
            renderChargeLevel(guiGraphics, x, y, 3, CHARGE_LEVEL_3, fillWidth, smoothFill, smoothAlpha);
        }

        if (smoothAlpha > 0.01f && (flashActive || wasFull)) {
            float smoothFlashTimer = lerp(previousFlashTimer, flashTimer, clampedPartial);
            float flashAlpha = computeFlashAlpha(smoothFlashTimer, FLASH_DURATION_TICKS);
            if (flashAlpha > 0.01f) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, smoothAlpha * flashAlpha);
                int flashX = x - (FLASH_WIDTH - BAR_WIDTH) / 2;
                guiGraphics.blit(CHARGE_BAR_FLASH, flashX, y, 0, 0, FLASH_WIDTH, FLASH_HEIGHT, FLASH_WIDTH, FLASH_HEIGHT);
            }
        }

        // Reset shader color
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private void renderChargeLevel(GuiGraphics guiGraphics, int baseX, int baseY, int level,
                                   ResourceLocation texture, int fillWidth, float smoothFill, float smoothAlpha) {
        float levelProgress = clamp(smoothFill * 3.0f - (level - 1), 0.0f, 1.0f);
        if (levelProgress <= 0.01f) {
            return;
        }

        float alpha = smoothAlpha * levelProgress;
        if (chargeLevel >= 3 && !isFadingOut && level == 3) {
            float pulse = (float) Math.sin((System.currentTimeMillis() % 900L) / 900.0f * Math.PI * 2.0f);
            alpha = clamp(alpha + (pulse * 0.08f), 0.0f, 1.0f);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        guiGraphics.blit(texture, baseX, baseY, 0, 0, fillWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
    }

    /**
     * Check if the indicator should be rendered
     */
    public boolean shouldRender() {
        return chargeLevel > 0 || animatedFill > 0.01f || fadeAlpha > 0.01f;
    }

    /**
     * Get the current charge level
     */
    public int getChargeLevel() {
        return chargeLevel;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    private static float computeFlashAlpha(float timer, float duration) {
        if (duration <= 0f) {
            return 0f;
        }
        float progress = clamp(timer / duration, 0.0f, 1.0f);
        if (progress <= 0.5f) {
            return progress / 0.5f;
        }
        return (1.0f - progress) / 0.5f;
    }
}
