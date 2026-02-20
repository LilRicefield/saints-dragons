package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Network message for requesting the player's tamed dragons list for the Draconic Codex.
 */
public class MessageDraconicCodexRequest {
    private static final long REQUEST_COOLDOWN_TICKS = 10L;
    private static final ConcurrentHashMap<UUID, Long> NEXT_ALLOWED_REQUEST_TICK = new ConcurrentHashMap<>();

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
        long now = serverLevel.getGameTime();
        UUID playerId = player.getUUID();
        long nextAllowed = NEXT_ALLOWED_REQUEST_TICK.getOrDefault(playerId, 0L);
        if (now < nextAllowed) {
            return;
        }
        NEXT_ALLOWED_REQUEST_TICK.put(playerId, now + REQUEST_COOLDOWN_TICKS);

        DragonCodexSavedData data = DragonCodexSavedData.get(serverLevel);
        List<DragonCodexSavedData.DragonCodexEntry> entries = data.getEntriesFor(player);

        List<DragonEntity> dragons = serverLevel.getEntitiesOfClass(
                DragonEntity.class,
                player.getBoundingBox().inflate(256.0),
                dragon -> dragon.isTame() && dragon.isOwnedBy(player)
        );
        for (DragonEntity dragon : dragons) {
            data.addDragon(player, dragon);
        }
        entries = data.getEntriesFor(player);

        if (!entries.isEmpty()) {
            List<UUID> staleBoundDragonIds = new java.util.ArrayList<>();
            for (DragonCodexSavedData.DragonCodexEntry entry : entries) {
                UUID dragonId = entry.dragonId();
                DragonEntity dragon = findDragon(serverLevel, dragonId);
                if (dragon != null && dragon.isTame() && dragon.isOwnedBy(player)) {
                    data.updateDragonName(player.getUUID(), dragonId, dragon.getName().getString());
                    data.updateDragonStats(player.getUUID(), dragon);
                    entry.setDisplayName(dragon.getName().getString());
                    continue;
                }

                if (entry.boundInBinder()) {
                    boolean binderExists = BinderComponentUtil.isDragonBoundInLoadedWorld(serverLevel, dragonId);
                    if (binderExists) {
                        data.updateDragonBoundState(player.getUUID(), dragonId, true);
                    } else {
                        // Bound entry with no live dragon and no binder found anywhere loaded:
                        // this is a true disappearance (e.g., creative delete/discard/despawn).
                        staleBoundDragonIds.add(dragonId);
                    }
                }
            }

            for (UUID staleDragonId : staleBoundDragonIds) {
                data.removeDragon(player.getUUID(), staleDragonId);
            }
            entries = data.getEntriesFor(player);
        }

        List<DragonCodexSavedData.DragonCodexEntry> sortedEntries = new java.util.ArrayList<>(entries);
        sortedEntries.sort(Comparator.comparing(entry -> entry.displayName().toLowerCase()));
        java.util.Map<String, Integer> nameCounts = new java.util.HashMap<>();
        java.util.List<MessageDraconicCodexList.Entry> payload = new java.util.ArrayList<>(sortedEntries.size());
        for (DragonCodexSavedData.DragonCodexEntry entry : sortedEntries) {
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
                    entry.genderKnown(),
                    entry.dragonType(),
                    entry.isBaby(),
                    entry.posX(),
                    entry.posY(),
                    entry.posZ(),
                    entry.biomeId()
            ));
        }
        MessageDraconicCodexList response = new MessageDraconicCodexList(payload);
        NetworkHandler.sendToPlayer(player, response);
    }

    private static DragonEntity findDragon(ServerLevel originLevel, UUID dragonId) {
        net.minecraft.world.entity.Entity sameLevelEntity = originLevel.getEntity(dragonId);
        if (sameLevelEntity instanceof DragonEntity dragon) {
            return dragon;
        }
        if (originLevel.getServer() == null) {
            return null;
        }
        for (ServerLevel level : originLevel.getServer().getAllLevels()) {
            if (level == originLevel) {
                continue;
            }
            net.minecraft.world.entity.Entity entity = level.getEntity(dragonId);
            if (entity instanceof DragonEntity dragon) {
                return dragon;
            }
        }
        return null;
    }
}
