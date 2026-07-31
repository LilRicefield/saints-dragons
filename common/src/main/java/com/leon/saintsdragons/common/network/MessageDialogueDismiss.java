package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.npc.dialogue.DialogueSessionRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record MessageDialogueDismiss(int entityId) {
    public static void encode(MessageDialogueDismiss message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
    }

    public static MessageDialogueDismiss decode(FriendlyByteBuf buffer) {
        return new MessageDialogueDismiss(buffer.readInt());
    }

    public static void handle(MessageDialogueDismiss message, ServerPlayer player) {
        if (player == null) {
            return;
        }
        DialogueSessionRegistry.suspend(player, message.entityId);
    }
}
