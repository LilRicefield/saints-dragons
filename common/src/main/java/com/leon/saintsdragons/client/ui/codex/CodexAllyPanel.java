package com.leon.saintsdragons.client.ui.codex;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

public class CodexAllyPanel {
    private final ResourceLocation editBoxTexture;
    private final ResourceLocation addIcon;
    private final ResourceLocation removeIcon;
    private CustomEditBox allyInput;
    private ImageButton addAllyButton;
    private ImageButton removeAllyButton;

    public CodexAllyPanel(ResourceLocation editBoxTexture, ResourceLocation addIcon, ResourceLocation removeIcon) {
        this.editBoxTexture = editBoxTexture;
        this.addIcon = addIcon;
        this.removeIcon = removeIcon;
    }

    public void initWidgets(Consumer<net.minecraft.client.gui.components.AbstractWidget> addWidget, Font font,
                            int leftPos, int topPos, Consumer<String> addCallback, Consumer<String> removeCallback) {
        int inputX = leftPos + 233;
        int inputY = topPos + 17;
        int inputWidth = 122;
        int inputHeight = 12;

        allyInput = new CustomEditBox(
                font,
                inputX,
                inputY,
                inputWidth,
                inputHeight,
                Component.translatable("saintsdragons.gui.draconic_codex.ally.input")
        );
        allyInput.setMaxLength(16);
        allyInput.setBordered(false);
        allyInput.setTextColor(CodexLayout.TEXT_COLOR);
        allyInput.setTextColorUneditable(CodexLayout.TEXT_COLOR);
        addWidget.accept(allyInput);

        int iconButtonX = inputX + inputWidth + 8;

        addAllyButton = new ImageButton(
                iconButtonX - 36, inputY + 18, 14, 14,
                0, 0, 0,
                addIcon,
                14, 14,
                button -> addAllyFromInput(addCallback)
        );
        addAllyButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("saintsdragons.gui.draconic_codex.ally.add")));
        addWidget.accept(addAllyButton);

        removeAllyButton = new ImageButton(
                iconButtonX - 19, inputY + 18, 14, 14,
                0, 0, 0,
                removeIcon,
                14, 14,
                button -> removeAllyFromInput(removeCallback)
        );
        removeAllyButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("saintsdragons.gui.draconic_codex.ally.remove")));
        addWidget.accept(removeAllyButton);
    }

    public void updateVisibility(boolean show) {
        if (allyInput != null) {
            allyInput.setVisible(show);
            allyInput.setEditable(show);
        }
        if (addAllyButton != null) {
            addAllyButton.visible = show;
            addAllyButton.active = show;
        }
        if (removeAllyButton != null) {
            removeAllyButton.visible = show;
            removeAllyButton.active = show;
        }
    }

    public void draw(GuiGraphics guiGraphics, Font font, int left, int top, int bottom,
                     List<String> allyList, int allyScrollOffset) {
        if (allyInput != null) {
            guiGraphics.blit(editBoxTexture,
                    allyInput.getX() - 1,
                    allyInput.getY() - 2,
                    0, 0,
                    120, 16,
                    120, 16);
        }

        int contentX = left + 81;
        int contentY = top - 5;

        String allyCountText = Component.translatable(
                "saintsdragons.gui.draconic_codex.ally.count",
                allyList.size(),
                com.leon.saintsdragons.server.entity.handler.DragonAllyManager.getMaxAlliesStatic()
        ).getString();
        guiGraphics.drawString(font, allyCountText, contentX, contentY, CodexLayout.TEXT_COLOR, false);

        int listTop = contentY + 16;
        int listBottom = bottom - 16;
        int visibleCount = Math.min(CodexLayout.MAX_VISIBLE_ALLIES, allyList.size() - allyScrollOffset);
        if (visibleCount < 0) {
            visibleCount = 0;
        }

        for (int i = 0; i < visibleCount; i++) {
            int index = i + allyScrollOffset;
            if (index >= allyList.size()) {
                break;
            }
            int y = listTop + (i * 12);
            guiGraphics.drawString(font, allyList.get(index), contentX, y, CodexLayout.TEXT_COLOR, false);
        }

        if (allyList.isEmpty()) {
            guiGraphics.drawString(font,
                    Component.translatable("saintsdragons.gui.draconic_codex.ally.empty").getString(),
                    contentX, listTop, CodexLayout.TEXT_COLOR, false);
        }
    }

    private void addAllyFromInput(Consumer<String> addCallback) {
        if (allyInput == null) {
            return;
        }
        String username = allyInput.getValue().trim();
        if (username.isEmpty()) {
            return;
        }
        addCallback.accept(username);
        allyInput.setValue("");
    }

    private void removeAllyFromInput(Consumer<String> removeCallback) {
        if (allyInput == null) {
            return;
        }
        String username = allyInput.getValue().trim();
        if (username.isEmpty()) {
            return;
        }
        removeCallback.accept(username);
        allyInput.setValue("");
    }
}
