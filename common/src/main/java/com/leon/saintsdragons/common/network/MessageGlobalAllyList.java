package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Sync global ally list to client.
 */
public class MessageGlobalAllyList {
    private final List<String> allyList;

    public MessageGlobalAllyList(List<String> allyList) {
        this.allyList = new ArrayList<>(allyList);
    }

    private MessageGlobalAllyList(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        this.allyList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            allyList.add(buffer.readUtf(16));
        }
    }

    public List<String> allyList() {
        return allyList;
    }

    public static void encode(MessageGlobalAllyList message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.allyList.size());
        for (String ally : message.allyList) {
            buffer.writeUtf(ally, 16);
        }
    }

    public static MessageGlobalAllyList decode(FriendlyByteBuf buffer) {
        return new MessageGlobalAllyList(buffer);
    }

    public static void handle(MessageGlobalAllyList message) {
        Services.PLATFORM.runOnClient(() -> com.leon.saintsdragons.client.network.ClientPacketHandlers.handleGlobalAllyList(message));
    }
}
