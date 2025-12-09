package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Health bar UI element for dragons.
 * Displays current health with wyvern-specific theming.
 */
public class DragonHealthBar extends DragonUIElement {
    private DragonEntity dragon;
    private float currentHealthPercent = 1.0f;
    private float targetHealthPercent = 1.0f;
    private long lastHealthUpdate = 0;

    // Cached text rendering values
    private String cachedHealthText = "";
    private int cachedTextWidth = 0;
    private float cachedHealth = -1;
    private float cachedMaxHealth = -1;
    private boolean cachedGender = false;
    
    public DragonHealthBar(int x, int y) {
        super(x, y, 18, 100); // Compact vertical health bar
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
        
        // Render health border (always full) - use colored rectangles for now
        guiGraphics.fill(x, y, x + width, y + height, 0x80FF0000); // Semi-transparent red border
        
        // Render health fill (clipped to health percentage)
        int fillHeight = (int) (height * currentHealthPercent);
        if (fillHeight > 0) {
            // Render from bottom up
            int fillY = y + height - fillHeight;
            guiGraphics.fill(x + 2, fillY + 2, x + width - 2, y + height - 2, 0xFF800000); // Dark red fill
        }
        
        // Render health text anchored to the left so longer values stay visible
        // Include gender symbol (♂/♀) for quick identification
        // Cache the text to avoid expensive String.format() every frame
        float currentHealth = dragon.getHealth();
        float maxHealth = dragon.getMaxHealth();
        boolean isFemale = dragon.isFemale();

        if (currentHealth != cachedHealth || maxHealth != cachedMaxHealth || isFemale != cachedGender) {
            String genderSymbol = isFemale ? "♀" : "♂";
            cachedHealthText = String.format("%s %.0f/%.0f", genderSymbol, currentHealth, maxHealth);
            cachedTextWidth = minecraft.font.width(cachedHealthText);
            cachedHealth = currentHealth;
            cachedMaxHealth = maxHealth;
            cachedGender = isFemale;
        }

        int textX = Math.max(4, x - cachedTextWidth - 8);
        int textY = y + (height - minecraft.font.lineHeight) / 2;

        guiGraphics.fill(textX - 2, textY - 2, textX + cachedTextWidth + 2, textY + minecraft.font.lineHeight + 2, 0x80000000);
        guiGraphics.drawString(minecraft.font, cachedHealthText, textX, textY, 0xFFFFFF);

        

        // Render drag handle when hovering
        if (isMouseOver(mouseX, mouseY)) {
            renderDragHandle(guiGraphics);
        }
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
