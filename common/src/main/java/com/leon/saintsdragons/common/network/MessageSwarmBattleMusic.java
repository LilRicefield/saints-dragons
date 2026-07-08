package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

public record MessageSwarmBattleMusic(boolean active, int durationTicks) {
    public static void encode(MessageSwarmBattleMusic message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active());
        buffer.writeVarInt(message.durationTicks());
    }

    public static MessageSwarmBattleMusic decode(FriendlyByteBuf buffer) {
        return new MessageSwarmBattleMusic(buffer.readBoolean(), buffer.readVarInt());
    }

    public static void handle(MessageSwarmBattleMusic message) {
        Services.PLATFORM.runOnClient(() ->
                com.leon.saintsdragons.client.network.ClientPacketHandlers.handleSwarmBattleMusic(message));
    }
}
