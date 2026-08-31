package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Removes only the requesting player's saved Codex record. It never removes an entity.
 */
public record MessageDraconicCodexRemoveEntry(UUID dragonId) {
    public static void encode(MessageDraconicCodexRemoveEntry message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.dragonId);
    }

    public static MessageDraconicCodexRemoveEntry decode(FriendlyByteBuf buffer) {
        return new MessageDraconicCodexRemoveEntry(buffer.readUUID());
    }

    public static void handle(MessageDraconicCodexRemoveEntry message, ServerPlayer player) {
        if (player == null || message.dragonId == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        DragonCodexSavedData.get(serverLevel).removeDragon(player.getUUID(), message.dragonId);
    }
}
