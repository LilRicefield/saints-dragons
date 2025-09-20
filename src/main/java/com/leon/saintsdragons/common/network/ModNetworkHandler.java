package com.leon.saintsdragons.common.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * Helper class for sending network messages.
 * Provides convenient methods for common network operations.
 */
public class ModNetworkHandler {
    
    /**
     * Send a message to the server
     */
    public static void sendToServer(Object message) {
        NetworkHandler.INSTANCE.sendToServer(message);
    }
    
    /**
     * Send a message to a specific player
     */
    public static void sendToPlayer(ServerPlayer player, Object message) {
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
    
    /**
     * Send a message to all players tracking an entity
     */
    public static void sendToTrackingPlayers(net.minecraft.world.entity.Entity entity, Object message) {
        NetworkHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
    }
    
    /**
     * Send a message to all players in a dimension
     */
    public static void sendToDimension(net.minecraft.world.level.Level level, Object message) {
        NetworkHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), message);
    }
}
