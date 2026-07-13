package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

public record MessageBloodTempestDodgeEffect(int entityId) {
    public static void encode(MessageBloodTempestDodgeEffect message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId());
    }

    public static MessageBloodTempestDodgeEffect decode(FriendlyByteBuf buffer) {
        return new MessageBloodTempestDodgeEffect(buffer.readVarInt());
    }

    public static void handle(MessageBloodTempestDodgeEffect message) {
        Services.PLATFORM.runOnClient(() ->
                com.leon.saintsdragons.client.network.ClientPacketHandlers.handleBloodTempestDodgeEffect(message));
    }
}
