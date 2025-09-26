package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.client.DragonRideKeybinds;
import com.leon.saintsdragons.common.registry.amphithere.AmphithereAbilities;
import com.leon.saintsdragons.common.registry.lightningdragon.LightningDragonAbilities;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Control guide UI element for dragons.
 * Shows current keybinds and their functions, updates dynamically.
 */
public class DragonControlGuide extends DragonUIElement {
    private static final int BASE_WIDTH = 150;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int KEY_BG_DARK = 0xFF333333;
    private static final int KEY_BG_LIGHT = 0xFF666666;
    private static final int COLOR_ATTACK = 0xFFFF5A5A;
    private static final int COLOR_MOVEMENT = 0xFF00FFFF;
    private static final int COLOR_ABILITY = 0xFFFFC845;

    private DragonEntity dragon;
    private final List<ControlEntry> controls = new ArrayList<>();

    public DragonControlGuide(int x, int y) {
        super(x, y, BASE_WIDTH, 80);
    }

    public void setDragon(DragonEntity dragon) {
        this.dragon = dragon;
        updateControls();
    }

    private void updateControls() {
        controls.clear();
        if (dragon == null) {
            this.height = 0;
            this.width = BASE_WIDTH;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Options options = mc.options;
        Font font = mc.font;

        controls.add(ControlEntry.forKey(options.keyAttack, Component.translatable("saintsdragons.ui.control.attack"), COLOR_ATTACK));
        controls.add(ControlEntry.forKey(DragonRideKeybinds.DRAGON_ASCEND, Component.translatable("saintsdragons.ui.control.ascend"), COLOR_MOVEMENT));
        controls.add(ControlEntry.forKey(DragonRideKeybinds.DRAGON_DESCEND, Component.translatable("saintsdragons.ui.control.descend"), COLOR_MOVEMENT));
        controls.add(ControlEntry.forKey(DragonRideKeybinds.DRAGON_ACCELERATE, Component.translatable("saintsdragons.ui.control.accelerate"), COLOR_MOVEMENT));

        DragonAbilityType<?, ?> primaryAbility = dragon.getRoarAbility();
        if (primaryAbility != null) {
            controls.add(ControlEntry.forKey(
                DragonRideKeybinds.DRAGON_PRIMARY_ABILITY,
                buildAbilityLabel("saintsdragons.ui.control.ability_primary", primaryAbility),
                COLOR_ABILITY
            ));
        }

        DragonAbilityType<?, ?> secondaryAbility = dragon.getSummonStormAbility();
        if (secondaryAbility != null) {
            controls.add(ControlEntry.forKey(
                DragonRideKeybinds.DRAGON_SECONDARY_ABILITY,
                buildAbilityLabel("saintsdragons.ui.control.ability_secondary", secondaryAbility),
                COLOR_ABILITY
            ));
        }

        DragonAbilityType<?, ?> tertiaryAbility = getTertiaryAbility(dragon);
        if (tertiaryAbility != null) {
            controls.add(ControlEntry.forKey(
                DragonRideKeybinds.DRAGON_TERTIARY_ABILITY,
                buildAbilityLabel("saintsdragons.ui.control.ability_tertiary", tertiaryAbility),
                COLOR_ABILITY
            ));
        }

        int lineSpacing = font.lineHeight + 2;
        int contentHeight = 14 + controls.size() * lineSpacing + 4;
        this.height = Math.max(40, contentHeight);

        int maxWidth = BASE_WIDTH;
        for (ControlEntry control : controls) {
            Component key = control.getKeyComponent();
            if (key.getString().isEmpty()) {
                continue;
            }
            int keyWidth = font.width(key) + 6;
            int functionWidth = font.width(control.functionText);
            maxWidth = Math.max(maxWidth, keyWidth + functionWidth + 14);
        }
        this.width = maxWidth;
    }

    private Component buildAbilityLabel(String slotTranslationKey, @Nullable DragonAbilityType<?, ?> abilityType) {
        Component slot = Component.translatable(slotTranslationKey);
        if (abilityType == null) {
            return slot;
        }
        Component abilityName = Component.translatable("saintsdragons.ability." + abilityType.getName());
        return Component.translatable("saintsdragons.ui.control.ability_with_name", slot, abilityName);
    }

    @Nullable
    private DragonAbilityType<?, ?> getTertiaryAbility(DragonEntity dragon) {
        if (dragon instanceof LightningDragonEntity) {
            return LightningDragonAbilities.LIGHTNING_BEAM;
        }
        if (dragon instanceof AmphithereEntity) {
            return AmphithereAbilities.FIRE_BODY;
        }
        return null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible || dragon == null || dragon.isDeadOrDying() || controls.isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        Component title = Component.translatable("saintsdragons.ui.control.title");
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, x + (width - titleWidth) / 2, y + 2, TITLE_COLOR);

        int startY = y + 14;
        int lineSpacing = font.lineHeight + 2;

        for (int i = 0; i < controls.size(); i++) {
            ControlEntry control = controls.get(i);
            Component keyComponent = control.getKeyComponent();
            if (keyComponent.getString().isEmpty()) {
                continue;
            }

            int keyWidth = font.width(keyComponent) + 6;
            int entryY = startY + i * lineSpacing;

            guiGraphics.fill(x + 2, entryY, x + 2 + keyWidth, entryY + font.lineHeight + 1, KEY_BG_DARK);
            guiGraphics.fill(x + 3, entryY + 1, x + 1 + keyWidth, entryY + font.lineHeight, KEY_BG_LIGHT);

            guiGraphics.drawString(font, keyComponent, x + 4, entryY + 1, 0xFFFFFFFF);
            guiGraphics.drawString(font, control.functionText, x + 2 + keyWidth + 4, entryY + 1, control.color);
        }

        if (isMouseOver(mouseX, mouseY)) {
            renderDragHandle(guiGraphics);
        }
    }

    private void renderDragHandle(GuiGraphics guiGraphics) {
        int handleSize = 4;
        int handleX = x + width - handleSize - 2;
        int handleY = y + 2;

        guiGraphics.fill(handleX, handleY, handleX + handleSize, handleY + handleSize, 0xFFFFFFFF);
        guiGraphics.fill(handleX + 1, handleY + 1, handleX + handleSize - 1, handleY + handleSize - 1, 0xFF000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

    private static class ControlEntry {
        private final KeyMapping keyMapping;
        private final Component functionText;
        private final int color;

        private ControlEntry(KeyMapping keyMapping, Component functionText, int color) {
            this.keyMapping = keyMapping;
            this.functionText = functionText;
            this.color = color;
        }

        static ControlEntry forKey(KeyMapping keyMapping, Component functionText, int color) {
            return new ControlEntry(keyMapping, functionText, color);
        }

        Component getKeyComponent() {
            return keyMapping != null ? keyMapping.getTranslatedKeyMessage() : Component.empty();
        }
    }
}
