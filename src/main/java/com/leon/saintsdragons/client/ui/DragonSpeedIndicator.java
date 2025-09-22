package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Speed indicator UI element for dragons.
 * Displays current flight speed with a visual gauge.
 */
public class DragonSpeedIndicator extends DragonUIElement {
    private static final double BLOCKS_PER_TICK_TO_MPH = 44.738725;
    private static final float MIN_BASELINE_MPH = 5.0f;
    private static final float MAX_DECAY_FACTOR = 0.05f;

    private DragonEntity dragon;
    private float currentSpeedMph = 0.0f;
    private float maxSpeedMph = MIN_BASELINE_MPH;
    private float baselineMaxSpeedMph = MIN_BASELINE_MPH;

    public DragonSpeedIndicator(int x, int y) {
        super(x, y, 120, 16); // Extended width for mph label
    }

    public void setDragon(DragonEntity dragon) {
        this.dragon = dragon;
        if (dragon != null) {
            baselineMaxSpeedMph = resolveBaselineSpeedMph(dragon);
            maxSpeedMph = baselineMaxSpeedMph;
        } else {
            currentSpeedMph = 0.0f;
            baselineMaxSpeedMph = MIN_BASELINE_MPH;
            maxSpeedMph = MIN_BASELINE_MPH;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible || dragon == null || dragon.isDeadOrDying()) {
            return;
        }

        updateSpeed();

        // Background
        guiGraphics.fill(x, y, x + width, y + height, 0x80000000);

        // Speed gauge background
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF333333);

        // Speed gauge fill
        float speedPercent = maxSpeedMph <= 0.0f ? 0.0f : Math.min(currentSpeedMph / maxSpeedMph, 1.0f);
        int gaugeWidth = width - 4;
        int fillWidth = (int) (gaugeWidth * speedPercent);

        if (fillWidth > 0) {
            // Color based on speed (green to yellow to red)
            int color = getSpeedColor(speedPercent);
            guiGraphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + height - 2, color);
        }

        // Speed text
        Font font = minecraft.font;
        String speedText = String.format("%.0f mph", currentSpeedMph);
        int textX = x + width - font.width(speedText) - 2;
        int textY = y + (height - font.lineHeight) / 2;
        guiGraphics.drawString(font, speedText, textX, textY, 0xFFFFFF);

        // Label
        guiGraphics.drawString(font, "Speed:", x + 2, textY, 0xCCCCCC);

        // Render drag handle when hovering
        if (isMouseOver(mouseX, mouseY)) {
            renderDragHandle(guiGraphics);
        }
    }

    private void updateSpeed() {
        if (dragon == null) return;

        // Calculate current speed from velocity
        double velX = dragon.getDeltaMovement().x;
        double velY = dragon.getDeltaMovement().y;
        double velZ = dragon.getDeltaMovement().z;
        double blocksPerTick = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
        currentSpeedMph = (float) (blocksPerTick * BLOCKS_PER_TICK_TO_MPH);

        if (currentSpeedMph > maxSpeedMph) {
            maxSpeedMph = currentSpeedMph;
        } else if (maxSpeedMph > baselineMaxSpeedMph) {
            float decay = (maxSpeedMph - baselineMaxSpeedMph) * MAX_DECAY_FACTOR;
            maxSpeedMph = Math.max(baselineMaxSpeedMph, maxSpeedMph - decay);
        }
    }

    private int getSpeedColor(float speedPercent) {
        if (speedPercent < 0.3f) {
            // Green for low speed
            return 0xFF00FF00;
        } else if (speedPercent < 0.7f) {
            // Yellow for medium speed
            return 0xFFFFFF00;
        } else if (speedPercent < 0.9f) {
            // Orange for high speed
            return 0xFFFF8800;
        } else {
            // Red only for max speed (90%+)
            return 0xFFFF0000;
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

    private float resolveBaselineSpeedMph(DragonEntity dragon) {
        double bestMph = 0.0;

        AttributeInstance flying = dragon.getAttribute(Attributes.FLYING_SPEED);
        if (flying != null) {
            bestMph = Math.max(bestMph, flying.getValue() * BLOCKS_PER_TICK_TO_MPH);
        }

        AttributeInstance movement = dragon.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            bestMph = Math.max(bestMph, movement.getValue() * BLOCKS_PER_TICK_TO_MPH);
        }

        if (bestMph <= 0.0) {
            bestMph = MIN_BASELINE_MPH;
        }

        return (float) Math.max(MIN_BASELINE_MPH, bestMph);
    }
}

