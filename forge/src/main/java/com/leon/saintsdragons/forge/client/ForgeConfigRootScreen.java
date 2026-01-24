package com.leon.saintsdragons.forge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ForgeConfigRootScreen extends Screen {
    private final Screen parent;

    public ForgeConfigRootScreen(Screen parent) {
        super(Component.translatable("saintsdragons.config_screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int x = (width - buttonWidth) / 2;
        int y = height / 2 - 36;

        addRenderableWidget(Button.builder(Component.translatable("saintsdragons.config_screen.attributes"), button -> {
            minecraft.setScreen(new ForgeDragonAttributesScreen(this));
        }).bounds(x, y, buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(Component.translatable("saintsdragons.config_screen.spawning"), button -> {
            minecraft.setScreen(new ForgeDragonSpawningScreen(this));
        }).bounds(x, y + 24, buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(Component.translatable("saintsdragons.config_screen.others"), button -> {
            minecraft.setScreen(new ForgeOthersScreen(this));
        }).bounds(x, y + 48, buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> minecraft.setScreen(parent))
                .bounds(x, y + 76, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
