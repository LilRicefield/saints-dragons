package com.leon.saintsdragons.server.entity.interfaces;

import net.minecraft.world.Container;

public interface DragonChestCarrier {
    boolean hasAttachedChest();

    void setAttachedChest(boolean value);

    Container getAttachedChestInventory();

    void removeAttachedChestAndDropContents();
}
