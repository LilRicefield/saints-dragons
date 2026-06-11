package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.npc.dialogue.DialogueSessionRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record MessageDialogueChoice(int entityId, int choiceIndex) {
    public static void encode(MessageDialogueChoice message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeInt(message.choiceIndex);
    }

    public static MessageDialogueChoice decode(FriendlyByteBuf buffer) {
        return new MessageDialogueChoice(
                buffer.readInt(),
                buffer.readInt()
        );
    }

    public static void handle(MessageDialogueChoice message, ServerPlayer player) {
        if (player == null) {
            return;
        }
        DialogueSessionRegistry.choose(player, message.entityId, message.choiceIndex);
    }
}
