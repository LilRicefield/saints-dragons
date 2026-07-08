package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

public record MessageSwarmWaveBar(boolean active, int wave, float progress, int durationTicks) {
    public static void encode(MessageSwarmWaveBar message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active());
        buffer.writeVarInt(message.wave());
        buffer.writeFloat(message.progress());
        buffer.writeVarInt(message.durationTicks());
    }

    public static MessageSwarmWaveBar decode(FriendlyByteBuf buffer) {
        return new MessageSwarmWaveBar(buffer.readBoolean(), buffer.readVarInt(), buffer.readFloat(), buffer.readVarInt());
    }

    public static void handle(MessageSwarmWaveBar message) {
        Services.PLATFORM.runOnClient(() ->
                com.leon.saintsdragons.client.network.ClientPacketHandlers.handleSwarmWaveBar(message));
    }
}
