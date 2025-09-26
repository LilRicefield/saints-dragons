package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Health bar UI element for dragons.
 * Displays current health with dragon-specific theming.
 */
public class DragonHealthBar extends DragonUIElement {
    private static final ResourceLocation LIGHTNING_HEALTH_BORDER = 
        ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/lightningdragon/health_border.png");
    private static final ResourceLocation LIGHTNING_HEALTH_FILL = 
        ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/lightningdragon/health_fill.png");
    
    private DragonEntity dragon;
    private float currentHealthPercent = 1.0f;
    private float targetHealthPercent = 1.0f;
    private long lastHealthUpdate = 0;
    
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
        String healthText = String.format("%.0f/%.0f", dragon.getHealth(), dragon.getMaxHealth());
        int textWidth = minecraft.font.width(healthText);
        int textX = Math.max(4, x - textWidth - 8);
        int textY = y + (height - minecraft.font.lineHeight) / 2;
        guiGraphics.fill(textX - 2, textY - 2, textX + textWidth + 2, textY + minecraft.font.lineHeight + 2, 0x80000000);
        guiGraphics.drawString(minecraft.font, healthText, textX, textY, 0xFFFFFF);
        
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
