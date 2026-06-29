package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.menu.DragonInventoryMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DragonInventoryScreen extends AbstractContainerScreen<DragonInventoryMenu> {
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int CHEST_SLOTS_U = 0;
    private static final int CHEST_SLOTS_V = 166;
    private static final int CHEST_SLOT_U = 0;
    private static final int CHEST_SLOT_V = 220;
    private static final int PREVIEW_FRAME_X = 26;
    private static final int PREVIEW_FRAME_Y = 18;
    private static final int PREVIEW_FRAME_SIZE = 52;
    private static final int PREVIEW_OFFSET_X = 51;
    private static final int PREVIEW_OFFSET_Y = 60;
    private static final int PREVIEW_SCALE = 10;
    private static final int PREVIEW_MOUSE_Y_OFFSET = 24;

    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/gui/dragon_inventory_gui.png");
    @Nullable
    private final LivingEntity dragon;

    public DragonInventoryScreen(DragonInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
        Entity vehicle = inventory.player.getVehicle();
        this.dragon = vehicle instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        guiGraphics.blit(TEXTURE, x + 7, y + 17, CHEST_SLOT_U, CHEST_SLOT_V, 18, 18, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        if (this.menu.hasChestInstalled()) {
            guiGraphics.blit(TEXTURE, x + 79, y + 17, CHEST_SLOTS_U, CHEST_SLOTS_V,
                    this.menu.getChestColumns() * 18, 54, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        if (this.dragon != null) {
            int renderX = x + PREVIEW_OFFSET_X;
            int renderY = y + PREVIEW_OFFSET_Y;
            guiGraphics.enableScissor(
                    x + PREVIEW_FRAME_X,
                    y + PREVIEW_FRAME_Y,
                    x + PREVIEW_FRAME_X + PREVIEW_FRAME_SIZE,
                    y + PREVIEW_FRAME_Y + PREVIEW_FRAME_SIZE
            );
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    renderX,
                    renderY,
                    PREVIEW_SCALE,
                    (float) renderX - mouseX,
                    (float) (renderY - PREVIEW_MOUSE_Y_OFFSET) - mouseY,
                    this.dragon
            );
            guiGraphics.disableScissor();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
