package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Optimized network message for syncing individual ally changes (delta sync).
 */
public class MessageDragonAllyDelta {
    private final int dragonId;
    private final String username;
    private final boolean isAdd;

    public MessageDragonAllyDelta(int dragonId, String username, boolean isAdd) {
        this.dragonId = dragonId;
        this.username = username;
        this.isAdd = isAdd;
    }

    private MessageDragonAllyDelta(FriendlyByteBuf buffer) {
        this.dragonId = buffer.readInt();
        this.username = buffer.readUtf(16);
        this.isAdd = buffer.readBoolean();
    }

    public int dragonId() {
        return dragonId;
    }

    public String username() {
        return username;
    }

    public boolean isAdd() {
        return isAdd;
    }

    public static void encode(MessageDragonAllyDelta message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.dragonId);
        buffer.writeUtf(message.username, 16);
        buffer.writeBoolean(message.isAdd);
    }

    public static MessageDragonAllyDelta decode(FriendlyByteBuf buffer) {
        return new MessageDragonAllyDelta(buffer);
    }

    public static void handle(MessageDragonAllyDelta message) {
        Services.PLATFORM.runOnClient(() -> com.leon.saintsdragons.client.network.ClientPacketHandlers.handleAllyDelta(message));
    }
}
