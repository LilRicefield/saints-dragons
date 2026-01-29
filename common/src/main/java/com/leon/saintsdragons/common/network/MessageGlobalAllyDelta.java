package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Delta update for global ally list.
 */
public class MessageGlobalAllyDelta {
    private final String username;
    private final boolean isAdd;

    public MessageGlobalAllyDelta(String username, boolean isAdd) {
        this.username = username;
        this.isAdd = isAdd;
    }

    private MessageGlobalAllyDelta(FriendlyByteBuf buffer) {
        this.username = buffer.readUtf(16);
        this.isAdd = buffer.readBoolean();
    }

    public String username() {
        return username;
    }

    public boolean isAdd() {
        return isAdd;
    }

    public static void encode(MessageGlobalAllyDelta message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.username, 16);
        buffer.writeBoolean(message.isAdd);
    }

    public static MessageGlobalAllyDelta decode(FriendlyByteBuf buffer) {
        return new MessageGlobalAllyDelta(buffer);
    }

    public static void handle(MessageGlobalAllyDelta message) {
        Services.PLATFORM.runOnClient(() -> com.leon.saintsdragons.client.network.ClientPacketHandlers.handleGlobalAllyDelta(message));
    }
}
