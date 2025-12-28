package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Health bar UI element for dragons.
 * Displays current health with dragon-specific theming.
 */
public class DragonHealthBar extends DragonUIElement {
    private static final ResourceLocation RAEVYX_BASE = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/raevyx/raevyx_base.png");
    private static final ResourceLocation RAEVYX_OVERLAY = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/healthbar/raevyx/raevyx_overlay.png");

    private static final ResourceLocation MALE_ICON = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/genders/male.png");
    private static final ResourceLocation FEMALE_ICON = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "textures/gui/genders/female.png");

    // Texture dimensions
    private static final int TEXTURE_WIDTH = 32;
    private static final int TEXTURE_HEIGHT = 128;
    private static final int GENDER_ICON_SIZE = 16;

    private DragonEntity dragon;
    private float currentHealthPercent = 1.0f;
    private float targetHealthPercent = 1.0f;
    private long lastHealthUpdate = 0;
    
    public DragonHealthBar(int x, int y) {
        super(x, y, TEXTURE_WIDTH, TEXTURE_HEIGHT); // Vertical health bar
    }
    
    public void setDragon(DragonEntity dragon) {
        this.dragon = dragon;
        updateHealth();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible || dragon == null || dragon.isDeadOrDying()) {
            return;
        }

        updateHealth();

        boolean isRaevyx = dragon instanceof Raevyx;

        if (isRaevyx) {
            // Render textured health bar for Raevyx
            renderTexturedHealthBar(guiGraphics, RAEVYX_BASE, RAEVYX_OVERLAY);
        } else {
            // Fallback to colored rectangles for other dragons
            renderFallbackHealthBar(guiGraphics);
        }

        // Render gender icon
        renderGenderIcon(guiGraphics);

        // Render drag handle when hovering
        if (isMouseOver(mouseX, mouseY)) {
            renderDragHandle(guiGraphics);
        }
    }

    private void renderTexturedHealthBar(GuiGraphics guiGraphics, ResourceLocation baseTexture, ResourceLocation overlayTexture) {
        // Render overlay texture (background/backing) FIRST - always full size
        guiGraphics.blit(overlayTexture, x, y, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // Calculate how much of the base texture to show (bottom-up)
        int fillHeight = (int) (TEXTURE_HEIGHT * currentHealthPercent);

        if (fillHeight > 0) {
            // Render base texture (health fill) ON TOP - clips from top, renders from bottom up
            // For vertical bars that fill bottom-up, we need to:
            // 1. Skip the top portion of the texture (v offset)
            // 2. Render starting from where the health begins

            int vOffset = TEXTURE_HEIGHT - fillHeight; // How much of the texture to skip from top
            int renderY = y + vOffset; // Where to start rendering on screen

            // blit(ResourceLocation, x, y, u, v, width, height, textureWidth, textureHeight)
            guiGraphics.blit(baseTexture, x, renderY, 0, vOffset, TEXTURE_WIDTH, fillHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
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
