package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.handler.DragonAllyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MessageGlobalAllyManagement {
    private final Action action;
    private final String username;

    public MessageGlobalAllyManagement(Action action, String username) {
        this.action = action;
        this.username = username;
    }

    private MessageGlobalAllyManagement(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(Action.class);
        this.username = buffer.readUtf(16);
    }

    public static void encode(MessageGlobalAllyManagement message, FriendlyByteBuf buffer) {
        buffer.writeEnum(message.action);
        buffer.writeUtf(message.username, 16);
    }

    public static MessageGlobalAllyManagement decode(FriendlyByteBuf buffer) {
        return new MessageGlobalAllyManagement(buffer);
    }

    public static void handle(MessageGlobalAllyManagement message, ServerPlayer player) {
        if (player == null) {
            return;
        }

        DragonAllyManager.AllyResult result;
        switch (message.action) {
            case ADD -> result = DragonAllyManager.addAllyForOwner(player, message.username);
            case REMOVE -> result = DragonAllyManager.removeAllyForOwner(player, message.username);
            default -> result = DragonAllyManager.AllyResult.INVALID_USERNAME;
        }

        Component resultMessage = Component.translatable("saintsdragons.message.ally." + result.name().toLowerCase(), message.username);
        player.sendSystemMessage(resultMessage);

        if (result == DragonAllyManager.AllyResult.SUCCESS) {
            boolean isAdd = message.action == Action.ADD;
            NetworkHandler.sendToPlayer(player, new MessageGlobalAllyDelta(message.username, isAdd));
        }
    }

    public enum Action {
        ADD,
        REMOVE
    }
}