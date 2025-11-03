package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Network message for syncing wyvern ally lists to clients.
 */
public class MessageDragonAllyList {
    private final int dragonId;
    private final List<String> allyList;

    public MessageDragonAllyList(int dragonId, List<String> allyList) {
        this.dragonId = dragonId;
        this.allyList = new ArrayList<>(allyList);
    }

    private MessageDragonAllyList(FriendlyByteBuf buffer) {
        this.dragonId = buffer.readInt();
        int size = buffer.readInt();
        this.allyList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            allyList.add(buffer.readUtf(16));
        }
    }

    public int dragonId() {
        return dragonId;
    }

    public List<String> allyList() {
        return allyList;
    }

    public static void encode(MessageDragonAllyList message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.dragonId);
        buffer.writeInt(message.allyList.size());
        for (String ally : message.allyList) {
            buffer.writeUtf(ally, 16);
        }
    }

    public static MessageDragonAllyList decode(FriendlyByteBuf buffer) {
        return new MessageDragonAllyList(buffer);
    }

    public static void handle(MessageDragonAllyList message) {
        Services.PLATFORM.runOnClient(() -> com.leon.saintsdragons.client.network.ClientPacketHandlers.handleAllyList(message));
    }
}
