package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record MessageDragonMovingSound(
        UUID playbackId,
        int entityId,
        UUID entityUuid,
        String soundId,
        float volume,
        float pitch,
        int durationTicks
) {

    public static void encode(MessageDragonMovingSound message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.playbackId());
        buffer.writeVarInt(message.entityId());
        buffer.writeUUID(message.entityUuid());
        buffer.writeUtf(message.soundId());
        buffer.writeFloat(message.volume());
        buffer.writeFloat(message.pitch());
        buffer.writeVarInt(message.durationTicks());
    }

    public static MessageDragonMovingSound decode(FriendlyByteBuf buffer) {
        return new MessageDragonMovingSound(
                buffer.readUUID(),
                buffer.readVarInt(),
                buffer.readUUID(),
                buffer.readUtf(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readVarInt()
        );
    }

    public static void handle(MessageDragonMovingSound message) {
        Services.PLATFORM.runOnClient(() ->
                com.leon.saintsdragons.client.network.ClientPacketHandlers.handleDragonMovingSound(message));
    }
}
