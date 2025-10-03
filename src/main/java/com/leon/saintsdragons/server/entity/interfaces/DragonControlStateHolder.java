package com.leon.saintsdragons.server.entity.interfaces;

import net.minecraft.world.entity.player.Player;

/**
 * Marker interface for dragons that expose a rider control state byte to clients.
 */
public interface DragonControlStateHolder {
    byte getControlState();
    void setControlState(byte controlState);

    default boolean canPlayerModifyControlState(Player player) {
        return player != null;
    }
}
