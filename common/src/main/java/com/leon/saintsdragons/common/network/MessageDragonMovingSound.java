package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

public record MessageDragonMovingSound(int entityId, String soundId, float volume, float pitch, int durationTicks) {

    public static void encode(MessageDragonMovingSound message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId());
        buffer.writeUtf(message.soundId());
        buffer.writeFloat(message.volume());
        buffer.writeFloat(message.pitch());
        buffer.writeVarInt(message.durationTicks());
    }

    public static MessageDragonMovingSound decode(FriendlyByteBuf buffer) {
        return new MessageDragonMovingSound(
                buffer.readVarInt(),
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

