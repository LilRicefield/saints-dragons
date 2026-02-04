package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.network.MessageDraconicCodexRequest;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.client.ui.codex.CodexAllyPanel;
import com.leon.saintsdragons.client.ui.codex.CodexDetailPanel;
import com.leon.saintsdragons.client.ui.codex.CodexDragonEntry;
import com.leon.saintsdragons.client.ui.codex.CodexDragonListPanel;
import com.leon.saintsdragons.client.ui.codex.CodexDragonRenderer;
import com.leon.saintsdragons.client.ui.codex.CodexEcologyPanel;
import com.leon.saintsdragons.client.ui.codex.CodexLayout;
import com.leon.saintsdragons.client.ui.codex.CodexTab;
import com.leon.saintsdragons.client.ui.codex.CodexTabPanel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
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
    // Flag to disable custom animations during GUI rendering
    public static final ThreadLocal<Boolean> RENDERING_IN_GUI = ThreadLocal.withInitial(() -> false);

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
    private static final ResourceLocation CODEX_EDIT_BOX =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/codex_edit_box.png");
    private static final ResourceLocation ADD_ICON =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/icons/add_icon.png");
    private static final ResourceLocation REMOVE_ICON =
            SaintsDragonsCommon.rl("textures/gui/draconiccodex/icons/remove_icon.png");
    private static final ResourceLocation IGNIVORUS_EGG_TEXTURE =
            SaintsDragonsCommon.rl("textures/item/ignivorus_egg_item.png");

    private final CodexTabPanel tabPanel = new CodexTabPanel();
    private final CodexDragonListPanel dragonListPanel = new CodexDragonListPanel();
    private final CodexDragonRenderer dragonRenderer = new CodexDragonRenderer();
    private final CodexEcologyPanel ecologyPanel = new CodexEcologyPanel(IGNIVORUS_EGG_TEXTURE);
    private final CodexAllyPanel allyPanel = new CodexAllyPanel(CODEX_EDIT_BOX, ADD_ICON, REMOVE_ICON);
    private final CodexDetailPanel detailPanel = new CodexDetailPanel(
            HEALTH_ICON, ARMOR_ICON, GENDER_ICON, HUNGER_ICON, HAPPINESS_ICON, VARIANT_ICON
    );

    private int leftPos;
    private int topPos;
    private int listScrollOffset = 0;
    private CodexTab activeTab = CodexTab.PHYSIOLOGY;
    private final List<CodexDragonEntry> dragonEntries = new ArrayList<>();
    private java.util.UUID selectedDragonId;
    private java.util.UUID pendingSelectionId;
    private List<String> allyList = new ArrayList<>();
    private int allyScrollOffset = 0;
    private int ecologyPage = 1;

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
        int actualWidth = CodexLayout.GUI_WIDTH;
        int actualHeight = CodexLayout.GUI_HEIGHT;

        this.leftPos = Math.max(0, (this.width - actualWidth) / 2);
        this.topPos = Math.max(0, (this.height - actualHeight) / 2);

        // Request current tamed dragon list from server
        NetworkHandler.sendToServer(new MessageDraconicCodexRequest());
        NetworkHandler.sendToServer(new com.leon.saintsdragons.common.network.MessageGlobalAllyRequest());

        allyPanel.initWidgets(this::addRenderableWidget, this.font, leftPos, topPos,
                this::addAllyFromInput, this::removeAllyFromInput);
        ecologyPanel.initWidgets(this::addRenderableWidget, this.font, leftPos, topPos,
                () -> ecologyPage,
                page -> ecologyPage = page,
                this::getSelectedEntry,
                this::updateEcologyWidgetVisibility);

        updateAllyWidgetVisibility();
        updateEcologyWidgetVisibility();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        guiGraphics.blit(BOOK_TEXTURE, leftPos, topPos, 0, 0,
                CodexLayout.GUI_WIDTH, CodexLayout.GUI_HEIGHT,
                CodexLayout.GUI_WIDTH, CodexLayout.GUI_HEIGHT);

        int listLeft = CodexLayout.getListLeft(leftPos);
        int listTop = CodexLayout.getListTop(topPos);
        int listRight = listLeft + CodexLayout.LIST_WIDTH;
        int listBottom = CodexLayout.getListBottom(topPos);

        dragonListPanel.draw(guiGraphics, this.font, listLeft, listTop, listRight, listBottom,
                mouseX, mouseY, dragonEntries, listScrollOffset, selectedDragonId);
        tabPanel.drawTabs(guiGraphics, leftPos, topPos, activeTab,
                TAB_PHYSIOLOGY, TAB_PHYSIOLOGY_CLOSED,
                TAB_ECOLOGY, TAB_ECOLOGY_CLOSED,
                TAB_ALLY, TAB_ALLY_CLOSED);
        detailPanel.draw(guiGraphics, this.font, activeTab, getSelectedEntry(),
                leftPos, topPos, mouseX, mouseY, ecologyPage, ecologyPanel, allyPanel,
                allyList, allyScrollOffset);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render dragon last with scissor clipping
        dragonRenderer.drawDragonPortrait(guiGraphics, this.minecraft, getSelectedEntry(), leftPos, topPos, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (activeTab == CodexTab.ECOLOGY && handleEcologyLinkClick(mouseX, mouseY)) {
                return true;
            }
            if (handleTabClick(mouseX, mouseY)) {
                return true;
            }

            int listLeft = CodexLayout.getListLeft(leftPos);
            int listTop = CodexLayout.getListTop(topPos);
            int listRight = listLeft + CodexLayout.LIST_WIDTH;
            java.util.UUID clickedId = dragonListPanel.handleClick(mouseX, mouseY, listLeft, listTop, listRight,
                    dragonEntries, listScrollOffset);
            if (clickedId != null) {
                CodexDragonEntry clickedEntry = dragonEntries.stream()
                        .filter(entry -> clickedId.equals(entry.entityId()))
                        .findFirst()
                        .orElse(null);
                selectedDragonId = clickedId;
                ecologyPage = 1;
                ecologyPanel.resetLinkScroll();
                updateEcologyWidgetVisibility();

                if (clickedEntry != null) {
                    playDragonGrumble(clickedEntry.dragonType());
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleEcologyLinkClick(double mouseX, double mouseY) {
        return ecologyPanel.handleLinkClick(mouseX, mouseY,
                page -> ecologyPage = page,
                this::updateEcologyWidgetVisibility);
    }

    private boolean handleTabClick(double mouseX, double mouseY) {
        CodexTab clicked = tabPanel.handleClick(mouseX, mouseY, leftPos, topPos);
        if (clicked == null) {
            return false;
        }
        activeTab = clicked;
        if (activeTab == CodexTab.ECOLOGY) {
            ecologyPage = 1;
            ecologyPanel.resetLinkScroll();
        }
        updateAllyWidgetVisibility();
        updateEcologyWidgetVisibility();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (activeTab == CodexTab.ECOLOGY) {
            if (ecologyPanel.handleLinkScroll(mouseX, mouseY, delta)) {
                return true;
            }
        }
        if (activeTab == CodexTab.ALLY) {
            if (allyList.size() > CodexLayout.MAX_VISIBLE_ALLIES) {
                if (delta < 0 && allyScrollOffset < allyList.size() - CodexLayout.MAX_VISIBLE_ALLIES) {
                    allyScrollOffset++;
                } else if (delta > 0 && allyScrollOffset > 0) {
                    allyScrollOffset--;
                }
                return true;
            }
        }
        if (dragonEntries.size() > CodexLayout.MAX_VISIBLE_DRAGONS) {
            if (delta < 0 && listScrollOffset < dragonEntries.size() - CodexLayout.MAX_VISIBLE_DRAGONS) {
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

    public void updateDragonList(List<CodexDragonEntry> entries) {
        dragonEntries.clear();
        dragonEntries.addAll(entries);
        dragonEntries.sort(Comparator.comparing(entry -> entry.displayName().toLowerCase()));

        if (pendingSelectionId != null) {
            for (CodexDragonEntry entry : dragonEntries) {
                if (Objects.equals(entry.entityId(), pendingSelectionId)) {
                    selectedDragonId = entry.entityId();
                    break;
                }
            }
            pendingSelectionId = null;
        }

        listScrollOffset = Math.min(listScrollOffset, Math.max(0, dragonEntries.size() - CodexLayout.MAX_VISIBLE_DRAGONS));
    }

    public void updateAllyList(List<String> newAllyList) {
        this.allyList = new ArrayList<>(newAllyList);
        this.allyList.sort(String.CASE_INSENSITIVE_ORDER);
        this.allyScrollOffset = Math.min(allyScrollOffset, Math.max(0, allyList.size() - CodexLayout.MAX_VISIBLE_ALLIES));
    }

    public void addAlly(String username) {
        if (!allyList.contains(username)) {
            allyList.add(username);
            allyList.sort(String.CASE_INSENSITIVE_ORDER);
            allyScrollOffset = Math.min(allyScrollOffset, Math.max(0, allyList.size() - CodexLayout.MAX_VISIBLE_ALLIES));
        }
    }

    public void removeAlly(String username) {
        allyList.removeIf(name -> name.equalsIgnoreCase(username));
        allyScrollOffset = Math.min(allyScrollOffset, Math.max(0, allyList.size() - CodexLayout.MAX_VISIBLE_ALLIES));
    }

    private CodexDragonEntry getSelectedEntry() {
        for (CodexDragonEntry entry : dragonEntries) {
            if (entry.entityId() != null && entry.entityId().equals(selectedDragonId)) {
                return entry;
            }
        }
        return null;
    }


    private void updateAllyWidgetVisibility() {
        allyPanel.updateVisibility(activeTab == CodexTab.ALLY);
    }

    private void updateEcologyWidgetVisibility() {
        ecologyPanel.updateWidgetVisibility(activeTab == CodexTab.ECOLOGY, getSelectedEntry(), ecologyPage);
    }

    private void addAllyFromInput(String username) {
        NetworkHandler.sendToServer(new com.leon.saintsdragons.common.network.MessageGlobalAllyManagement(
                com.leon.saintsdragons.common.network.MessageGlobalAllyManagement.Action.ADD,
                username
        ));
    }

    private void removeAllyFromInput(String username) {
        NetworkHandler.sendToServer(new com.leon.saintsdragons.common.network.MessageGlobalAllyManagement(
                com.leon.saintsdragons.common.network.MessageGlobalAllyManagement.Action.REMOVE,
                username
        ));
    }

    private void playDragonGrumble(String dragonType) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        net.minecraft.sounds.SoundEvent grumbleSound = switch (dragonType) {
            case "raevyx" -> ModSounds.RAEVYX_GRUMBLE_1.get();
            case "ignivorus" -> ModSounds.IGNIVORUS_GRUMBLE_1.get();
            case "cindervane" -> ModSounds.CINDERVANE_GRUMBLE_1.get();
            case "nulljaw" -> ModSounds.NULLJAW_GRUMBLE_1.get();
            case "stegonaut" -> ModSounds.STEGONAUT_GRUMBLE_1.get();
            default -> null;
        };

        if (grumbleSound != null) {
            this.minecraft.player.playSound(grumbleSound, 0.8f, 1.0f);
        }
    }
}
