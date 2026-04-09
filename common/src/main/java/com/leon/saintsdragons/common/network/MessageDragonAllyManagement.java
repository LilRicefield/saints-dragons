package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonAllyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Network message for managing player allies on a dragon.
 * Handles adding/removing allies and syncing ally lists.
 */
public class MessageDragonAllyManagement {
    private final int dragonId;
    private final Action action;
    private final String username;

    public MessageDragonAllyManagement(int dragonId, Action action, String username) {
        this.dragonId = dragonId;
        this.action = action;
        this.username = username;
    }

    private MessageDragonAllyManagement(FriendlyByteBuf buffer) {
        this.dragonId = buffer.readInt();
        this.action = buffer.readEnum(Action.class);
        this.username = buffer.readUtf(16);
    }

    public static void encode(MessageDragonAllyManagement message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.dragonId);
        buffer.writeEnum(message.action);
        buffer.writeUtf(message.username, 16);
    }

    public static MessageDragonAllyManagement decode(FriendlyByteBuf buffer) {
        return new MessageDragonAllyManagement(buffer);
    }

    public static void handle(MessageDragonAllyManagement message, ServerPlayer player) {
        if (player == null) {
            return;
        }

        Entity entity = player.level().getEntity(message.dragonId);
        if (!(entity instanceof DragonEntity dragon)) {
            player.sendSystemMessage(Component.translatable("saintsdragons.message.dragon_not_found"));
            return;
        }

        if (!dragon.isTame() || !dragon.isOwnedBy(player)) {
            player.sendSystemMessage(Component.translatable("saintsdragons.message.not_dragon_owner"));
            return;
        }

        DragonAllyManager allyManager = dragon.allyManager;
        DragonAllyManager.AllyResult result;

        switch (message.action) {
            case ADD -> result = allyManager.addAlly(message.username);
            case REMOVE -> result = allyManager.removeAlly(message.username);
            default -> result = DragonAllyManager.AllyResult.INVALID_USERNAME;
        }

        Component resultMessage = Component.translatable("saintsdragons.message.ally." + result.name().toLowerCase(), message.username);
        player.sendSystemMessage(resultMessage);

        if (result == DragonAllyManager.AllyResult.SUCCESS) {
            boolean isAdd = message.action == Action.ADD;
            NetworkHandler.sendToPlayer(player, new MessageDragonAllyDelta(dragon.getId(), message.username, isAdd));
        }
    }

    public enum Action {
        ADD,
        REMOVE
    }
}
