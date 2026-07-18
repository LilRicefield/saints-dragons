package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

public record MessageMountedTeleport(
        int entityId,
        double originX,
        double originY,
        double originZ,
        double x,
        double y,
        double z,
        float yRot,
        float xRot
) {
    public static void encode(MessageMountedTeleport message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId());
        buffer.writeDouble(message.originX());
        buffer.writeDouble(message.originY());
        buffer.writeDouble(message.originZ());
        buffer.writeDouble(message.x());
        buffer.writeDouble(message.y());
        buffer.writeDouble(message.z());
        buffer.writeFloat(message.yRot());
        buffer.writeFloat(message.xRot());
    }

    public static MessageMountedTeleport decode(FriendlyByteBuf buffer) {
        return new MessageMountedTeleport(
                buffer.readVarInt(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(MessageMountedTeleport message) {
        Services.PLATFORM.runOnClient(() ->
                com.leon.saintsdragons.client.network.ClientPacketHandlers.handleMountedTeleport(message));
    }
}
