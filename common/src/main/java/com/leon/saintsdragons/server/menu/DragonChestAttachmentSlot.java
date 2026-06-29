package com.leon.saintsdragons.server.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class DragonChestAttachmentSlot extends Slot {
    private final int containerIndex;
    private final BooleanSupplier hasChest;
    private final Predicate<ItemStack> chestItem;
    private final Runnable installChest;
    private final BiConsumer<Player, ItemStack> removeChest;
    private final Runnable syncIndicator;

    public DragonChestAttachmentSlot(Container container,
                                     int index,
                                     int x,
                                     int y,
                                     BooleanSupplier hasChest,
                                     Predicate<ItemStack> chestItem,
                                     Runnable installChest,
                                     BiConsumer<Player, ItemStack> removeChest,
                                     Runnable syncIndicator) {
        super(container, index, x, y);
        this.containerIndex = index;
        this.hasChest = hasChest;
        this.chestItem = chestItem;
        this.installChest = installChest;
        this.removeChest = removeChest;
        this.syncIndicator = syncIndicator;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return !this.hasChest.getAsBoolean() && this.chestItem.test(stack);
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return this.hasChest.getAsBoolean();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return 1;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!this.hasChest.getAsBoolean() && this.chestItem.test(this.getItem())) {
            this.installChest.run();
            this.container.setItem(this.containerIndex, ItemStack.EMPTY);
        }
        this.syncIndicator.run();
    }

    @Override
    public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        super.onTake(player, stack);
        if (this.hasChest.getAsBoolean()) {
            this.removeChest.accept(player, stack);
        }
        this.syncIndicator.run();
    }
}
