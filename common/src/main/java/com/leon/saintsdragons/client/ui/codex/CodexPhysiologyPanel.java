package com.leon.saintsdragons.client.ui.codex;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CodexPhysiologyPanel {
    private final ResourceLocation healthIcon;
    private final ResourceLocation armorIcon;
    private final ResourceLocation genderIcon;
    private final ResourceLocation hungerIcon;
    private final ResourceLocation happinessIcon;
    private final ResourceLocation variantIcon;

    public CodexPhysiologyPanel(ResourceLocation healthIcon, ResourceLocation armorIcon, ResourceLocation genderIcon,
                                ResourceLocation hungerIcon, ResourceLocation happinessIcon, ResourceLocation variantIcon) {
        this.healthIcon = healthIcon;
        this.armorIcon = armorIcon;
        this.genderIcon = genderIcon;
        this.hungerIcon = hungerIcon;
        this.happinessIcon = happinessIcon;
        this.variantIcon = variantIcon;
    }

    public void draw(GuiGraphics guiGraphics, Font font, CodexTab activeTab, CodexDragonEntry selected,
                     int leftPos, int topPos, int mouseX, int mouseY, int ecologyPage,
                     CodexEcologyPanel ecologyPanel, CodexAllyPanel allyPanel,
                     java.util.List<String> allyList, int allyScrollOffset) {
        int left = CodexLayout.getDetailLeft(leftPos);
        int top = CodexLayout.getDetailTop(topPos);
        int right = CodexLayout.getDetailRight(leftPos);
        int bottom = CodexLayout.getDetailBottom(topPos);
        int contentX = leftPos + 232;
        int contentY = topPos - 2;

        if (activeTab == CodexTab.ALLY) {
            allyPanel.draw(guiGraphics, font, left, top, bottom, allyList, allyScrollOffset);
            return;
        }

        if (selected == null) {
            return;
        }

        int boxX = leftPos + CodexLayout.NAME_BOX_X;
        int boxY = topPos + CodexLayout.NAME_BOX_Y;
        int textWidth = font.width(selected.displayName());
        int textX = boxX + Math.max(0, (CodexLayout.NAME_BOX_WIDTH - textWidth) / 2);
        int textY = boxY + Math.max(0, (CodexLayout.NAME_BOX_HEIGHT - font.lineHeight) / 2);
        guiGraphics.drawString(font, selected.displayName(), textX, textY, CodexLayout.TEXT_COLOR, false);

        if (activeTab == CodexTab.PHYSIOLOGY) {
            drawHealthStat(guiGraphics, font, selected, leftPos, topPos);
            drawHungerStat(guiGraphics, font, selected, leftPos, topPos);
            drawHappinessStat(guiGraphics, font, selected, leftPos, topPos);
            drawVariantStat(guiGraphics, font, selected, leftPos, topPos);
            drawArmorStat(guiGraphics, font, selected, leftPos, topPos);
            drawGenderStat(guiGraphics, font, selected, leftPos, topPos);
            drawPositionStat(guiGraphics, font, selected, leftPos, topPos);
            drawBiomeStat(guiGraphics, font, selected, leftPos, topPos);
        } else if (activeTab == CodexTab.ECOLOGY) {
            ecologyPanel.draw(guiGraphics, font, selected, ecologyPage, contentX, contentY, mouseX, mouseY);
        } else {
            guiGraphics.drawString(font, activeTab.description(), contentX, contentY + 16, CodexLayout.TEXT_COLOR, false);
        }
    }

    private void drawHealthStat(GuiGraphics guiGraphics, Font font, CodexDragonEntry selected, int leftPos, int topPos) {
        int iconX = leftPos + CodexLayout.HEALTH_ICON_OFFSET_X;
        int iconY = topPos + CodexLayout.HEALTH_ICON_OFFSET_Y;
        guiGraphics.blit(healthIcon, iconX, iconY, 0, 0,
                CodexLayout.STAT_ICON_WIDTH, CodexLayout.STAT_ICON_HEIGHT,
                CodexLayout.STAT_ICON_WIDTH, CodexLayout.STAT_ICON_HEIGHT);
        String healthValue = formatStat(selected.currentHealth()) + "/" + formatStat(selected.maxHealth());
        guiGraphics.drawString(font, healthValue, iconX + CodexLayout.STAT_ICON_WIDTH + 2,
                iconY + CodexLayout.STAT_TEXT_OFFSET_Y, CodexLayout.TEXT_COLOR, false);
    }

    private void drawArmorStat(GuiGraphics guiGraphics, Font font, CodexDragonEntry selected, int leftPos, int topPos) {
        int iconX = leftPos + CodexLayout.HEALTH_ICON_OFFSET_X;
        int iconY = topPos + CodexLayout.HEALTH_ICON_OFFSET_Y + CodexLayout.STAT_ICON_HEIGHT + CodexLayout.STAT_ICON_GAP_Y;
        guiGraphics.blit(armorIcon, iconX, iconY, 0, 0,
                CodexLayout.STAT_ICON_WIDTH, CodexLayout.STAT_ICON_HEIGHT,
                CodexLayout.STAT_ICON_WIDTH, CodexLayout.STAT_ICON_HEIGHT);
        String armorValue = formatStat(selected.armor());
        guiGraphics.drawString(font, armorValue, iconX + CodexLayout.STAT_ICON_WIDTH + 2,
                iconY + CodexLayout.STAT_TEXT_OFFSET_Y, CodexLayout.TEXT_COLOR, false);
    }

    private void drawHungerStat(GuiGraphics guiGraphics, Font font, CodexDragonEntry selected, int leftPos, int topPos) {
        int iconX = leftPos + CodexLayout.HUNGER_ICON_OFFSET_X;
        int iconY = topPos + CodexLayout.HEALTH_ICON_OFFSET_Y;
        guiGraphics.blit(hungerIcon, iconX, iconY, 0, 0,
                CodexLayout.STAT_ICON_WIDTH, CodexLayout.STAT_ICON_HEIGHT,
                CodexLayout.STAT_ICON_WIDTH, CodexLayout.STAT_ICON_HEIGHT);
        String hungerValue = formatStat(selected.hunger()) + "/100";
        guiGraphics.drawString(font, hungerValue, iconX + CodexLayout.STAT_ICON_WIDTH + 2,
                iconY + CodexLayout.STAT_TEXT_OFFSET_Y, CodexLayout.TEXT_COLOR, false);
    }

    private void drawHappinessStat(GuiGraphics guiGraphics, Font font, CodexDragonEntry selected, int leftPos, int topPos) {
        int iconX = leftPos + CodexLayout.HAPPINESS_ICON_OFFSET_X;
        int iconY = topPos + CodexLayout.HEALTH_ICON_OFFSET_Y + CodexLayout.STAT_ICON_HEIGHT + CodexLayout.STAT_ICON_GAP_Y;
        guiGraphics.blit(happinessIcon, iconX, iconY, 0, 0,
                CodexLayout.STAT_ICON_WIDTH, CodexLayout.STAT_ICON_HEIGHT,
                CodexLayout.STAT_ICON_WIDTH, CodexLayout.STAT_ICON_HEIGHT);
        String happinessValue = formatStat(selected.happiness()) + "/100";
        guiGraphics.drawString(font, happinessValue, iconX + CodexLayout.STAT_ICON_WIDTH + 2,
                iconY + CodexLayout.STAT_TEXT_OFFSET_Y, CodexLayout.TEXT_COLOR, false);
    }

    private void drawVariantStat(GuiGraphics guiGraphics, Font font, CodexDragonEntry selected, int leftPos, int topPos) {
        int iconX = leftPos + CodexLayout.VARIANT_ICON_OFFSET_X;
        int iconY = topPos + CodexLayout.HEALTH_ICON_OFFSET_Y + (CodexLayout.STAT_ICON_HEIGHT + CodexLayout.STAT_ICON_GAP_Y) * 2;
        guiGraphics.blit(variantIcon, iconX, iconY, 0, 0,
                CodexLayout.STAT_ICON_WIDTH, CodexLayout.STAT_ICON_HEIGHT,
                CodexLayout.STAT_ICON_WIDTH, CodexLayout.STAT_ICON_HEIGHT);
        String key = resolveVariantTranslationKey(selected);
        String value = Component.translatable(key).getString();
        guiGraphics.drawString(font, value, iconX + CodexLayout.STAT_ICON_WIDTH + 2,
                iconY + CodexLayout.STAT_TEXT_OFFSET_Y, CodexLayout.TEXT_COLOR, false);
    }

    private String resolveVariantTranslationKey(CodexDragonEntry selected) {
        if (selected.variantId() <= 0) {
            return "saintsdragons.variant.default";
        }
        return switch (selected.dragonType()) {
            case "ignivorus" -> "saintsdragons.variant.crimson";
            case "cindervane" -> "saintsdragons.variant.albino";
            case "raevyx" -> "saintsdragons.variant.night_gold";
            case "volitans" -> "saintsdragons.variant.bloodshot";
            default -> "saintsdragons.variant.default";
        };
    }

    private void drawGenderStat(GuiGraphics guiGraphics, Font font, CodexDragonEntry selected, int leftPos, int topPos) {
        int iconX = leftPos + CodexLayout.HEALTH_ICON_OFFSET_X;
        int iconY = topPos + CodexLayout.HEALTH_ICON_OFFSET_Y + (CodexLayout.STAT_ICON_HEIGHT + CodexLayout.STAT_ICON_GAP_Y) * 2;
        guiGraphics.blit(genderIcon, iconX, iconY, 0, 0,
                CodexLayout.STAT_ICON_WIDTH, CodexLayout.STAT_ICON_HEIGHT,
                CodexLayout.STAT_ICON_WIDTH, CodexLayout.STAT_ICON_HEIGHT);
        String genderKey = selected.genderKnown()
                ? (selected.genderId() == 1 ? "saintsdragons.gender.female" : "saintsdragons.gender.male")
                : "saintsdragons.gui.draconic_codex.physiology.gender_unknown";
        String genderValue = Component.translatable(genderKey).getString();
        guiGraphics.drawString(font, genderValue, iconX + CodexLayout.STAT_ICON_WIDTH + 2,
                iconY + CodexLayout.STAT_TEXT_OFFSET_Y, CodexLayout.TEXT_COLOR, false);
    }

    private String formatStat(double value) {
        if (Math.abs(value - Math.round(value)) < 0.01) {
            return String.format("%.0f", value);
        }
        return String.format("%.1f", value);
    }

    private void drawPositionStat(GuiGraphics guiGraphics, Font font, CodexDragonEntry selected, int leftPos, int topPos) {
        int textX = leftPos + 231;
        int textY = topPos + 15;
        Component line = Component.translatable("saintsdragons.gui.draconic_codex.physiology.position",
                formatCoordinate(selected.posX()),
                formatCoordinate(selected.posY()),
                formatCoordinate(selected.posZ()));
        drawWrappedLine(guiGraphics, font, line, textX, textY, 90, 5);
    }

    private void drawBiomeStat(GuiGraphics guiGraphics, Font font, CodexDragonEntry selected, int leftPos, int topPos) {
        int textX = leftPos + 231;
        int textY = topPos + 50;
        Component line = Component.translatable("saintsdragons.gui.draconic_codex.physiology.biome", selected.biomeId());
        drawWrappedLine(guiGraphics, font, line, textX, textY, 140, 10);
    }

    private String formatCoordinate(double value) {
        return String.format("%.1f", value);
    }

    private void drawWrappedLine(GuiGraphics guiGraphics, Font font, Component text,
                                 int x, int y, int width, int maxLines) {
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, width);
        int count = Math.min(lines.size(), maxLines);
        for (int i = 0; i < count; i++) {
            guiGraphics.drawString(font, lines.get(i), x, y + (i * font.lineHeight), CodexLayout.TEXT_COLOR, false);
        }
    }
}