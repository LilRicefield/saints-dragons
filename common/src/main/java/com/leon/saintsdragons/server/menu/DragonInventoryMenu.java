package com.leon.saintsdragons.server.menu;

import com.leon.saintsdragons.common.registry.ModMenus;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.interfaces.DragonChestCarrier;
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
    private static final int CHEST_SLOT_INDEX = 0;
    private static final int CARGO_SLOT_START = 1;
    private static final int CARGO_COLUMNS = 5;
    private static final int CARGO_ROWS = 3;
    private static final int CARGO_SLOT_COUNT = CARGO_COLUMNS * CARGO_ROWS;
    private static final int CARGO_SLOT_END = CARGO_SLOT_START + CARGO_SLOT_COUNT;
    private static final int PLAYER_INV_START = CARGO_SLOT_END;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container cargoInventory;
    private final Container chestSlotInventory;
    private final ContainerData data;
    @Nullable
    private final DragonChestCarrier chestCarrier;
    @Nullable
    private final Entity carrierEntity;

    public DragonInventoryMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, new SimpleContainer(CARGO_SLOT_COUNT), new SimpleContainerData(1));
    }

    public DragonInventoryMenu(int containerId, Inventory playerInventory, Stegonaut stegonaut) {
        this(containerId, playerInventory, (DragonChestCarrier) stegonaut);
    }

    public DragonInventoryMenu(int containerId, Inventory playerInventory, DragonChestCarrier chestCarrier) {
        this(containerId, playerInventory, chestCarrier, chestCarrier.getAttachedChestInventory(), new SimpleContainerData(1));
        this.data.set(0, chestCarrier.hasAttachedChest() ? 1 : 0);
    }

    private DragonInventoryMenu(int containerId,
                                Inventory playerInventory,
                                @Nullable DragonChestCarrier chestCarrier,
                                Container cargoInventory,
                                ContainerData data) {
        super(ModMenus.DRAGON_INVENTORY.get(), containerId);
        this.chestCarrier = chestCarrier;
        this.carrierEntity = chestCarrier instanceof Entity entity ? entity : null;
        this.cargoInventory = cargoInventory;
        this.data = data;
        this.chestSlotInventory = new SimpleContainer(1);
        this.cargoInventory.startOpen(playerInventory.player);
        addDataSlots(data);
        syncChestIndicator();

        this.addSlot(new DragonChestAttachmentSlot(
                this.chestSlotInventory,
                CHEST_SLOT_INDEX,
                8,
                18,
                this::hasChestInstalled,
                stack -> stack.is(Items.CHEST),
                this::installChest,
                (player, stack) -> removeChest(),
                this::syncChestIndicator
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

    public boolean hasChestInstalled() {
        return this.data.get(0) != 0;
    }

    public int getChestColumns() {
        return CARGO_COLUMNS;
    }

    @Override
    public void setData(int id, int data) {
        super.setData(id, data);
        if (id == 0) {
            syncChestIndicator();
        }
    }

    @Override
    public void broadcastChanges() {
        if (this.chestCarrier != null) {
            this.data.set(0, this.chestCarrier.hasAttachedChest() ? 1 : 0);
            syncChestIndicator();
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

            if (index == CHEST_SLOT_INDEX) {
                if (!quickMoveAttachedChestToPlayer()) {
                    return ItemStack.EMPTY;
                }
            } else if (index < CARGO_SLOT_END) {
                if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!hasChestInstalled() && stack.is(Items.CHEST)) {
                    if (!quickMovePlayerChestIntoAttachment(player, index, stack)) {
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

    private void syncChestIndicator() {
        if (hasChestInstalled()) {
            this.chestSlotInventory.setItem(CHEST_SLOT_INDEX, new ItemStack(Items.CHEST));
        } else {
            this.chestSlotInventory.setItem(CHEST_SLOT_INDEX, ItemStack.EMPTY);
        }
    }

    private void installChest() {
        if (this.chestCarrier == null || this.chestCarrier.hasAttachedChest()) {
            return;
        }
        this.chestCarrier.setAttachedChest(true);
        this.data.set(0, 1);
        if (this.carrierEntity != null) {
            this.carrierEntity.playSound(SoundEvents.DONKEY_CHEST, 1.0F, 1.0F);
        }
        syncChestIndicator();
    }

    private void removeChest() {
        if (this.chestCarrier == null || !this.chestCarrier.hasAttachedChest()) {
            return;
        }
        this.chestCarrier.removeAttachedChestAndDropContents();
        this.data.set(0, 0);
        syncChestIndicator();
    }

    private boolean quickMovePlayerChestIntoAttachment(Player player, int index, ItemStack stack) {
        if (this.chestCarrier == null || this.chestCarrier.hasAttachedChest() || !stack.is(Items.CHEST)) {
            return false;
        }
        installChest();
        if (!hasChestInstalled()) {
            return false;
        }
        stack.shrink(1);
        return true;
    }

    private boolean quickMoveAttachedChestToPlayer() {
        if (this.chestCarrier == null || !hasChestInstalled()) {
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
