package com.leon.saintsdragons.common.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Legacy convenience wrapper retained for existing call-sites during migration.
 */
public final class ModNetworkHandler {
    private ModNetworkHandler() {
    }

    public static void sendToServer(Object message) {
        NetworkHandler.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        NetworkHandler.sendToPlayer(player, message);
    }

    public static void sendToTrackingPlayers(Entity entity, Object message) {
        NetworkHandler.sendToTracking(entity, message);
    }

    public static void sendToDimension(Level level, Object message) {
        NetworkHandler.sendToDimension(level, message);
    }
}
