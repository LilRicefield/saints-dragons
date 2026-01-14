package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Displays a fire breath energy meter when riding Ignivorus.
 * Mirrors the Raevyx beam meter visuals with Ignivorus textures.
 */
public class IgnivorusFireBreathMeterIndicator {
    private static final ResourceLocation FIRE_BASE = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/ignivorus/ignivorus_fire_base.png");
    private static final ResourceLocation FIRE_OVERLAY = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/ignivorus/ignivorus_fire_overlay.png");
    private static final ResourceLocation FIRE_FLASH_RED = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/ignivorus/ignivorus_fire_overlay_flashes_red.png");
    private static final ResourceLocation FIRE_FLASH_WHITE = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/ignivorus/ignivorus_fire_overlay_flashes_white.png");
    private static final ResourceLocation FIRE_ICON = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/ignivorus/red_flame.png");

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 30;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 1;

    private static final float FILL_FALL_SPEED = 0.04f;
    private static final float FILL_RISE_SPEED = 0.02f;

    private float breathEnergy = 1.0f;
    private float previousAnimatedFill = 1.0f;
    private float animatedFill = 1.0f;

    private float previousFadeAlpha = 1.0f;
    private float fadeAlpha = 1.0f;

    private boolean isBreathing = false;

    private float redFlashAlpha = 0.0f;
    private float previousRedFlashAlpha = 0.0f;
    private float whiteFlashAlpha = 0.0f;
    private float previousWhiteFlashAlpha = 0.0f;

    public void setBreathEnergy(float energy) {
        this.breathEnergy = Math.max(0.0f, Math.min(1.0f, energy));
    }

    public void setBreathing(boolean breathing) {
        this.isBreathing = breathing;
    }

    public void tick() {
        previousAnimatedFill = animatedFill;
        previousFadeAlpha = fadeAlpha;
        previousRedFlashAlpha = redFlashAlpha;
        previousWhiteFlashAlpha = whiteFlashAlpha;

        float targetFill = breathEnergy;

        if (animatedFill < targetFill) {
            animatedFill = Math.min(targetFill, animatedFill + FILL_RISE_SPEED);
        } else if (animatedFill > targetFill) {
            animatedFill = Math.max(targetFill, animatedFill - FILL_FALL_SPEED);
        }

        fadeAlpha = 1.0f;

        if (isBreathing) {
            float pulse = (float) Math.sin((System.currentTimeMillis() % 500L) / 500.0f * Math.PI * 2.0f);
            whiteFlashAlpha = 0.6f + (pulse * 0.4f);
        } else {
            whiteFlashAlpha = Math.max(0.0f, whiteFlashAlpha - 0.15f);
        }

        if (breathEnergy < 0.25f && !isBreathing) {
            float pulse = (float) Math.sin((System.currentTimeMillis() % 1000L) / 1000.0f * Math.PI * 2.0f);
            redFlashAlpha = 0.5f + (pulse * 0.5f);
        } else {
            redFlashAlpha = 0.0f;
        }
    }

    public void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTicks) {
        float clampedPartial = clamp(partialTicks, 0.0f, 1.0f);
        float smoothFill = lerp(previousAnimatedFill, animatedFill, clampedPartial);
        float smoothAlpha = lerp(previousFadeAlpha, fadeAlpha, clampedPartial);
        float smoothRedFlash = lerp(previousRedFlashAlpha, redFlashAlpha, clampedPartial);
        float smoothWhiteFlash = lerp(previousWhiteFlashAlpha, whiteFlashAlpha, clampedPartial);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - 45;

        int iconX = x - ICON_SIZE - ICON_GAP;
        int iconY = y + (BAR_HEIGHT - ICON_SIZE) / 2;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, smoothAlpha);
        guiGraphics.blit(FIRE_ICON, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        if (smoothWhiteFlash > 0.01f) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, smoothAlpha * smoothWhiteFlash);
            guiGraphics.blit(FIRE_FLASH_WHITE, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, smoothAlpha);

        int fillWidth = Math.max(0, Math.min(BAR_WIDTH, Math.round(BAR_WIDTH * smoothFill)));
        if (fillWidth > 0) {
            guiGraphics.blit(FIRE_BASE, x, y, 0, 0, fillWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        }

        guiGraphics.blit(FIRE_OVERLAY, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

        if (smoothRedFlash > 0.01f) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, smoothAlpha * smoothRedFlash);
            guiGraphics.blit(FIRE_FLASH_RED, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }
}
