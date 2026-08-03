package com.leon.saintsdragons.server.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public final class DragonEquipmentAttachmentSlot extends Slot {
    private final int containerIndex;
    private final BooleanSupplier installed;
    private final BooleanSupplier canInstall;
    private final Predicate<Player> canRemove;
    private final Predicate<ItemStack> acceptedItem;
    private final Runnable install;
    private final BiConsumer<Player, ItemStack> remove;
    private final Runnable syncIndicator;

    public DragonEquipmentAttachmentSlot(
            Container container,
            int index,
            int x,
            int y,
            BooleanSupplier installed,
            BooleanSupplier canInstall,
            Predicate<Player> canRemove,
            Predicate<ItemStack> acceptedItem,
            Runnable install,
            BiConsumer<Player, ItemStack> remove,
            Runnable syncIndicator
    ) {
        super(container, index, x, y);
        this.containerIndex = index;
        this.installed = installed;
        this.canInstall = canInstall;
        this.canRemove = canRemove;
        this.acceptedItem = acceptedItem;
        this.install = install;
        this.remove = remove;
        this.syncIndicator = syncIndicator;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return !installed.getAsBoolean() && canInstall.getAsBoolean() && acceptedItem.test(stack);
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return installed.getAsBoolean() && canRemove.test(player);
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
        if (!installed.getAsBoolean()
                && canInstall.getAsBoolean()
                && acceptedItem.test(getItem())) {
            install.run();
            container.setItem(containerIndex, ItemStack.EMPTY);
        }
        syncIndicator.run();
    }

    @Override
    public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        super.onTake(player, stack);
        if (installed.getAsBoolean()) {
            remove.accept(player, stack);
        }
        syncIndicator.run();
    }
}
