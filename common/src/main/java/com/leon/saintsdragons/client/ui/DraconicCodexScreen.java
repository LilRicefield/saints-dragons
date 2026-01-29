package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.network.MessageDraconicCodexRequest;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.ModEntities;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public class DraconicCodexScreen extends Screen {
    private static final int GUI_WIDTH = 393;
    private static final int GUI_HEIGHT = 214;
    private static final int TAB_HEIGHT = 22;
    private static final int TAB_WIDTH = 26;
    private static final int TAB_CLOSED_WIDTH = 10;
    private static final int TAB_CLOSED_HEIGHT = 22;
    private static final int MAX_VISIBLE_DRAGONS = 10;
    private static final int LIST_WIDTH = 120;
    private static final int TEXT_COLOR = 0x5B3A12;
    private static final int STAT_ICON_WIDTH = 8;
    private static final int STAT_ICON_HEIGHT = 9;
    private static final int HEALTH_ICON_OFFSET_X = 96;
    private static final int HEALTH_ICON_OFFSET_Y = 147;
    private static final int HUNGER_ICON_OFFSET_X = HEALTH_ICON_OFFSET_X + 67;
    private static final int HAPPINESS_ICON_OFFSET_X = HUNGER_ICON_OFFSET_X;
    private static final int VARIANT_ICON_OFFSET_X = HUNGER_ICON_OFFSET_X;
    private static final int STAT_ICON_GAP_Y = 5;
    private static final int STAT_TEXT_OFFSET_Y = 1;
    private static final int NAME_BOX_X = 110;
    private static final int NAME_BOX_Y = 32;
    private static final int NAME_BOX_WIDTH = 88;
    private static final int NAME_BOX_HEIGHT = 14;
    private static final int DRAGON_RENDER_BOX_X = 112;
    private static final int DRAGON_RENDER_BOX_Y = 56;
    private static final int DRAGON_RENDER_BOX_SIZE = 87;

    private static final ResourceLocation BOOK_TEXTURE =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/draconic_codex.png");
    private static final ResourceLocation TAB_PHYSIOLOGY =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/physiology_tab.png");
    private static final ResourceLocation TAB_ECOLOGY =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/ecology_tab.png");
    private static final ResourceLocation TAB_ALLY =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/ally_tab.png");
    private static final ResourceLocation TAB_PHYSIOLOGY_CLOSED =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/physiology_tab_closed.png");
    private static final ResourceLocation TAB_ECOLOGY_CLOSED =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/ecology_tab_closed.png");
    private static final ResourceLocation TAB_ALLY_CLOSED =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/ally_tab_closed.png");
    private static final ResourceLocation HEALTH_ICON =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/icons/health_icon.png");
    private static final ResourceLocation ARMOR_ICON =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/icons/armor_icon.png");
    private static final ResourceLocation GENDER_ICON =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/icons/gender_icon.png");
    private static final ResourceLocation HUNGER_ICON =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/icons/hunger_icon.png");
    private static final ResourceLocation HAPPINESS_ICON =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/icons/happiness_icon.png");
    private static final ResourceLocation VARIANT_ICON =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/icons/variant_icon.png");
    private static final ResourceLocation IGNIVORUS_PORTRAIT =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/ignivorus_portrait.png");

    private int leftPos;
    private int topPos;
    private int listScrollOffset = 0;
    private CodexTab activeTab = CodexTab.PHYSIOLOGY;
    private final List<DragonEntry> dragonEntries = new ArrayList<>();
    private java.util.UUID selectedDragonId;
    private java.util.UUID pendingSelectionId;
    private List<String> allyList = new ArrayList<>();
    private int allyScrollOffset = 0;
    private static final int MAX_VISIBLE_ALLIES = 8;

    private net.minecraft.client.gui.components.EditBox allyInput;
    private net.minecraft.client.gui.components.Button addAllyButton;
    private net.minecraft.client.gui.components.Button removeAllyButton;

    public DraconicCodexScreen() {
        this(null);
    }

    public DraconicCodexScreen(@Nullable java.util.UUID preselectedDragonId) {
        super(Component.translatable("saintsdragons.gui.draconic_codex.title"));
        this.pendingSelectionId = preselectedDragonId;
    }

    @Override
    protected void init() {
        super.init();
        int actualWidth = GUI_WIDTH;
        int actualHeight = GUI_HEIGHT;

        this.leftPos = Math.max(0, (this.width - actualWidth) / 2);
        this.topPos = Math.max(0, (this.height - actualHeight) / 2);

        // Request current tamed dragon list from server
        NetworkHandler.sendToServer(new MessageDraconicCodexRequest());
        NetworkHandler.sendToServer(new com.leon.saintsdragons.common.network.MessageGlobalAllyRequest());

        initAllyWidgets();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        guiGraphics.blit(BOOK_TEXTURE, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        int listLeft = getListLeft();
        int listTop = getListTop();
        int listRight = listLeft + LIST_WIDTH;
        int listBottom = getListBottom();

        drawDragonList(guiGraphics, listLeft, listTop, listRight, listBottom, mouseX, mouseY);
        drawTabs(guiGraphics);
        drawDetailPanel(guiGraphics, getDetailLeft(), getDetailTop(), getDetailRight(), getDetailBottom());
        drawDragonPortrait(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawTabs(GuiGraphics guiGraphics) {
        drawTab(guiGraphics, CodexTab.PHYSIOLOGY, TAB_PHYSIOLOGY, TAB_PHYSIOLOGY_CLOSED, 0);
        drawTab(guiGraphics, CodexTab.ECOLOGY, TAB_ECOLOGY, TAB_ECOLOGY_CLOSED, 1);
        drawTab(guiGraphics, CodexTab.ALLY, TAB_ALLY, TAB_ALLY_CLOSED, 2);
    }

    private void drawTab(GuiGraphics guiGraphics, CodexTab tab, ResourceLocation activeTexture, ResourceLocation inactiveTexture, int index) {
        boolean isActive = tab == activeTab;
        int x = getActiveTabX();
        int y = getTabY(index);
        if (isActive) {
            guiGraphics.blit(activeTexture, x, y, 0, 0, TAB_WIDTH, TAB_HEIGHT, TAB_WIDTH, TAB_HEIGHT);
        } else {
            guiGraphics.blit(inactiveTexture, x, y, 0, 0, TAB_CLOSED_WIDTH, TAB_CLOSED_HEIGHT, TAB_CLOSED_WIDTH, TAB_CLOSED_HEIGHT);
        }
    }

    private void drawDragonList(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int mouseX, int mouseY) {
        int visibleCount = Math.min(MAX_VISIBLE_DRAGONS, dragonEntries.size() - listScrollOffset);
        if (visibleCount < 0) {
            visibleCount = 0;
        }

        for (int i = 0; i < visibleCount; i++) {
            int index = i + listScrollOffset;
            if (index >= dragonEntries.size()) {
                break;
            }
            DragonEntry entry = dragonEntries.get(index);
            int y = top + (i * 12);
            boolean isSelected = entry.entityId != null && entry.entityId.equals(selectedDragonId);
            int nameColor = isSelected ? 0xFF8B0000 : TEXT_COLOR;
            guiGraphics.drawString(this.font, entry.displayName, left, y, nameColor, false);
        }

        if (dragonEntries.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("saintsdragons.gui.draconic_codex.empty").getString(), left, top, TEXT_COLOR, false);
        }

        if (dragonEntries.size() > MAX_VISIBLE_DRAGONS) {
            if (listScrollOffset > 0) {
                guiGraphics.drawString(this.font, "↑", right - 8, top - 12, TEXT_COLOR, false);
            }
            if (listScrollOffset + MAX_VISIBLE_DRAGONS < dragonEntries.size()) {
                guiGraphics.drawString(this.font, "↓", right - 8, bottom + 2, TEXT_COLOR, false);
            }
        }
    }

    private void drawDetailPanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        int contentX = leftPos + 90;
        int contentY = topPos + 60;

        if (activeTab == CodexTab.ALLY) {
            drawAllyPanel(guiGraphics, left, top, right, bottom);
            return;
        }

        DragonEntry selected = getSelectedEntry();
        if (selected == null) {
            return;
        }

        int boxX = leftPos + NAME_BOX_X;
        int boxY = topPos + NAME_BOX_Y;
        int textWidth = this.font.width(selected.displayName);
        int textX = boxX + Math.max(0, (NAME_BOX_WIDTH - textWidth) / 2);
        int textY = boxY + Math.max(0, (NAME_BOX_HEIGHT - this.font.lineHeight) / 2);
        guiGraphics.drawString(this.font, selected.displayName, textX, textY, TEXT_COLOR, false);
        if (activeTab == CodexTab.PHYSIOLOGY) {
            String armorText = Component.translatable(
                    "saintsdragons.gui.draconic_codex.physiology.armor",
                    formatStat(selected.armor)
            ).getString();
            String genderKey = selected.genderKnown
                    ? (selected.genderId == 1 ? "saintsdragons.gender.female" : "saintsdragons.gender.male")
                    : "saintsdragons.gui.draconic_codex.physiology.gender_unknown";
            String genderValue = Component.translatable(genderKey).getString();
            String genderText = Component.translatable(
                    "saintsdragons.gui.draconic_codex.physiology.gender",
                    genderValue
            ).getString();

            drawHealthStat(guiGraphics, selected);
            drawHungerStat(guiGraphics, selected);
            drawHappinessStat(guiGraphics, selected);
            drawVariantStat(guiGraphics, selected);
            drawArmorStat(guiGraphics, selected);
            drawGenderStat(guiGraphics, selected);
        } else {
            guiGraphics.drawString(this.font, activeTab.description(), contentX, contentY + 16, TEXT_COLOR, false);
        }
    }

    private void drawAllyPanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        int contentX = left + 8;
        int contentY = top + 8;

        String allyCountText = Component.translatable(
                "saintsdragons.gui.draconic_codex.ally.count",
                allyList.size(),
                com.leon.saintsdragons.server.entity.handler.DragonAllyManager.getMaxAlliesStatic()
        ).getString();
        guiGraphics.drawString(this.font, allyCountText, contentX, contentY, TEXT_COLOR, false);

        int listTop = contentY + 16;
        int listBottom = bottom - 16;
        int visibleCount = Math.min(MAX_VISIBLE_ALLIES, allyList.size() - allyScrollOffset);
        if (visibleCount < 0) {
            visibleCount = 0;
        }

        for (int i = 0; i < visibleCount; i++) {
            int index = i + allyScrollOffset;
            if (index >= allyList.size()) break;
            int y = listTop + (i * 12);
            guiGraphics.drawString(this.font, allyList.get(index), contentX, y, TEXT_COLOR, false);
        }

        if (allyList.isEmpty()) {
            guiGraphics.drawString(this.font,
                    Component.translatable("saintsdragons.gui.draconic_codex.ally.empty").getString(),
                    contentX, listTop, TEXT_COLOR, false);
        }
    }

    private void drawHealthStat(GuiGraphics guiGraphics, DragonEntry selected) {
        int iconX = leftPos + HEALTH_ICON_OFFSET_X;
        int iconY = topPos + HEALTH_ICON_OFFSET_Y;
        guiGraphics.blit(HEALTH_ICON, iconX, iconY, 0, 0, STAT_ICON_WIDTH, STAT_ICON_HEIGHT, STAT_ICON_WIDTH, STAT_ICON_HEIGHT);
        String healthValue = formatStat(selected.currentHealth) + "/" + formatStat(selected.maxHealth);
        guiGraphics.drawString(this.font, ":" + healthValue, iconX + STAT_ICON_WIDTH + 2, iconY + STAT_TEXT_OFFSET_Y, TEXT_COLOR, false);
    }

    private void drawArmorStat(GuiGraphics guiGraphics, DragonEntry selected) {
        int iconX = leftPos + HEALTH_ICON_OFFSET_X;
        int iconY = topPos + HEALTH_ICON_OFFSET_Y + STAT_ICON_HEIGHT + STAT_ICON_GAP_Y;
        guiGraphics.blit(ARMOR_ICON, iconX, iconY, 0, 0, STAT_ICON_WIDTH, STAT_ICON_HEIGHT, STAT_ICON_WIDTH, STAT_ICON_HEIGHT);
        String armorValue = formatStat(selected.armor);
        guiGraphics.drawString(this.font, ":" + armorValue, iconX + STAT_ICON_WIDTH + 2, iconY + STAT_TEXT_OFFSET_Y, TEXT_COLOR, false);
    }

    private void drawHungerStat(GuiGraphics guiGraphics, DragonEntry selected) {
        int iconX = leftPos + HUNGER_ICON_OFFSET_X;
        int iconY = topPos + HEALTH_ICON_OFFSET_Y;
        guiGraphics.blit(HUNGER_ICON, iconX, iconY, 0, 0, STAT_ICON_WIDTH, STAT_ICON_HEIGHT, STAT_ICON_WIDTH, STAT_ICON_HEIGHT);
        String hungerValue = formatStat(selected.hunger) + "/100";
        guiGraphics.drawString(this.font, ":" + hungerValue, iconX + STAT_ICON_WIDTH + 2, iconY + STAT_TEXT_OFFSET_Y, TEXT_COLOR, false);
    }

    private void drawHappinessStat(GuiGraphics guiGraphics, DragonEntry selected) {
        int iconX = leftPos + HAPPINESS_ICON_OFFSET_X;
        int iconY = topPos + HEALTH_ICON_OFFSET_Y + STAT_ICON_HEIGHT + STAT_ICON_GAP_Y;
        guiGraphics.blit(HAPPINESS_ICON, iconX, iconY, 0, 0, STAT_ICON_WIDTH, STAT_ICON_HEIGHT, STAT_ICON_WIDTH, STAT_ICON_HEIGHT);
        String happinessValue = formatStat(selected.happiness) + "/100";
        guiGraphics.drawString(this.font, ":" + happinessValue, iconX + STAT_ICON_WIDTH + 2, iconY + STAT_TEXT_OFFSET_Y, TEXT_COLOR, false);
    }

    private void drawVariantStat(GuiGraphics guiGraphics, DragonEntry selected) {
        int iconX = leftPos + VARIANT_ICON_OFFSET_X;
        int iconY = topPos + HEALTH_ICON_OFFSET_Y + (STAT_ICON_HEIGHT + STAT_ICON_GAP_Y) * 2;
        guiGraphics.blit(VARIANT_ICON, iconX, iconY, 0, 0, STAT_ICON_WIDTH, STAT_ICON_HEIGHT, STAT_ICON_WIDTH, STAT_ICON_HEIGHT);
        String key = selected.variantId == 1 ? "saintsdragons.variant.crimson" : "saintsdragons.variant.default";
        String value = Component.translatable(key).getString();
        guiGraphics.drawString(this.font, ":" + value, iconX + STAT_ICON_WIDTH + 2, iconY + STAT_TEXT_OFFSET_Y, TEXT_COLOR, false);
    }

    private void drawGenderStat(GuiGraphics guiGraphics, DragonEntry selected) {
        int iconX = leftPos + HEALTH_ICON_OFFSET_X;
        int iconY = topPos + HEALTH_ICON_OFFSET_Y + (STAT_ICON_HEIGHT + STAT_ICON_GAP_Y) * 2;
        guiGraphics.blit(GENDER_ICON, iconX, iconY, 0, 0, STAT_ICON_WIDTH, STAT_ICON_HEIGHT, STAT_ICON_WIDTH, STAT_ICON_HEIGHT);
        String genderKey = selected.genderKnown
                ? (selected.genderId == 1 ? "saintsdragons.gender.female" : "saintsdragons.gender.male")
                : "saintsdragons.gui.draconic_codex.physiology.gender_unknown";
        String genderValue = Component.translatable(genderKey).getString();
        guiGraphics.drawString(this.font, ":" + genderValue, iconX + STAT_ICON_WIDTH + 2, iconY + STAT_TEXT_OFFSET_Y, TEXT_COLOR, false);
    }

    private void drawDragonPortrait(GuiGraphics guiGraphics) {
        DragonEntry selected = getSelectedEntry();
        if (selected == null || this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        com.leon.saintsdragons.server.entity.base.DragonEntity dragon = findDragonEntity(selected.entityId);
        if (dragon == null) {
            return;
        }
        if (dragon.getType() != ModEntities.IGNIVORUS.get()) {
            return;
        }
        int x = leftPos + DRAGON_RENDER_BOX_X;
        int y = topPos + DRAGON_RENDER_BOX_Y;
        guiGraphics.blit(IGNIVORUS_PORTRAIT, x, y, 0, 0, DRAGON_RENDER_BOX_SIZE, DRAGON_RENDER_BOX_SIZE, DRAGON_RENDER_BOX_SIZE, DRAGON_RENDER_BOX_SIZE);
    }

    private com.leon.saintsdragons.server.entity.base.DragonEntity findDragonEntity(java.util.UUID dragonId) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        for (net.minecraft.world.entity.Entity entity : this.minecraft.level.entitiesForRendering()) {
            if (entity instanceof com.leon.saintsdragons.server.entity.base.DragonEntity dragon
                    && dragon.getUUID().equals(dragonId)) {
                return dragon;
            }
        }
        return null;
    }

    private String formatStat(double value) {
        if (Math.abs(value - Math.round(value)) < 0.01) {
            return String.format("%.0f", value);
        }
        return String.format("%.1f", value);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (handleTabClick(mouseX, mouseY)) {
                return true;
            }

            int listLeft = getListLeft();
            int listTop = getListTop();
            int listRight = listLeft + LIST_WIDTH;
            for (int i = 0; i < Math.min(MAX_VISIBLE_DRAGONS, dragonEntries.size() - listScrollOffset); i++) {
                int index = i + listScrollOffset;
                if (index >= dragonEntries.size()) break;
                int y = listTop + (i * 12);
                if (mouseX >= listLeft && mouseX <= listRight && mouseY >= y && mouseY < y + 12) {
                    selectedDragonId = dragonEntries.get(index).entityId;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleTabClick(double mouseX, double mouseY) {
        if (mouseY < getTabY(0) || mouseY > getTabY(2) + TAB_HEIGHT) {
            return false;
        }
        if (isWithinTab(mouseX, mouseY, 0)) {
            activeTab = CodexTab.PHYSIOLOGY;
            updateAllyWidgetVisibility();
            return true;
        }
        if (isWithinTab(mouseX, mouseY, 1)) {
            activeTab = CodexTab.ECOLOGY;
            updateAllyWidgetVisibility();
            return true;
        }
        if (isWithinTab(mouseX, mouseY, 2)) {
            activeTab = CodexTab.ALLY;
            updateAllyWidgetVisibility();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (activeTab == CodexTab.ALLY) {
            if (allyList.size() > MAX_VISIBLE_ALLIES) {
                if (delta < 0 && allyScrollOffset < allyList.size() - MAX_VISIBLE_ALLIES) {
                    allyScrollOffset++;
                } else if (delta > 0 && allyScrollOffset > 0) {
                    allyScrollOffset--;
                }
                return true;
            }
        }
        if (dragonEntries.size() > MAX_VISIBLE_DRAGONS) {
            if (delta < 0 && listScrollOffset < dragonEntries.size() - MAX_VISIBLE_DRAGONS) {
                listScrollOffset++;
            } else if (delta > 0 && listScrollOffset > 0) {
                listScrollOffset--;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void updateDragonList(List<DragonEntry> entries) {
        dragonEntries.clear();
        dragonEntries.addAll(entries);
        dragonEntries.sort(Comparator.comparing(entry -> entry.displayName.toLowerCase()));

        if (pendingSelectionId != null) {
            for (DragonEntry entry : dragonEntries) {
                if (Objects.equals(entry.entityId, pendingSelectionId)) {
                    selectedDragonId = entry.entityId;
                    break;
                }
            }
            pendingSelectionId = null;
        }

        listScrollOffset = Math.min(listScrollOffset, Math.max(0, dragonEntries.size() - MAX_VISIBLE_DRAGONS));
    }

    public void updateAllyList(List<String> newAllyList) {
        this.allyList = new ArrayList<>(newAllyList);
        this.allyList.sort(String.CASE_INSENSITIVE_ORDER);
        this.allyScrollOffset = Math.min(allyScrollOffset, Math.max(0, allyList.size() - MAX_VISIBLE_ALLIES));
    }

    public void addAlly(String username) {
        if (!allyList.contains(username)) {
            allyList.add(username);
            allyList.sort(String.CASE_INSENSITIVE_ORDER);
            allyScrollOffset = Math.min(allyScrollOffset, Math.max(0, allyList.size() - MAX_VISIBLE_ALLIES));
        }
    }

    public void removeAlly(String username) {
        allyList.removeIf(name -> name.equalsIgnoreCase(username));
        allyScrollOffset = Math.min(allyScrollOffset, Math.max(0, allyList.size() - MAX_VISIBLE_ALLIES));
    }

    private DragonEntry getSelectedEntry() {
        for (DragonEntry entry : dragonEntries) {
            if (entry.entityId != null && entry.entityId.equals(selectedDragonId)) {
                return entry;
            }
        }
        return null;
    }

    public record DragonEntry(java.util.UUID entityId, String displayName, double currentHealth, double maxHealth, double armor, double hunger, double happiness, int variantId, byte genderId, boolean genderKnown) {
    }

    private enum CodexTab {
        PHYSIOLOGY("saintsdragons.gui.draconic_codex.tab.physiology",
                "saintsdragons.gui.draconic_codex.placeholder.physiology"),
        ECOLOGY("saintsdragons.gui.draconic_codex.tab.ecology",
                "saintsdragons.gui.draconic_codex.placeholder.ecology"),
        ALLY("saintsdragons.gui.draconic_codex.tab.ally",
                "saintsdragons.gui.draconic_codex.placeholder.ally");

        private final String labelKey;
        private final String descKey;

        CodexTab(String labelKey, String descKey) {
            this.labelKey = labelKey;
            this.descKey = descKey;
        }

        public Component label() {
            return Component.translatable(labelKey);
        }

        public Component description() {
            return Component.translatable(descKey);
        }
    }

    private int getListLeft() {
        return leftPos + 7;
    }

    private int getListTop() {
        return topPos + 47;
    }

    private int getListBottom() {
        return topPos + GUI_HEIGHT - 18;
    }

    private int getDetailLeft() {
        return leftPos + 150;
    }

    private int getDetailTop() {
        return topPos + 44;
    }

    private int getDetailRight() {
        return leftPos + GUI_WIDTH - 18;
    }

    private int getDetailBottom() {
        return topPos + GUI_HEIGHT - 18;
    }

    private int getActiveTabX() {
        return leftPos + 366;
    }

    private int getTabY(int index) {
        return topPos + 24 + (TAB_HEIGHT + 2) * index;
    }

    private boolean isWithinTab(double mouseX, double mouseY, int index) {
        int y = getTabY(index);
        int x = getActiveTabX();
        return mouseX >= x && mouseX <= x + TAB_WIDTH && mouseY >= y && mouseY <= y + TAB_HEIGHT;
    }

    private void initAllyWidgets() {
        int inputX = getDetailLeft() + 12;
        int inputY = topPos + 52;
        int inputWidth = 140;
        int inputHeight = 20;

        allyInput = new net.minecraft.client.gui.components.EditBox(
                this.font,
                inputX,
                inputY,
                inputWidth,
                inputHeight,
                Component.translatable("saintsdragons.gui.draconic_codex.ally.input")
        );
        allyInput.setMaxLength(16);
        this.addRenderableWidget(allyInput);

        addAllyButton = net.minecraft.client.gui.components.Button.builder(
                Component.translatable("saintsdragons.gui.draconic_codex.ally.add"),
                button -> addAllyFromInput()
        ).bounds(inputX + inputWidth + 8, inputY, 60, 20).build();
        this.addRenderableWidget(addAllyButton);

        removeAllyButton = net.minecraft.client.gui.components.Button.builder(
                Component.translatable("saintsdragons.gui.draconic_codex.ally.remove"),
                button -> removeAllyFromInput()
        ).bounds(inputX + inputWidth + 8, inputY + 24, 60, 20).build();
        this.addRenderableWidget(removeAllyButton);

        updateAllyWidgetVisibility();
    }

    private void updateAllyWidgetVisibility() {
        boolean show = activeTab == CodexTab.ALLY;
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

    private void addAllyFromInput() {
        if (allyInput == null) {
            return;
        }
        String username = allyInput.getValue().trim();
        if (username.isEmpty()) {
            return;
        }
        NetworkHandler.sendToServer(new com.leon.saintsdragons.common.network.MessageGlobalAllyManagement(
                com.leon.saintsdragons.common.network.MessageGlobalAllyManagement.Action.ADD,
                username
        ));
        allyInput.setValue("");
    }

    private void removeAllyFromInput() {
        if (allyInput == null) {
            return;
        }
        String username = allyInput.getValue().trim();
        if (username.isEmpty()) {
            return;
        }
        NetworkHandler.sendToServer(new com.leon.saintsdragons.common.network.MessageGlobalAllyManagement(
                com.leon.saintsdragons.common.network.MessageGlobalAllyManagement.Action.REMOVE,
                username
        ));
        allyInput.setValue("");
    }
}
