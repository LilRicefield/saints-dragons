package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Displays a beam energy meter near the crosshair when the Raevyx is beaming or has depleted energy.
 * Shows depletion as the beam is used, with cooldown regeneration.
 */
public class RaevyxBeamMeterIndicator {
    private static final ResourceLocation BEAM_BASE = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/raevyx/raevyx_beam_base.png");
    private static final ResourceLocation BEAM_OVERLAY = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/raevyx/raevyx_beam_overlay.png");
    private static final ResourceLocation BEAM_FLASH_RED = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/raevyx/raevyx_beam_overlay_flashes_red.png");
    private static final ResourceLocation BEAM_FLASH_WHITE = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/raevyx/raevyx_beam_overlay_flashes_white.png");

    // Texture dimensions (matching Ignivorus bar)
    private static final int BAR_WIDTH = 128;
    private static final int BAR_HEIGHT = 32;

    // Animation constants
    private static final float FILL_FALL_SPEED = 0.04f;  // Depletion speed
    private static final float FILL_RISE_SPEED = 0.02f;  // Regeneration speed (slower)
    private static final float FADE_SPEED = 0.08f;

    private float beamEnergy = 1.0f; // 0.0 to 1.0 (full to empty)
    private float previousAnimatedFill = 1.0f;
    private float animatedFill = 1.0f;

    // Fade out animation
    private float previousFadeAlpha = 1.0f;
    private float fadeAlpha = 1.0f;
    private boolean isFadingOut = false;
    private boolean isBeaming = false;
    private int hideTimer = 0;
    private static final int HIDE_DELAY_TICKS = 60; // 3 seconds after full energy

    // Flash animations
    private float redFlashAlpha = 0.0f;
    private float previousRedFlashAlpha = 0.0f;
    private float whiteFlashAlpha = 0.0f;
    private float previousWhiteFlashAlpha = 0.0f;

    public RaevyxBeamMeterIndicator() {
    }

    /**
     * Update the beam energy level (0.0 = empty, 1.0 = full)
     */
    public void setBeamEnergy(float energy) {
        this.beamEnergy = Math.max(0.0f, Math.min(1.0f, energy));

        // Show UI if energy is not full or is beaming
        if (beamEnergy < 0.999f || isBeaming) {
            isFadingOut = false;
            fadeAlpha = 1.0f;
            hideTimer = 0;
        }
    }

    /**
     * Set whether the dragon is currently beaming
     */
    public void setBeaming(boolean beaming) {
        this.isBeaming = beaming;
        if (beaming) {
            isFadingOut = false;
            fadeAlpha = 1.0f;
            hideTimer = 0;
        }
    }

    /**
     * Tick animations
     */
    public void tick() {
        previousAnimatedFill = animatedFill;
        previousFadeAlpha = fadeAlpha;
        previousRedFlashAlpha = redFlashAlpha;
        previousWhiteFlashAlpha = whiteFlashAlpha;

        float targetFill = beamEnergy;

        // Smoothly animate toward target
        if (animatedFill < targetFill) {
            // Regenerating - slower
            animatedFill = Math.min(targetFill, animatedFill + FILL_RISE_SPEED);
        } else if (animatedFill > targetFill) {
            // Depleting - faster
            animatedFill = Math.max(targetFill, animatedFill - FILL_FALL_SPEED);
        }

        // Auto-hide when full and not beaming
        if (beamEnergy >= 0.999f && !isBeaming) {
            if (hideTimer < HIDE_DELAY_TICKS) {
                hideTimer++;
            } else {
                isFadingOut = true;
            }
        } else {
            hideTimer = 0;
        }

        // Animate fade out
        if (isFadingOut) {
            fadeAlpha = Math.max(0f, fadeAlpha - FADE_SPEED);
            if (fadeAlpha <= 0f) {
                isFadingOut = false;
            }
        } else if (isBeaming || beamEnergy < 0.999f) {
            fadeAlpha = Math.min(1.0f, fadeAlpha + FADE_SPEED);
        }

        // White flash pulsates when actively beaming
        if (isBeaming) {
            // Sine wave pulse for smooth oscillation (faster cycle - 0.5 seconds)
            float pulse = (float) Math.sin((System.currentTimeMillis() % 500L) / 500.0f * Math.PI * 2.0f);
            whiteFlashAlpha = 0.6f + (pulse * 0.4f); // Oscillates between 0.2 and 1.0
        } else {
            // Fade out when not beaming
            whiteFlashAlpha = Math.max(0.0f, whiteFlashAlpha - 0.15f);
        }

        // Red flash pulse when low energy (below 25%) and NOT beaming
        if (beamEnergy < 0.25f && !isBeaming) {
            // Sine wave pulse for smooth oscillation
            float pulse = (float) Math.sin((System.currentTimeMillis() % 1000L) / 1000.0f * Math.PI * 2.0f);
            redFlashAlpha = 0.5f + (pulse * 0.5f); // Oscillates between 0.0 and 1.0
        } else {
            redFlashAlpha = 0.0f;
        }
    }

    /**
     * Render the beam meter near the crosshair
     */
    public void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTicks) {
        if (!shouldRender()) {
            return;
        }

        float clampedPartial = clamp(partialTicks, 0.0f, 1.0f);
        float smoothFill = lerp(previousAnimatedFill, animatedFill, clampedPartial);
        float smoothAlpha = lerp(previousFadeAlpha, fadeAlpha, clampedPartial);
        float smoothRedFlash = lerp(previousRedFlashAlpha, redFlashAlpha, clampedPartial);
        float smoothWhiteFlash = lerp(previousWhiteFlashAlpha, whiteFlashAlpha, clampedPartial);

        if (smoothAlpha <= 0.01f) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Position: centered horizontally, above hotbar (same as fireball charge)
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - 86;

        // White flash overlay when actively beaming (render first, underneath everything)
        if (smoothWhiteFlash > 0.01f) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, smoothAlpha * smoothWhiteFlash);
            guiGraphics.blit(BEAM_FLASH_WHITE, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        }

        // Apply fade alpha for base layer
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, smoothAlpha);

        // Calculate fill width based on beam energy (depletes from right to left)
        int fillWidth = Math.max(0, Math.min(BAR_WIDTH, Math.round(BAR_WIDTH * smoothFill)));

        // Render the base (depletion sprite) - only render the filled portion
        if (fillWidth > 0) {
            guiGraphics.blit(BEAM_BASE, x, y, 0, 0, fillWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        }

        // Render the overlay (border) - always full size
        guiGraphics.blit(BEAM_OVERLAY, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

        // Red flash overlay when low energy (below 25%) and NOT beaming
        if (smoothRedFlash > 0.01f) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, smoothAlpha * smoothRedFlash);
            guiGraphics.blit(BEAM_FLASH_RED, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        }

        // Reset shader color
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    /**
     * Check if the indicator should be rendered
     */
    public boolean shouldRender() {
        return isBeaming || animatedFill < 0.999f || fadeAlpha > 0.01f;
    }

    /**
     * Get the current beam energy
     */
    public float getBeamEnergy() {
        return beamEnergy;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }
}
