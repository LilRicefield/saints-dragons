package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import com.leon.saintsdragons.server.menu.IvyInventoryMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class IvyInventoryScreen extends AbstractContainerScreen<IvyInventoryMenu> {
    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/gui/ivy/ivy_inventory.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private static final int IMAGE_W = 176;
    private static final int IMAGE_H = 234;
    private static final int BASE_W = 176;
    private static final int PREVIEW_X = 51;
    private static final int PREVIEW_Y = 74;
    private static final int PREVIEW_SCALE = 28;
    private static final int PREVIEW_MOUSE_Y_OFFSET = 34;
    private static final int VITALS_DEST_ICON_X = 84;
    private static final int VITALS_DEST_BAR_X = 96;
    private static final int VITALS_DEST_HEALTH_Y = 11;
    private static final int VITALS_DEST_ARMOR_Y = 23;
    private static final int VITALS_VALUE_X = 160;
    private static final int VITALS_SOURCE_HEART_ICON_X = 177;
    private static final int VITALS_SOURCE_HEART_ICON_Y = 0;
    private static final int VITALS_SOURCE_ARMOR_ICON_X = 177;
    private static final int VITALS_SOURCE_ARMOR_ICON_Y = 12;
    private static final int VITALS_SOURCE_BAR_X = 187;
    private static final int VITALS_SOURCE_HEALTH_BAR_Y = 0;
    private static final int VITALS_SOURCE_ARMOR_BAR_Y = 12;
    private static final int VITALS_SOURCE_FILL_X = 190;
    private static final int VITALS_SOURCE_HEALTH_FILL_Y = 9;
    private static final int VITALS_SOURCE_ARMOR_FILL_Y = 21;
    private static final int ICON_SIZE = 9;
    private static final int BAR_W = 62;
    private static final int BAR_H = 8;
    private static final int FILL_W = 44;
    private static final int FILL_H = 2;

    public IvyInventoryScreen(IvyInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = IMAGE_W;
        this.imageHeight = IMAGE_H;
        this.titleLabelX = 80;
        this.titleLabelY = 6;
        this.inventoryLabelX = 7;
        this.inventoryLabelY = 142;
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, BASE_W, IMAGE_H, TEX_W, TEX_H);
        renderIvyPreview(guiGraphics, x, y, mouseX, mouseY);
        renderVitals(guiGraphics, x, y);
    }

    private void renderIvyPreview(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        IvyTheDragonMerchant ivy = getIvy();
        if (ivy == null) {
            return;
        }
        int renderX = x + PREVIEW_X;
        int renderY = y + PREVIEW_Y;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                renderX,
                renderY,
                PREVIEW_SCALE,
                (float) renderX - mouseX,
                (float) (renderY - PREVIEW_MOUSE_Y_OFFSET) - mouseY,
                ivy
        );
    }

    private void renderVitals(GuiGraphics guiGraphics, int x, int y) {
        IvyTheDragonMerchant ivy = getIvy();
        int healthFillWidth = FILL_W;
        int healthValue = 0;
        if (ivy != null) {
            float healthRatio = Math.max(0.0F, Math.min(1.0F, ivy.getHealth() / ivy.getMaxHealth()));
            healthFillWidth = Math.round(FILL_W * healthRatio);
            healthValue = Math.round(ivy.getHealth());
        }
        int armorValue = getArmorValue();
        int armorFillWidth = getArmorFillWidth();

        int iconX = x + VITALS_DEST_ICON_X;
        int barX = x + VITALS_DEST_BAR_X;
        int healthY = y + VITALS_DEST_HEALTH_Y;
        int armorY = y + VITALS_DEST_ARMOR_Y;

        guiGraphics.blit(TEXTURE, iconX, healthY, VITALS_SOURCE_HEART_ICON_X, VITALS_SOURCE_HEART_ICON_Y,
                ICON_SIZE, ICON_SIZE, TEX_W, TEX_H);
        guiGraphics.blit(TEXTURE, barX, healthY, VITALS_SOURCE_BAR_X, VITALS_SOURCE_HEALTH_BAR_Y,
                BAR_W, BAR_H, TEX_W, TEX_H);
        if (healthFillWidth > 0) {
            guiGraphics.blit(TEXTURE, barX + 3, healthY + 3,
                    VITALS_SOURCE_FILL_X, VITALS_SOURCE_HEALTH_FILL_Y,
                    healthFillWidth, FILL_H, TEX_W, TEX_H);
        }

        guiGraphics.blit(TEXTURE, iconX, armorY, VITALS_SOURCE_ARMOR_ICON_X, VITALS_SOURCE_ARMOR_ICON_Y,
                ICON_SIZE, ICON_SIZE, TEX_W, TEX_H);
        guiGraphics.blit(TEXTURE, barX, armorY, VITALS_SOURCE_BAR_X, VITALS_SOURCE_ARMOR_BAR_Y,
                BAR_W, BAR_H, TEX_W, TEX_H);
        if (armorFillWidth > 0) {
            guiGraphics.blit(TEXTURE, barX + 3, armorY + 3,
                    VITALS_SOURCE_FILL_X, VITALS_SOURCE_ARMOR_FILL_Y,
                    armorFillWidth, FILL_H, TEX_W, TEX_H);
        }

        renderVitalValue(guiGraphics, String.valueOf(healthValue), x + VITALS_VALUE_X, healthY);
        renderVitalValue(guiGraphics, String.valueOf(armorValue), x + VITALS_VALUE_X, armorY);
    }

    private void renderVitalValue(GuiGraphics guiGraphics, String value, int x, int y) {
        guiGraphics.drawString(this.font, value, x - this.font.width(value), y + 1, 0xFFEDE7D2, false);
    }

    private int getArmorFillWidth() {
        return Math.round(FILL_W * getArmorRatio());
    }

    private float getArmorRatio() {
        return Math.max(0.0F, Math.min(1.0F, getArmorValue() / 20.0F));
    }

    private int getArmorValue() {
        int armor = 0;
        armor += getArmorDefense(IvyInventoryMenu.HELMET_SLOT);
        armor += getArmorDefense(IvyInventoryMenu.CHESTPLATE_SLOT);
        armor += getArmorDefense(IvyInventoryMenu.LEGGINGS_SLOT);
        armor += getArmorDefense(IvyInventoryMenu.BOOTS_SLOT);
        return armor;
    }

    private int getArmorDefense(int slotIndex) {
        ItemStack stack = this.menu.getSlot(slotIndex).getItem();
        return stack.getItem() instanceof ArmorItem armor ? armor.getDefense() : 0;
    }

    @Nullable
    private IvyTheDragonMerchant getIvy() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(this.menu.getIvyEntityId());
        return entity instanceof IvyTheDragonMerchant ivy ? ivy : null;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
