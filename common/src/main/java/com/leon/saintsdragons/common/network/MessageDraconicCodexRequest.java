package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Network message for requesting the player's tamed dragons list for the Draconic Codex.
 */
public class MessageDraconicCodexRequest {

    public static void encode(MessageDraconicCodexRequest message, FriendlyByteBuf buffer) {
    }

    public static MessageDraconicCodexRequest decode(FriendlyByteBuf buffer) {
        return new MessageDraconicCodexRequest();
    }

    public static void handle(MessageDraconicCodexRequest message, ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        DragonCodexSavedData data = DragonCodexSavedData.get(serverLevel);
        List<DragonCodexSavedData.DragonCodexEntry> entries = data.getEntriesFor(player);

        if (entries.isEmpty()) {
            List<DragonEntity> dragons = serverLevel.getEntitiesOfClass(
                    DragonEntity.class,
                    player.getBoundingBox().inflate(256.0),
                    dragon -> dragon.isTame() && dragon.isOwnedBy(player)
            );
            for (DragonEntity dragon : dragons) {
                data.addDragon(player, dragon);
            }
            entries = data.getEntriesFor(player);
        }

        if (!entries.isEmpty()) {
            for (DragonCodexSavedData.DragonCodexEntry entry : entries) {
                UUID dragonId = entry.dragonId();
                net.minecraft.world.entity.Entity entity = serverLevel.getEntity(dragonId);
                if (entity instanceof DragonEntity dragon && dragon.isTame() && dragon.isOwnedBy(player)) {
                    data.updateDragonName(player.getUUID(), dragonId, dragon.getName().getString());
                    data.updateDragonStats(player.getUUID(), dragon);
                    entry.setDisplayName(dragon.getName().getString());
                }
            }
        }

        entries.sort(Comparator.comparing(entry -> entry.displayName().toLowerCase()));
        java.util.Map<String, Integer> nameCounts = new java.util.HashMap<>();
        java.util.List<MessageDraconicCodexList.Entry> payload = new java.util.ArrayList<>(entries.size());
        for (DragonCodexSavedData.DragonCodexEntry entry : entries) {
            String baseName = entry.displayName();
            String key = baseName.toLowerCase();
            int index = nameCounts.getOrDefault(key, 0);
            nameCounts.put(key, index + 1);
            String displayName = index == 0 ? baseName : baseName + " (" + index + ")";
            payload.add(new MessageDraconicCodexList.Entry(
                    entry.dragonId(),
                    displayName,
                    entry.currentHealth(),
                    entry.maxHealth(),
                    entry.armor(),
                    entry.hunger(),
                    entry.happiness(),
                    entry.variantId(),
                    entry.genderId(),
                    entry.genderKnown()
            ));
        }
        MessageDraconicCodexList response = new MessageDraconicCodexList(payload);
        NetworkHandler.sendToPlayer(player, response);
    }
}
