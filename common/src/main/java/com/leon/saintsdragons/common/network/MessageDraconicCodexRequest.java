package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageDraconicCodexRequest {
    private static final long REQUEST_COOLDOWN_TICKS = 10L;
    private static final ConcurrentHashMap<UUID, Long> NEXT_ALLOWED_REQUEST_TICK = new ConcurrentHashMap<>();
    private final boolean pruneMissingBoundEntries;
    public MessageDraconicCodexRequest(boolean pruneMissingBoundEntries) {
        this.pruneMissingBoundEntries = pruneMissingBoundEntries;
    }

    public static void encode(MessageDraconicCodexRequest message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.pruneMissingBoundEntries);
    }

    public static MessageDraconicCodexRequest decode(FriendlyByteBuf buffer) {
        return new MessageDraconicCodexRequest(buffer.readBoolean());
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
        if (!message.pruneMissingBoundEntries) {
            long nextAllowed = NEXT_ALLOWED_REQUEST_TICK.getOrDefault(playerId, 0L);
            if (now < nextAllowed) {
                return;
            }
        }
        NEXT_ALLOWED_REQUEST_TICK.put(playerId, now + REQUEST_COOLDOWN_TICKS);

        DragonCodexSavedData data = DragonCodexSavedData.get(serverLevel);
        Map<UUID, DragonEntity> loadedOwnedDragons = collectLoadedOwnedDragons(serverLevel, playerId);
        for (DragonEntity dragon : loadedOwnedDragons.values()) {
            if (!isValidCodexDragon(dragon)) {
                data.removeDragon(playerId, dragon.getUUID());
                continue;
            }
            data.addDragon(playerId, dragon);
        }

        List<DragonCodexSavedData.DragonCodexEntry> entries = data.getEntriesFor(player);

        if (!entries.isEmpty()) {
            for (DragonCodexSavedData.DragonCodexEntry entry : entries) {
                UUID dragonId = entry.dragonId();
                DragonEntity dragon = loadedOwnedDragons.get(dragonId);
                if (dragon != null && !isValidCodexDragon(dragon)) {
                    data.removeDragon(player.getUUID(), dragonId);
                    continue;
                }
                if (dragon != null && dragon.isTame() && dragon.isOwnedBy(player)) {
                    data.updateDragonName(player.getUUID(), dragonId, dragon.getName().getString());
                    data.updateDragonStats(player.getUUID(), dragon);
                    continue;
                }

                if (entry.boundInBinder()) {
                    boolean binderExists = BinderComponentUtil.isDragonBoundInLoadedWorld(serverLevel, dragonId);
                    if (binderExists) {
                        data.updateDragonBoundState(player.getUUID(), dragonId, true);
                    } else if (message.pruneMissingBoundEntries) {
                        data.removeDragon(player.getUUID(), dragonId);
                    }
                }
            }
            entries = data.getEntriesFor(player);
        }

        List<DragonCodexSavedData.DragonCodexEntry> sortedEntries = new ArrayList<>(entries);
        sortedEntries.sort(Comparator.comparing(entry -> entry.displayName().toLowerCase()));
        Map<String, Integer> nameCounts = new HashMap<>();
        List<MessageDraconicCodexList.Entry> payload = new ArrayList<>(sortedEntries.size());
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
                    entry.variantResourceId(),
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

    private static Map<UUID, DragonEntity> collectLoadedOwnedDragons(ServerLevel originLevel, UUID ownerId) {
        Map<UUID, DragonEntity> dragons = new HashMap<>();
        if (originLevel.getServer() == null || ownerId == null) {
            return dragons;
        }

        for (ServerLevel level : originLevel.getServer().getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof DragonEntity dragon && dragon.isTame() && ownerId.equals(dragon.getOwnerUUID())) {
                    dragons.put(dragon.getUUID(), dragon);
                }
            }
        }
        return dragons;
    }

    private static boolean isValidCodexDragon(DragonEntity dragon) {
        return dragon != null
                && dragon.isAlive()
                && !dragon.isDeadOrDying()
                && !dragon.isDying()
                && !dragon.isRemoved()
                && dragon.getHealth() > 0.0F;
    }
}
