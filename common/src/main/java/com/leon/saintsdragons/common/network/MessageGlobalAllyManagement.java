package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.handler.DragonAllyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manage global allies for all owned dragons.
 */
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

        Component resultMessage;
        if (result == DragonAllyManager.AllyResult.EASTER_EGG) {
            resultMessage = switch (message.username.toLowerCase()) {
                case "notch" -> Component.translatable("saintsdragons.message.easter_egg.notch");
                case "jeb_" -> Component.translatable("saintsdragons.message.easter_egg.jeb_");
                case "dinnerbone" -> Component.translatable("saintsdragons.message.easter_egg.dinnerbone");
                case "grumm" -> Component.translatable("saintsdragons.message.easter_egg.grumm");
                case "herobrine" -> Component.translatable("saintsdragons.message.easter_egg.herobrine");
                default -> Component.translatable("saintsdragons.message.ally.easter_egg");
            };
        } else {
            resultMessage = Component.translatable("saintsdragons.message.ally." + result.name().toLowerCase(), message.username);
        }
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
