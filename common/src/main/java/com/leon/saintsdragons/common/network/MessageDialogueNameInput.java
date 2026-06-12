package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.npc.dialogue.DialogueSessionRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record MessageDialogueNameInput(int entityId, String name) {
    private static final int MAX_RAW_LENGTH = 128;

    public static void encode(MessageDialogueNameInput message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeUtf(message.name, MAX_RAW_LENGTH);
    }

    public static MessageDialogueNameInput decode(FriendlyByteBuf buffer) {
        return new MessageDialogueNameInput(buffer.readInt(), buffer.readUtf(MAX_RAW_LENGTH));
    }

    public static void handle(MessageDialogueNameInput message, ServerPlayer player) {
        if (player == null) {
            return;
        }
        DialogueSessionRegistry.submitName(player, message.entityId, message.name);
    }
}
