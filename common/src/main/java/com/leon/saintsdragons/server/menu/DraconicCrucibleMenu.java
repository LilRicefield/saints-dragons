package com.leon.saintsdragons.server.menu;

import com.leon.saintsdragons.common.block.DraconicCrucibleBlockEntity;
import com.leon.saintsdragons.common.block.crucible.DraconicCrucibleFuelTier;
import com.leon.saintsdragons.common.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

public class DraconicCrucibleMenu extends AbstractContainerMenu {
    private static final int CRUCIBLE_SLOT_COUNT = DraconicCrucibleBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = CRUCIBLE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container crucible;
    private final ContainerData data;
    @Nullable
    private final DraconicCrucibleBlockEntity blockEntity;

    public DraconicCrucibleMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CRUCIBLE_SLOT_COUNT),
                new SimpleContainerData(DraconicCrucibleBlockEntity.DATA_COUNT), null);
    }

    public DraconicCrucibleMenu(int containerId, Inventory playerInventory,
                                DraconicCrucibleBlockEntity crucible, ContainerData data) {
        this(containerId, playerInventory, crucible, data, crucible);
    }

    private DraconicCrucibleMenu(int containerId, Inventory playerInventory, Container crucible,
                                 ContainerData data, @Nullable DraconicCrucibleBlockEntity blockEntity) {
        super(ModMenus.DRACONIC_CRUCIBLE.get(), containerId);
        checkContainerSize(crucible, CRUCIBLE_SLOT_COUNT);
        checkContainerDataCount(data, DraconicCrucibleBlockEntity.DATA_COUNT);
        this.crucible = crucible;
        this.data = data;
        this.blockEntity = blockEntity;
        this.crucible.startOpen(playerInventory.player);
        addDataSlots(data);

        BooleanSupplier unlocked = () -> !isProcessing();
        this.addSlot(new OutputSlot(
                crucible, DraconicCrucibleBlockEntity.OUTPUT_SLOT, 80, 21, unlocked));
        this.addSlot(new FuelSlot(
                crucible, DraconicCrucibleBlockEntity.FUEL_SLOT, 30, 91, unlocked));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                this.addSlot(new LockableSlot(crucible,
                        DraconicCrucibleBlockEntity.INPUT_SLOT_START + column + row * 3,
                        62 + column * 18, 60 + row * 18, unlocked));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 126 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 184));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.blockEntity == null || this.blockEntity.isUsableBy(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < CRUCIBLE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (DraconicCrucibleFuelTier.resolve(stack) != DraconicCrucibleFuelTier.NONE) {
            if (!moveItemStackTo(stack,
                    DraconicCrucibleBlockEntity.FUEL_SLOT,
                    DraconicCrucibleBlockEntity.FUEL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack,
                DraconicCrucibleBlockEntity.INPUT_SLOT_START,
                DraconicCrucibleBlockEntity.INPUT_SLOT_START + DraconicCrucibleBlockEntity.INPUT_SLOT_COUNT,
                false)) {
            if (index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.crucible.stopOpen(player);
    }

    public int getBurnTime() {
        return this.data.get(0);
    }

    public int getBurnTimeTotal() {
        return this.data.get(1);
    }

    public int getProcessingProgress() {
        return this.data.get(2);
    }

    public int getProcessingTimeTotal() {
        return this.data.get(3);
    }

    public int getHeatLevel() {
        return this.data.get(4);
    }

    public boolean isProcessing() {
        return this.data.get(5) != 0;
    }

    public boolean canStartProcessing() {
        return this.data.get(6) != 0;
    }

    public int getTierChargeCapacity(int heatLevel) {
        return heatLevel >= 1 && heatLevel <= 3 ? this.data.get(6 + heatLevel) : 0;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        return id == 0
                && this.blockEntity != null
                && this.blockEntity.beginProcessing(player.level());
    }

    private static class LockableSlot extends Slot {
        private final BooleanSupplier unlocked;

        private LockableSlot(Container container, int index, int x, int y, BooleanSupplier unlocked) {
            super(container, index, x, y);
            this.unlocked = unlocked;
        }

        @Override
        public boolean isActive() {
            return this.unlocked.getAsBoolean();
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return this.unlocked.getAsBoolean();
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return this.unlocked.getAsBoolean();
        }
    }

    private static final class OutputSlot extends LockableSlot {
        private OutputSlot(Container container, int index, int x, int y, BooleanSupplier unlocked) {
            super(container, index, x, y, unlocked);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return true;
        }
    }

    private static final class FuelSlot extends LockableSlot {
        private FuelSlot(Container container, int index, int x, int y, BooleanSupplier unlocked) {
            super(container, index, x, y, unlocked);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return DraconicCrucibleFuelTier.resolve(stack) != DraconicCrucibleFuelTier.NONE;
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return true;
        }
    }
}
