package com.leon.saintsdragons.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public interface NetworkHelper {
    <T> void registerServerbound(Class<T> type,
                                 ResourceLocation id,
                                 PacketEncoder<T> encoder,
                                 PacketDecoder<T> decoder,
                                 ServerboundHandler<T> handler);

    <T> void registerClientbound(Class<T> type,
                                 ResourceLocation id,
                                 PacketEncoder<T> encoder,
                                 PacketDecoder<T> decoder,
                                 ClientboundHandler<T> handler);

    void sendToServer(Object message);

    void sendToPlayer(ServerPlayer player, Object message);

    void sendToTracking(Entity entity, Object message);

    void sendToDimension(Level level, Object message);

    @FunctionalInterface
    interface PacketEncoder<T> {
        void encode(T message, FriendlyByteBuf buffer);
    }

    @FunctionalInterface
    interface PacketDecoder<T> {
        T decode(FriendlyByteBuf buffer);
    }

    @FunctionalInterface
    interface ServerboundHandler<T> {
        void handle(T message, ServerPlayer player);
    }

    @FunctionalInterface
    interface ClientboundHandler<T> {
        void handle(T message);
    }
}
