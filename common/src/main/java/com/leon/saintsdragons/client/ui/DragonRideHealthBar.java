package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Horizontal dragon health bar displayed above hotbar when riding.
 * 128x32 pixels, centered on screen.
 */
public class DragonRideHealthBar {
    // Raevyx textures (128x32 horizontal)
    private static final ResourceLocation RAEVYX_BASE = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/raevyx/raevyx_base.png");
    private static final ResourceLocation RAEVYX_OVERLAY = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/raevyx/raevyx_overlay.png");
    private static final ResourceLocation IGNIVORUS_BASE = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/ignivorus/ignivorus_base.png");
    private static final ResourceLocation IGNIVORUS_OVERLAY = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/ignivorus/ignivorus_overlay.png");
    private static final ResourceLocation CINDERVANE_BASE = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/cindervane/cindervane_base.png");
    private static final ResourceLocation CINDERVANE_OVERLAY = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/cindervane/cindervane_overlay.png");
    private static final ResourceLocation NULLJAW_BASE = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/nulljaw/nulljaw_base.png");
    private static final ResourceLocation NULLJAW_OVERLAY = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/nulljaw/nulljaw_overlay.png");

    // Bar dimensions
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 22;

    private DragonEntity dragon;
    private float currentHealthPercent = 1.0f;
    private float targetHealthPercent = 1.0f;
    private long lastHealthUpdate = 0;

    // Cached text rendering values
    private String cachedHealthText = "";
    private int cachedTextWidth = 0;
    private float cachedHealth = -1;
    private float cachedMaxHealth = -1;

    private final Minecraft minecraft;

    public DragonRideHealthBar() {
        this.minecraft = Minecraft.getInstance();
    }

    public void setDragon(DragonEntity dragon) {
        this.dragon = dragon;
        updateHealth();
    }

    public DragonEntity getDragon() {
        return dragon;
    }

    /**
     * Render the health bar above the hotbar
     */
    public void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTicks) {
        if (dragon == null || dragon.isDeadOrDying()) {
            return;
        }

        updateHealth();

        // Position: centered horizontally, above hotbar (same Y as beam meter)
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - 57; // Above hotbar
        if (dragon instanceof Cindervane || dragon instanceof Nulljaw) {
            y += 6;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Render dragon-specific health bar
        if (dragon instanceof Raevyx) {
            renderTexturedHealthBar(guiGraphics, x, y, RAEVYX_BASE, RAEVYX_OVERLAY);
        } else if (dragon instanceof Ignivorus) {
            renderTexturedHealthBar(guiGraphics, x, y, IGNIVORUS_BASE, IGNIVORUS_OVERLAY);
        } else if (dragon instanceof Cindervane) {
            renderTexturedHealthBar(guiGraphics, x, y, CINDERVANE_BASE, CINDERVANE_OVERLAY);
        } else if (dragon instanceof Nulljaw) {
            renderTexturedHealthBar(guiGraphics, x, y, NULLJAW_BASE, NULLJAW_OVERLAY);
        } else {
            // Fallback colored bar for other dragons
            renderFallbackHealthBar(guiGraphics, x, y);
        }

        // Render health text
        renderHealthText(guiGraphics, x, y);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private void renderTexturedHealthBar(GuiGraphics guiGraphics, int x, int y, ResourceLocation baseTexture, ResourceLocation overlayTexture) {
        // Render overlay texture (background/backing) FIRST - always full size
        guiGraphics.blit(overlayTexture, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

        // Calculate how much of the base texture to show (left-to-right)
        int fillWidth = Math.max(0, Math.min(BAR_WIDTH, Math.round(BAR_WIDTH * currentHealthPercent)));

        if (fillWidth > 0) {
            // Render base texture (health fill) ON TOP - clips from right, renders left-to-right
            guiGraphics.blit(baseTexture, x, y, 0, 0, fillWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        }
    }

    private void renderFallbackHealthBar(GuiGraphics guiGraphics, int x, int y) {
        // Border
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF333333);

        // Background
        guiGraphics.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + BAR_HEIGHT - 1, 0xFF1A1A1A);

        // Health fill
        int fillWidth = Math.max(0, Math.round((BAR_WIDTH - 2) * currentHealthPercent));
        if (fillWidth > 0) {
            // Color based on health percentage
            int color = getHealthColor(currentHealthPercent);
            guiGraphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + BAR_HEIGHT - 1, color);
        }
    }

    private int getHealthColor(float healthPercent) {
        if (healthPercent > 0.75f) {
            return 0xFF00FF00; // Green
        } else if (healthPercent > 0.5f) {
            return 0xFFFFFF00; // Yellow
        } else if (healthPercent > 0.25f) {
            return 0xFFFF8800; // Orange
        } else {
            return 0xFFFF0000; // Red
        }
    }

    private void renderHealthText(GuiGraphics guiGraphics, int x, int y) {
        // Cache the text to avoid expensive String.format() every frame
        float currentHealth = dragon.getHealth();
        float maxHealth = dragon.getMaxHealth();

        if (currentHealth != cachedHealth || maxHealth != cachedMaxHealth) {
            cachedHealthText = String.format("%.0f/%.0f", currentHealth, maxHealth);
            cachedTextWidth = minecraft.font.width(cachedHealthText);
            cachedHealth = currentHealth;
            cachedMaxHealth = maxHealth;
        }

        // Position text centered on the health bar
        int textX = x + (BAR_WIDTH - cachedTextWidth) / 2;
        int textY = y + (BAR_HEIGHT - minecraft.font.lineHeight) / 2;

        // Render text with shadow for readability
        int textColor = getHealthTextColor();
        guiGraphics.drawString(minecraft.font, cachedHealthText, textX, textY, textColor, true);
    }

    private int getHealthTextColor() {
        if (dragon instanceof Raevyx) {
            return 0xFFFFFF; // White
        }
        return 0xFFFFFF; // White fallback
    }

    private void updateHealth() {
        if (dragon == null) return;

        float newHealthPercent = dragon.getHealth() / dragon.getMaxHealth();
        if (newHealthPercent != targetHealthPercent) {
            targetHealthPercent = newHealthPercent;
            lastHealthUpdate = System.currentTimeMillis();
        }

        // Smooth health bar animation
        long timeSinceUpdate = System.currentTimeMillis() - lastHealthUpdate;
        if (timeSinceUpdate < 500) { // 500ms animation
            float animationProgress = timeSinceUpdate / 500.0f;
            currentHealthPercent = currentHealthPercent + (targetHealthPercent - currentHealthPercent) * animationProgress;
        } else {
            currentHealthPercent = targetHealthPercent;
        }
    }
}
