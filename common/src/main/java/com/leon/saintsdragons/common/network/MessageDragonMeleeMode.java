package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

public record MessageDragonMeleeMode(int mode) {

    public static void encode(MessageDragonMeleeMode message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.mode());
    }

    public static MessageDragonMeleeMode decode(FriendlyByteBuf buffer) {
        return new MessageDragonMeleeMode(buffer.readVarInt());
    }

    public static void handle(MessageDragonMeleeMode message) {
        Services.PLATFORM.runOnClient(() ->
                com.leon.saintsdragons.client.network.ClientPacketHandlers.handleMeleeMode(message));
    }
}