package com.leon.saintsdragons.server.entity.interfaces;

public interface DragonSaddleCarrier extends DragonChestCarrier {
    boolean hasSaddle();

    void setSaddle(boolean saddled);

    default boolean canAttachChest() {
        return hasSaddle() && !hasAttachedChest();
    }

    default boolean canRemoveSaddle() {
        return !hasAttachedChest();
    }
}
