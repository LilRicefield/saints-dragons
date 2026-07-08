package com.leon.saintsdragons.forge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public abstract class ForgePagedConfigScreen extends Screen {
    protected final Screen parent;
    protected final List<ConfigEntry> entries = new ArrayList<>();

    private Button saveButton;
    private Button backButton;
    private int scrollOffset;
    private int contentHeight;
    private int panelTop;
    private int panelBottom;
    private int panelLeft;
    private int panelRight;

    protected ForgePagedConfigScreen(Screen parent, Component title) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    protected abstract void buildEntries(List<ConfigEntry> entries);

    protected abstract void onSave();

    protected void addHeaderButtons() {
        // Optional header buttons for subclasses.
    }

    protected int getPanelTop() {
        return 56;
    }

    protected void rebuildWidgets() {
        storeAllValues();
        clearWidgets();

        entries.clear();
        buildEntries(entries);
        scrollOffset = 0;

        panelTop = getPanelTop();
        panelBottom = height - 44;
        panelLeft = 12;
        panelRight = width - 12;

        addHeaderButtons();

        int buttonY = height - 28;
        saveButton = addRenderableWidget(Button.builder(Component.translatable("saintsdragons.config_screen.save"), button -> {
            storeAllValues();
            for (ConfigEntry entry : entries) {
                entry.applyValue();
            }
            onSave();
        }).bounds(width / 2 - 70, buttonY, 60, 20).build());

        backButton = addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> minecraft.setScreen(parent))
                .bounds(width / 2 + 10, buttonY, 60, 20)
                .build());

        int labelX = 20;
        int inputX = width / 2 + 20;
        int inputWidth = Math.max(80, width - inputX - 20);

        for (ConfigEntry entry : entries) {
            entry.attach(this, labelX, inputX, 0, inputWidth);
        }

        contentHeight = entries.stream().mapToInt(ConfigEntry::getHeight).sum();
    }

    private void storeAllValues() {
        for (ConfigEntry entry : entries) {
            entry.storeValue();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= panelTop && mouseY <= panelBottom) {
            int maxScroll = Math.max(0, contentHeight - (panelBottom - panelTop));
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 12));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        setEntryWidgetsVisible(false);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderEntries(graphics, mouseX, mouseY, partialTick);
    }

    private void renderEntries(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        layoutEntries();
        double scale = minecraft.getWindow().getGuiScale();
        int scissorLeft = (int) (panelLeft * scale);
        int scissorTop = (int) (minecraft.getWindow().getHeight() - panelBottom * scale);
        int scissorWidth = (int) ((panelRight - panelLeft) * scale);
        int scissorHeight = (int) ((panelBottom - panelTop) * scale);

        RenderSystem.enableScissor(scissorLeft, scissorTop, scissorWidth, scissorHeight);

        for (ConfigEntry entry : entries) {
            if (entry.isVisibleInPanel()) {
                entry.renderLabel(graphics, font);
                entry.renderWidget(graphics, mouseX, mouseY, partialTick);
            }
        }

        RenderSystem.disableScissor();
    }

    private void setEntryWidgetsVisible(boolean visible) {
        for (ConfigEntry entry : entries) {
            entry.setVisible(visible);
        }
    }

    private void layoutEntries() {
        int y = panelTop - scrollOffset;
        for (ConfigEntry entry : entries) {
            entry.updatePosition(y);
            boolean visible = y + entry.getHeight() > panelTop && y < panelBottom;
            entry.setVisible(visible);
            entry.setVisibleInPanel(visible);
            y += entry.getHeight();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layoutEntries();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected abstract static class ConfigEntry {
        protected final Component label;
        protected int labelX;
        protected int inputX;
        protected int y;
        protected int inputWidth;
        protected boolean visibleInPanel;

        protected ConfigEntry(Component label) {
            this.label = label;
        }

        protected void attach(ForgePagedConfigScreen screen, int labelX, int inputX, int y, int inputWidth) {
            this.labelX = labelX;
            this.inputX = inputX;
            this.y = y;
            this.inputWidth = inputWidth;
            addWidgets(screen);
        }

        protected void updatePosition(int y) {
            this.y = y;
            updateWidgetPositions();
        }

        protected abstract void addWidgets(ForgePagedConfigScreen screen);

        protected abstract void updateWidgetPositions();

        protected abstract void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

        protected abstract void setVisible(boolean visible);

        protected void setVisibleInPanel(boolean visible) {
            this.visibleInPanel = visible;
        }

        protected boolean isVisibleInPanel() {
            return visibleInPanel;
        }

        protected abstract void storeValue();

        protected abstract void applyValue();

        protected abstract int getHeight();

        protected void renderLabel(GuiGraphics graphics, net.minecraft.client.gui.Font font) {
            graphics.drawString(font, label, labelX, y + 6, 0xE0E0E0);
        }
    }

    protected static final class SectionEntry extends ConfigEntry {
        protected SectionEntry(Component label) {
            super(label);
        }

        @Override
        protected void addWidgets(ForgePagedConfigScreen screen) {
            // No widgets for section headers.
        }

        @Override
        protected void updateWidgetPositions() {
            // No widgets to position.
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // No widgets to render.
        }

        @Override
        protected void setVisible(boolean visible) {
            // No widgets to show/hide.
        }

        @Override
        protected void storeValue() {
            // No-op.
        }

        @Override
        protected void applyValue() {
            // No-op.
        }

        @Override
        protected int getHeight() {
            return 20;
        }

        @Override
        protected void renderLabel(GuiGraphics graphics, net.minecraft.client.gui.Font font) {
            graphics.drawString(font, label, labelX, y + 2, 0xFFFFFF);
        }
    }

    protected static final class DoubleEntry extends ConfigEntry {
        private final DoubleSupplier getter;
        private final DoubleConsumer setter;
        private final Runnable saver;
        private String value;
        private EditBox editBox;

        protected DoubleEntry(Component label, DoubleSupplier getter, DoubleConsumer setter, Runnable saver) {
            super(label);
            this.getter = getter;
            this.setter = setter;
            this.saver = saver;
            this.value = formatDouble(getter.getAsDouble());
        }

        @Override
        protected void addWidgets(ForgePagedConfigScreen screen) {
            editBox = new EditBox(screen.font, inputX, y, inputWidth, 18, label);
            editBox.setValue(value);
            editBox.setFilter(text -> text.isEmpty() || text.matches("-?\\d*(\\.\\d*)?"));
            screen.addRenderableWidget(editBox);
        }

        @Override
        protected void updateWidgetPositions() {
            editBox.setX(inputX);
            editBox.setY(y);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            editBox.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void setVisible(boolean visible) {
            editBox.setVisible(visible);
            editBox.active = visible;
        }

        @Override
        protected void storeValue() {
            if (editBox != null) {
                value = editBox.getValue();
            }
        }

        @Override
        protected void applyValue() {
            if (value == null || value.isBlank()) {
                return;
            }
            try {
                double parsed = Double.parseDouble(value);
                setter.accept(parsed);
                if (saver != null) {
                    saver.run();
                }
            } catch (NumberFormatException ignored) {
                // Keep previous value if parsing fails.
            }
        }

        @Override
        protected int getHeight() {
            return 24;
        }

        private static String formatDouble(double value) {
            if (value == (long) value) {
                return String.format(Locale.ROOT, "%d", (long) value);
            }
            return String.format(Locale.ROOT, "%.4f", value);
        }
    }

    protected static final class PercentDoubleEntry extends ConfigEntry {
        private final DoubleSupplier getter;
        private final DoubleConsumer setter;
        private final Runnable saver;
        private String value;
        private EditBox editBox;

        protected PercentDoubleEntry(Component label, DoubleSupplier getter, DoubleConsumer setter, Runnable saver) {
            super(label);
            this.getter = getter;
            this.setter = setter;
            this.saver = saver;
            this.value = formatDouble(clampChance(getter.getAsDouble()) * 100.0D);
        }

        @Override
        protected void addWidgets(ForgePagedConfigScreen screen) {
            editBox = new EditBox(screen.font, inputX, y, inputWidth, 18, label);
            editBox.setValue(value);
            editBox.setFilter(text -> text.isEmpty() || text.matches("-?\\d*(\\.\\d*)?"));
            screen.addRenderableWidget(editBox);
        }

        @Override
        protected void updateWidgetPositions() {
            editBox.setX(inputX);
            editBox.setY(y);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            editBox.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void setVisible(boolean visible) {
            editBox.setVisible(visible);
            editBox.active = visible;
        }

        @Override
        protected void storeValue() {
            if (editBox != null) {
                value = editBox.getValue();
            }
        }

        @Override
        protected void applyValue() {
            if (value == null || value.isBlank()) {
                return;
            }
            try {
                double parsedPercent = Double.parseDouble(value);
                setter.accept(clampChance(parsedPercent / 100.0D));
                if (saver != null) {
                    saver.run();
                }
            } catch (NumberFormatException ignored) {
                // Keep previous value if parsing fails.
            }
        }

        @Override
        protected int getHeight() {
            return 24;
        }

        private static double clampChance(double value) {
            return Math.max(0.0D, Math.min(1.0D, value));
        }

        private static String formatDouble(double value) {
            if (value == (long) value) {
                return String.format(Locale.ROOT, "%d", (long) value);
            }
            return String.format(Locale.ROOT, "%.4f", value);
        }
    }

    protected static final class IntEntry extends ConfigEntry {
        private final IntSupplier getter;
        private final IntConsumer setter;
        private final Runnable saver;
        private String value;
        private EditBox editBox;

        protected IntEntry(Component label, IntSupplier getter, IntConsumer setter, Runnable saver) {
            super(label);
            this.getter = getter;
            this.setter = setter;
            this.saver = saver;
            this.value = Integer.toString(getter.getAsInt());
        }

        @Override
        protected void addWidgets(ForgePagedConfigScreen screen) {
            editBox = new EditBox(screen.font, inputX, y, inputWidth, 18, label);
            editBox.setValue(value);
            editBox.setFilter(text -> text.isEmpty() || text.matches("-?\\d*"));
            screen.addRenderableWidget(editBox);
        }

        @Override
        protected void updateWidgetPositions() {
            editBox.setX(inputX);
            editBox.setY(y);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            editBox.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void setVisible(boolean visible) {
            editBox.setVisible(visible);
            editBox.active = visible;
        }

        @Override
        protected void storeValue() {
            if (editBox != null) {
                value = editBox.getValue();
            }
        }

        @Override
        protected void applyValue() {
            if (value == null || value.isBlank()) {
                return;
            }
            try {
                int parsed = Integer.parseInt(value);
                setter.accept(parsed);
                if (saver != null) {
                    saver.run();
                }
            } catch (NumberFormatException ignored) {
                // Keep previous value if parsing fails.
            }
        }

        @Override
        protected int getHeight() {
            return 24;
        }
    }

    protected static final class BooleanEntry extends ConfigEntry {
        private final BooleanSupplier getter;
        private final Consumer<Boolean> setter;
        private final Runnable saver;
        private boolean value;
        private CycleButton<Boolean> button;

        protected BooleanEntry(Component label, BooleanSupplier getter, Consumer<Boolean> setter, Runnable saver) {
            super(label);
            this.getter = getter;
            this.setter = setter;
            this.saver = saver;
            this.value = getter.getAsBoolean();
        }

        @Override
        protected void addWidgets(ForgePagedConfigScreen screen) {
            button = CycleButton.onOffBuilder(value).create(inputX, y, inputWidth, 18, label);
            screen.addRenderableWidget(button);
        }

        @Override
        protected void updateWidgetPositions() {
            button.setX(inputX);
            button.setY(y);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            button.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void setVisible(boolean visible) {
            button.visible = visible;
            button.active = visible;
        }

        @Override
        protected void storeValue() {
            if (button != null) {
                value = button.getValue();
            }
        }

        @Override
        protected void applyValue() {
            setter.accept(value);
            if (saver != null) {
                saver.run();
            }
        }

        @Override
        protected int getHeight() {
            return 24;
        }
    }

    protected static final class IntSliderEntry extends ConfigEntry {
        private final IntSupplier getter;
        private final IntConsumer setter;
        private final Runnable saver;
        private final int min;
        private final int max;
        private final int defaultValue;
        private final java.util.function.IntFunction<Component> textGetter;
        private int value;
        private AbstractSliderButton slider;

        protected IntSliderEntry(Component label, IntSupplier getter, IntConsumer setter, Runnable saver,
                                 int min, int max, int defaultValue,
                                 java.util.function.IntFunction<Component> textGetter) {
            super(label);
            this.getter = getter;
            this.setter = setter;
            this.saver = saver;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
            this.textGetter = textGetter;
            this.value = clamp(getter.getAsInt());
        }

        @Override
        protected void addWidgets(ForgePagedConfigScreen screen) {
            slider = new AbstractSliderButton(inputX, y, inputWidth, 18, messageFor(value), normalized(value)) {
                @Override
                protected void updateMessage() {
                    setMessage(messageFor(valueFromSlider(this.value)));
                }

                @Override
                protected void applyValue() {
                    IntSliderEntry.this.value = valueFromSlider(this.value);
                }
            };
            screen.addRenderableWidget(slider);
        }

        @Override
        protected void updateWidgetPositions() {
            slider.setX(inputX);
            slider.setY(y);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            slider.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void setVisible(boolean visible) {
            slider.visible = visible;
            slider.active = visible;
        }

        @Override
        protected void storeValue() {
            if (slider != null) {
                value = clamp(value);
            }
        }

        @Override
        protected void applyValue() {
            setter.accept(value);
            if (saver != null) {
                saver.run();
            }
        }

        @Override
        protected int getHeight() {
            return 24;
        }

        private Component messageFor(int value) {
            return textGetter == null ? Component.literal(Integer.toString(value)) : textGetter.apply(value);
        }

        private double normalized(int value) {
            if (max <= min) {
                return 0.0D;
            }
            return (clamp(value) - min) / (double) (max - min);
        }

        private int valueFromSlider(double sliderValue) {
            if (max <= min) {
                return defaultValue;
            }
            int parsed = min + (int) Math.round(sliderValue * (max - min));
            return clamp(parsed);
        }

        private int clamp(int value) {
            return Math.max(min, Math.min(max, value));
        }
    }

    protected static final class ListEntry extends ConfigEntry {
        private final Supplier<List<String>> getter;
        private final Consumer<List<String>> setter;
        private final Runnable saver;
        private String value;
        private EditBox editBox;

        protected ListEntry(Component label, Supplier<List<String>> getter, Consumer<List<String>> setter, Runnable saver) {
            super(label);
            this.getter = getter;
            this.setter = setter;
            this.saver = saver;
            this.value = String.join(", ", getter.get());
        }

        @Override
        protected void addWidgets(ForgePagedConfigScreen screen) {
            editBox = new EditBox(screen.font, inputX, y, inputWidth, 18, label);
            // Additional biome lists can be long; don't clip at default textbox length.
            editBox.setMaxLength(2048);
            editBox.setValue(value);
            screen.addRenderableWidget(editBox);
        }

        @Override
        protected void updateWidgetPositions() {
            editBox.setX(inputX);
            editBox.setY(y);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            editBox.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void setVisible(boolean visible) {
            editBox.setVisible(visible);
            editBox.active = visible;
        }

        @Override
        protected void storeValue() {
            if (editBox != null) {
                value = editBox.getValue();
            }
        }

        @Override
        protected void applyValue() {
            if (value == null) {
                return;
            }
            List<String> parsed = new ArrayList<>();
            for (String entry : value.split(",")) {
                String trimmed = entry.trim();
                if (!trimmed.isEmpty()) {
                    parsed.add(trimmed);
                }
            }
            setter.accept(parsed);
            if (saver != null) {
                saver.run();
            }
        }

        @Override
        protected int getHeight() {
            return 24;
        }
    }
}
