package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Network message for requesting a dragon's player ally list from server.
 * Sent by client when opening the ally management GUI.
 */
public class MessageDragonAllyRequest {
    private final int dragonId;

    public MessageDragonAllyRequest(int dragonId) {
        this.dragonId = dragonId;
    }

    private MessageDragonAllyRequest(FriendlyByteBuf buffer) {
        this.dragonId = buffer.readInt();
    }

    public static void encode(MessageDragonAllyRequest message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.dragonId);
    }

    public static MessageDragonAllyRequest decode(FriendlyByteBuf buffer) {
        return new MessageDragonAllyRequest(buffer);
    }

    public static void handle(MessageDragonAllyRequest message, ServerPlayer player) {
        if (player == null) {
            return;
        }

        Entity entity = player.level().getEntity(message.dragonId);
        if (!(entity instanceof DragonEntity dragon)) {
            return;
        }

        if (!dragon.isTame() || !dragon.isOwnedBy(player)) {
            return;
        }

        NetworkHandler.sendToPlayer(player, new MessageDragonAllyList(dragon.getId(), dragon.allyManager.getAllyUsernames()));
    }
}
