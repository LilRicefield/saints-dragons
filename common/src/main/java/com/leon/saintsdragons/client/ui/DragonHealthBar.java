package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Health bar UI element for dragons.
 * Displays current health with dragon-specific theming.
 */
public class DragonHealthBar extends DragonUIElement {
    // Raevyx textures (32x128)
    private static final ResourceLocation RAEVYX_BASE = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/raevyx/raevyx_base.png");
    private static final ResourceLocation RAEVYX_OVERLAY = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/raevyx/raevyx_overlay.png");

    // Cindervane textures (16x128)
    private static final ResourceLocation CINDERVANE_BASE = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/cindervane/cindervane_base.png");
    private static final ResourceLocation CINDERVANE_OVERLAY = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/cindervane/cindervane_overlay.png");

    // Nulljaw textures (32x128)
    private static final ResourceLocation NULLJAW_BASE = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/nulljaw/nulljaw_base.png");
    private static final ResourceLocation NULLJAW_OVERLAY = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/nulljaw/nulljaw_overlay.png");

    private static final ResourceLocation MALE_ICON = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/genders/male.png");
    private static final ResourceLocation FEMALE_ICON = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/genders/female.png");

    // Default dimensions
    private static final int DEFAULT_WIDTH = 16;
    private static final int DEFAULT_HEIGHT = 128;
    private static final int GENDER_ICON_SIZE = 16;

    // Dragon-specific dimensions (set when dragon is assigned)
    private int textureWidth = DEFAULT_WIDTH;
    private int textureHeight = DEFAULT_HEIGHT;

    private DragonEntity dragon;
    private float currentHealthPercent = 1.0f;
    private float targetHealthPercent = 1.0f;
    private long lastHealthUpdate = 0;

    // Cached text rendering values
    private String cachedHealthText = "";
    private int cachedTextWidth = 0;
    private float cachedHealth = -1;
    private float cachedMaxHealth = -1;
    
    public DragonHealthBar(int x, int y) {
        super(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT); // Vertical health bar
    }

    public void setDragon(DragonEntity dragon) {
        this.dragon = dragon;
        updateDimensions();
        updateHealth();
    }

    private void updateDimensions() {
        if (dragon instanceof Raevyx) {
            textureWidth = 32;
            textureHeight = 128;
        } else if (dragon instanceof Cindervane) {
            textureWidth = 16;
            textureHeight = 128;
        } else if (dragon instanceof Nulljaw) {
            textureWidth = 32;
            textureHeight = 128;
        } else {
            textureWidth = DEFAULT_WIDTH;
            textureHeight = DEFAULT_HEIGHT;
        }

        // Update the UI element dimensions
        this.width = textureWidth;
        this.height = textureHeight;
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible || dragon == null || dragon.isDeadOrDying()) {
            return;
        }

        updateHealth();

        // Render dragon-specific health bar
        if (dragon instanceof Raevyx) {
            renderTexturedHealthBar(guiGraphics, RAEVYX_BASE, RAEVYX_OVERLAY);
        } else if (dragon instanceof Cindervane) {
            renderTexturedHealthBar(guiGraphics, CINDERVANE_BASE, CINDERVANE_OVERLAY);
        } else if (dragon instanceof Nulljaw) {
            renderTexturedHealthBar(guiGraphics, NULLJAW_BASE, NULLJAW_OVERLAY);
        } else {
            // Fallback to colored rectangles for other dragons
            renderFallbackHealthBar(guiGraphics);
        }

        // Render gender icon
        renderGenderIcon(guiGraphics);

        // Render health numbers for dragons with custom health bars
        if (dragon instanceof Raevyx || dragon instanceof Cindervane || dragon instanceof Nulljaw) {
            renderHealthText(guiGraphics);
        }

        // Render drag handle when hovering
        if (isMouseOver(mouseX, mouseY)) {
            renderDragHandle(guiGraphics);
        }
    }

    private void renderTexturedHealthBar(GuiGraphics guiGraphics, ResourceLocation baseTexture, ResourceLocation overlayTexture) {
        // Render overlay texture (background/backing) FIRST - always full size
        guiGraphics.blit(overlayTexture, x, y, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);

        // Calculate how much of the base texture to show (bottom-up)
        int fillHeight = (int) (textureHeight * currentHealthPercent);

        if (fillHeight > 0) {
            // Render base texture (health fill) ON TOP - clips from top, renders from bottom up
            // For vertical bars that fill bottom-up, we need to:
            // 1. Skip the top portion of the texture (v offset)
            // 2. Render starting from where the health begins

            int vOffset = textureHeight - fillHeight; // How much of the texture to skip from top
            int renderY = y + vOffset; // Where to start rendering on screen

            // blit(ResourceLocation, x, y, u, v, width, height, textureWidth, textureHeight)
            guiGraphics.blit(baseTexture, x, renderY, 0, vOffset, textureWidth, fillHeight, textureWidth, textureHeight);
        }
    }

    private void renderFallbackHealthBar(GuiGraphics guiGraphics) {
        // Old colored rectangle rendering for non-Raevyx dragons
        guiGraphics.fill(x, y, x + width, y + height, 0x80FF0000); // Semi-transparent red border

        int fillHeight = (int) (height * currentHealthPercent);
        if (fillHeight > 0) {
            int fillY = y + height - fillHeight;
            guiGraphics.fill(x + 2, fillY + 2, x + width - 2, y + height - 2, 0xFF800000); // Dark red fill
        }
    }

    private void renderGenderIcon(GuiGraphics guiGraphics) {
        boolean isFemale = dragon.isFemale();
        ResourceLocation genderIcon = isFemale ? FEMALE_ICON : MALE_ICON;

        // Position the gender icon to the right of the health bar, vertically centered
        int iconX = x + width + 4;
        int iconY = y + (height - GENDER_ICON_SIZE) / 2;

        guiGraphics.blit(genderIcon, iconX, iconY, 0, 0, GENDER_ICON_SIZE, GENDER_ICON_SIZE, GENDER_ICON_SIZE, GENDER_ICON_SIZE);
    }

    private void renderHealthText(GuiGraphics guiGraphics) {
        // Cache the text to avoid expensive String.format() every frame
        float currentHealth = dragon.getHealth();
        float maxHealth = dragon.getMaxHealth();

        if (currentHealth != cachedHealth || maxHealth != cachedMaxHealth) {
            cachedHealthText = String.format("%.0f/%.0f", currentHealth, maxHealth);
            cachedTextWidth = minecraft.font.width(cachedHealthText);
            cachedHealth = currentHealth;
            cachedMaxHealth = maxHealth;
        }

        // Position text at the top of the health bar, centered
        int textX = x + (width - cachedTextWidth) / 2;
        int textY = y - minecraft.font.lineHeight - 1; // 1 pixel above the health bar

        // Render black background
        guiGraphics.fill(textX - 2, textY - 1, textX + cachedTextWidth + 2, textY + minecraft.font.lineHeight + 1, 0xFF000000);

        // Render text with dragon-specific color
        int textColor = getHealthTextColor();
        guiGraphics.drawString(minecraft.font, cachedHealthText, textX, textY, textColor);
    }

    private int getHealthTextColor() {
        if (dragon instanceof Raevyx) {
            return 0xDC143C; // Crimson
        } else if (dragon instanceof Cindervane) {
            return 0xFFA500; // Orange
        } else if (dragon instanceof Nulljaw) {
            return 0x8B008B; // Dark purple
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
    
    private void renderDragHandle(GuiGraphics guiGraphics) {
        // Simple drag handle indicator
        int handleSize = 4;
        int handleX = x + width - handleSize - 2;
        int handleY = y + 2;
        
        guiGraphics.fill(handleX, handleY, handleX + handleSize, handleY + handleSize, 0xFFFFFFFF);
        guiGraphics.fill(handleX + 1, handleY + 1, handleX + handleSize - 1, handleY + handleSize - 1, 0xFF000000);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Only allow dragging from the drag handle area
        int handleSize = 4;
        int handleX = x + width - handleSize - 2;
        int handleY = y + 2;
        
        if (button == 0 && mouseX >= handleX && mouseX <= handleX + handleSize && 
            mouseY >= handleY && mouseY <= handleY + handleSize) {
            startDragging(mouseX, mouseY);
            return true;
        }
        return false;
    }
}
