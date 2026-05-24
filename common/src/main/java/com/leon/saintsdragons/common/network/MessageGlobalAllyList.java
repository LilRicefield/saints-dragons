package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record MessageGlobalAllyList(List<String> allyList) {
    public MessageGlobalAllyList {
        allyList = List.copyOf(allyList);
    }

    public static void encode(MessageGlobalAllyList message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.allyList().size());
        for (String ally : message.allyList()) {
            buffer.writeUtf(ally, 16);
        }
    }

    public static MessageGlobalAllyList decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        List<String> allyList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            allyList.add(buffer.readUtf(16));
        }
        return new MessageGlobalAllyList(allyList);
    }

    public static void handle(MessageGlobalAllyList message) {
        Services.PLATFORM.runOnClient(() -> com.leon.saintsdragons.client.network.ClientPacketHandlers.handleGlobalAllyList(message));
    }
}
