package com.leon.saintsdragons.server.menu;

import com.leon.saintsdragons.common.registry.ModMenus;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.interfaces.DragonSaddleCarrier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class DragonInventoryMenu extends AbstractContainerMenu {
    private static final int SADDLE_SLOT_INDEX = 0;
    private static final int CHEST_SLOT_INDEX = 1;
    private static final int ATTACHMENT_CONTAINER_SLOT = 0;
    private static final int SADDLE_DATA_INDEX = 0;
    private static final int CHEST_DATA_INDEX = 1;
    private static final int CARGO_SLOT_START = 2;
    private static final int CARGO_COLUMNS = 5;
    private static final int CARGO_ROWS = 3;
    private static final int CARGO_SLOT_COUNT = CARGO_COLUMNS * CARGO_ROWS;
    private static final int CARGO_SLOT_END = CARGO_SLOT_START + CARGO_SLOT_COUNT;
    private static final int PLAYER_INV_START = CARGO_SLOT_END;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container cargoInventory;
    private final Container saddleSlotInventory;
    private final Container chestSlotInventory;
    private final ContainerData data;
    @Nullable
    private final DragonSaddleCarrier equipmentCarrier;
    @Nullable
    private final Entity carrierEntity;

    public DragonInventoryMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, new SimpleContainer(CARGO_SLOT_COUNT), new SimpleContainerData(2));
    }

    public DragonInventoryMenu(int containerId, Inventory playerInventory, Stegonaut stegonaut) {
        this(containerId, playerInventory, (DragonSaddleCarrier) stegonaut);
    }

    public DragonInventoryMenu(int containerId, Inventory playerInventory, DragonSaddleCarrier equipmentCarrier) {
        this(containerId, playerInventory, equipmentCarrier, equipmentCarrier.getAttachedChestInventory(), new SimpleContainerData(2));
        this.data.set(SADDLE_DATA_INDEX, equipmentCarrier.hasSaddle() ? 1 : 0);
        this.data.set(CHEST_DATA_INDEX, equipmentCarrier.hasAttachedChest() ? 1 : 0);
        syncEquipmentIndicators();
    }

    private DragonInventoryMenu(int containerId,
                                Inventory playerInventory,
                                @Nullable DragonSaddleCarrier equipmentCarrier,
                                Container cargoInventory,
                                ContainerData data) {
        super(ModMenus.DRAGON_INVENTORY.get(), containerId);
        this.equipmentCarrier = equipmentCarrier;
        this.carrierEntity = equipmentCarrier instanceof Entity entity ? entity : null;
        this.cargoInventory = cargoInventory;
        this.data = data;
        this.saddleSlotInventory = new SimpleContainer(1);
        this.chestSlotInventory = new SimpleContainer(1);
        this.cargoInventory.startOpen(playerInventory.player);
        addDataSlots(data);
        syncEquipmentIndicators();

        this.addSlot(new DragonEquipmentAttachmentSlot(
                this.saddleSlotInventory,
                ATTACHMENT_CONTAINER_SLOT,
                8,
                18,
                this::hasSaddleInstalled,
                () -> !hasSaddleInstalled(),
                this::canRemoveSaddle,
                stack -> stack.is(Items.SADDLE),
                this::installSaddle,
                (player, stack) -> removeSaddle(),
                this::syncEquipmentIndicators
        ));

        this.addSlot(new DragonEquipmentAttachmentSlot(
                this.chestSlotInventory,
                ATTACHMENT_CONTAINER_SLOT,
                8,
                40,
                this::hasChestInstalled,
                this::canInstallChest,
                player -> true,
                stack -> stack.is(Items.CHEST),
                this::installChest,
                (player, stack) -> removeChest(),
                this::syncEquipmentIndicators
        ));

        for (int row = 0; row < CARGO_ROWS; row++) {
            for (int col = 0; col < CARGO_COLUMNS; col++) {
                int slotIndex = col + row * CARGO_COLUMNS;
                int x = 80 + col * 18;
                int y = 18 + row * 18;
                this.addSlot(new CargoSlot(this.cargoInventory, slotIndex, x, y));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public boolean hasSaddleInstalled() {
        return this.data.get(SADDLE_DATA_INDEX) != 0;
    }

    public boolean hasChestInstalled() {
        return this.data.get(CHEST_DATA_INDEX) != 0;
    }

    public int getChestColumns() {
        return CARGO_COLUMNS;
    }

    @Override
    public void setData(int id, int data) {
        super.setData(id, data);
        if (id == SADDLE_DATA_INDEX || id == CHEST_DATA_INDEX) {
            syncEquipmentIndicators();
        }
    }

    @Override
    public void broadcastChanges() {
        if (this.equipmentCarrier != null) {
            this.data.set(SADDLE_DATA_INDEX, this.equipmentCarrier.hasSaddle() ? 1 : 0);
            this.data.set(CHEST_DATA_INDEX, this.equipmentCarrier.hasAttachedChest() ? 1 : 0);
            syncEquipmentIndicators();
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (this.carrierEntity == null) {
            return true;
        }
        return this.carrierEntity.isAlive() && this.carrierEntity.distanceTo(player) < 8.0F;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();

            if (index == SADDLE_SLOT_INDEX) {
                if (!quickMoveAttachedSaddleToPlayer(player)) {
                    return ItemStack.EMPTY;
                }
            } else if (index == CHEST_SLOT_INDEX) {
                if (!quickMoveAttachedChestToPlayer()) {
                    return ItemStack.EMPTY;
                }
            } else if (index < CARGO_SLOT_END) {
                if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!hasSaddleInstalled() && stack.is(Items.SADDLE)) {
                    if (!quickMovePlayerSaddleIntoAttachment(stack)) {
                        return ItemStack.EMPTY;
                    }
                } else if (canInstallChest() && stack.is(Items.CHEST)) {
                    if (!quickMovePlayerChestIntoAttachment(stack)) {
                        return ItemStack.EMPTY;
                    }
                } else if (hasChestInstalled()) {
                    if (!this.moveItemStackTo(stack, CARGO_SLOT_START, CARGO_SLOT_END, false)) {
                        if (index < PLAYER_INV_END) {
                            if (!this.moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (index < PLAYER_INV_END) {
                    if (!this.moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return original;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.cargoInventory.stopOpen(player);
    }

    private void syncEquipmentIndicators() {
        if (hasSaddleInstalled()) {
            this.saddleSlotInventory.setItem(ATTACHMENT_CONTAINER_SLOT, new ItemStack(Items.SADDLE));
        } else {
            this.saddleSlotInventory.setItem(ATTACHMENT_CONTAINER_SLOT, ItemStack.EMPTY);
        }
        if (hasChestInstalled()) {
            this.chestSlotInventory.setItem(ATTACHMENT_CONTAINER_SLOT, new ItemStack(Items.CHEST));
        } else {
            this.chestSlotInventory.setItem(ATTACHMENT_CONTAINER_SLOT, ItemStack.EMPTY);
        }
    }

    private boolean canInstallChest() {
        return hasSaddleInstalled() && !hasChestInstalled();
    }

    private boolean canRemoveSaddle(Player player) {
        if (!hasChestInstalled()) {
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer && this.carrierEntity != null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.all.remove_chest_before_saddle", this.carrierEntity.getName()),
                    true
            );
        }
        return false;
    }

    private void installSaddle() {
        if (this.equipmentCarrier == null || this.equipmentCarrier.hasSaddle()) {
            return;
        }
        this.equipmentCarrier.setSaddle(true);
        this.data.set(SADDLE_DATA_INDEX, 1);
        if (this.carrierEntity != null) {
            this.carrierEntity.playSound(SoundEvents.HORSE_SADDLE, 1.0F, 1.0F);
        }
        syncEquipmentIndicators();
    }

    private void removeSaddle() {
        if (this.equipmentCarrier == null || !this.equipmentCarrier.canRemoveSaddle()) {
            return;
        }
        this.equipmentCarrier.setSaddle(false);
        this.data.set(SADDLE_DATA_INDEX, 0);
        syncEquipmentIndicators();
    }

    private void installChest() {
        if (this.equipmentCarrier == null || !this.equipmentCarrier.canAttachChest()) {
            return;
        }
        this.equipmentCarrier.setAttachedChest(true);
        this.data.set(CHEST_DATA_INDEX, 1);
        if (this.carrierEntity != null) {
            this.carrierEntity.playSound(SoundEvents.DONKEY_CHEST, 1.0F, 1.0F);
        }
        syncEquipmentIndicators();
    }

    private void removeChest() {
        if (this.equipmentCarrier == null || !this.equipmentCarrier.hasAttachedChest()) {
            return;
        }
        this.equipmentCarrier.removeAttachedChestAndDropContents();
        this.data.set(CHEST_DATA_INDEX, 0);
        syncEquipmentIndicators();
    }

    private boolean quickMovePlayerChestIntoAttachment(ItemStack stack) {
        if (this.equipmentCarrier == null || !canInstallChest() || !stack.is(Items.CHEST)) {
            return false;
        }
        installChest();
        if (!hasChestInstalled()) {
            return false;
        }
        stack.shrink(1);
        return true;
    }

    private boolean quickMovePlayerSaddleIntoAttachment(ItemStack stack) {
        if (this.equipmentCarrier == null || hasSaddleInstalled() || !stack.is(Items.SADDLE)) {
            return false;
        }
        installSaddle();
        if (!hasSaddleInstalled()) {
            return false;
        }
        stack.shrink(1);
        return true;
    }

    private boolean quickMoveAttachedSaddleToPlayer(Player player) {
        if (this.equipmentCarrier == null || !hasSaddleInstalled() || !canRemoveSaddle(player)) {
            return false;
        }
        ItemStack saddle = new ItemStack(Items.SADDLE);
        if (!this.moveItemStackTo(saddle, PLAYER_INV_START, HOTBAR_END, true)) {
            return false;
        }
        removeSaddle();
        return true;
    }

    private boolean quickMoveAttachedChestToPlayer() {
        if (this.equipmentCarrier == null || !hasChestInstalled()) {
            return false;
        }
        ItemStack chest = new ItemStack(Items.CHEST);
        if (!this.moveItemStackTo(chest, PLAYER_INV_START, HOTBAR_END, true)) {
            return false;
        }
        removeChest();
        return true;
    }

    private final class CargoSlot extends Slot {
        private CargoSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return hasChestInstalled();
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return hasChestInstalled();
        }
    }

}
