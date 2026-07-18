package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

public record MessageDragonlordFlightBoost(int durationTicks) {

    public static void encode(MessageDragonlordFlightBoost message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.durationTicks());
    }

    public static MessageDragonlordFlightBoost decode(FriendlyByteBuf buffer) {
        return new MessageDragonlordFlightBoost(buffer.readVarInt());
    }

    public static void handle(MessageDragonlordFlightBoost message) {
        Services.PLATFORM.runOnClient(() ->
                com.leon.saintsdragons.client.network.ClientPacketHandlers.handleDragonlordFlightBoost(message));
    }
}
