package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;

public enum MessageDialogueClose {
    INSTANCE;

    public static void encode(MessageDialogueClose message, FriendlyByteBuf buffer) {
    }

    public static MessageDialogueClose decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(MessageDialogueClose message) {
        Services.PLATFORM.runOnClient(com.leon.saintsdragons.client.network.ClientPacketHandlers::handleDialogueClose);
    }
}
