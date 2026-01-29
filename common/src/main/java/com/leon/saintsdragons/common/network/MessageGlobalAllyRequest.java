package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.handler.DragonAllyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Request the global ally list for the Draconic Codex.
 */
public class MessageGlobalAllyRequest {
    public static void encode(MessageGlobalAllyRequest message, FriendlyByteBuf buffer) {
    }

    public static MessageGlobalAllyRequest decode(FriendlyByteBuf buffer) {
        return new MessageGlobalAllyRequest();
    }

    public static void handle(MessageGlobalAllyRequest message, ServerPlayer player) {
        if (player == null) {
            return;
        }
        NetworkHandler.sendToPlayer(player, new MessageGlobalAllyList(
                DragonAllyManager.getAllyUsernamesForOwner(player)
        ));
    }
}
