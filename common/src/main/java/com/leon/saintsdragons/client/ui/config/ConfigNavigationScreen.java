package com.leon.saintsdragons.client.ui.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Shared config navigation used by Forge and Fabric so both loaders expose the
 * same page hierarchy even though their actual config editors are different.
 */
public class ConfigNavigationScreen extends Screen {
    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;

    private final Screen parent;
    private final List<Destination> destinations;

    public ConfigNavigationScreen(Screen parent, Component title, List<Destination> destinations) {
        super(title);
        this.parent = parent;
        this.destinations = List.copyOf(destinations);
    }

    @Override
    protected void init() {
        int x = (width - BUTTON_WIDTH) / 2;
        int contentHeight = destinations.size() * BUTTON_SPACING + 28;
        int y = Math.max(44, (height - contentHeight) / 2);

        for (int index = 0; index < destinations.size(); index++) {
            Destination destination = destinations.get(index);
            Button button = addRenderableWidget(Button.builder(destination.label(), ignored -> {
                if (minecraft != null) {
                    minecraft.setScreen(destination.screenFactory().apply(this));
                }
            }).bounds(x, y + index * BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build());
            button.active = destination.available().getAsBoolean();
            if (!button.active && destination.unavailableReason() != null) {
                button.setTooltip(Tooltip.create(destination.unavailableReason()));
            }
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, ignored -> {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }).bounds(x, y + destinations.size() * BUTTON_SPACING + 4, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    public record Destination(Component label,
                              Function<Screen, Screen> screenFactory,
                              BooleanSupplier available,
                              Component unavailableReason) {
        public Destination(Component label, Function<Screen, Screen> screenFactory) {
            this(label, screenFactory, () -> true, null);
        }
    }
}
