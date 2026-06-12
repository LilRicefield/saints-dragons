package com.leon.saintsdragons.server.entity.npc.dialogue;

import com.leon.saintsdragons.common.network.MessageDialogueClose;
import com.leon.saintsdragons.common.network.MessageDialogueOpen;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DialogueSessionRegistry {
    private static final double MAX_INTERACTION_DISTANCE_SQR = 64.0D;
    private static final Map<UUID, DialogueSession> SESSIONS = new ConcurrentHashMap<>();

    private DialogueSessionRegistry() {
    }

    public static void start(ServerPlayer player, IvyTheDragonMerchant ivy, DialogueDefinition dialogue) {
        DialogueDefinition.Node startNode = dialogue.startNode();
        if (startNode == null) {
            NetworkHandler.sendToPlayer(player, MessageDialogueClose.INSTANCE);
            return;
        }
        endForEntity(ivy);
        SESSIONS.put(player.getUUID(), new DialogueSession(ivy.getId(), dialogue.id(), dialogue.start()));
        sendNode(player, ivy.getId(), dialogue, dialogue.start(), startNode);
    }

    public static void choose(ServerPlayer player, int entityId, int choiceIndex) {
        DialogueSession session = SESSIONS.get(player.getUUID());
        if (session == null || session.entityId() != entityId) {
            return;
        }
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof IvyTheDragonMerchant ivy) || player.distanceToSqr(entity) > MAX_INTERACTION_DISTANCE_SQR) {
            end(player);
            return;
        }
        DialogueDefinition dialogue = DialogueRegistry.get(session.dialogueId());
        if (dialogue == null) {
            end(player);
            return;
        }
        DialogueDefinition.Node currentNode = dialogue.nodes().get(session.nodeId());
        if (currentNode == null || currentNode.isEnd() || choiceIndex < 0 || choiceIndex >= currentNode.choices().size()) {
            end(player);
            return;
        }
        String nextNodeId = currentNode.choices().get(choiceIndex).next();
        DialogueDefinition.Node nextNode = dialogue.nodes().get(nextNodeId);
        if (nextNode == null) {
            end(player);
            return;
        }
        if (currentNode.opensTrade()) {
            SESSIONS.remove(player.getUUID());
            NetworkHandler.sendToPlayer(player, MessageDialogueClose.INSTANCE);
            ivy.openDialogueTrade(player, dialogue.id(), nextNodeId);
            return;
        }
        SESSIONS.put(player.getUUID(), new DialogueSession(entityId, dialogue.id(), nextNodeId));
        sendNode(player, entityId, dialogue, nextNodeId, nextNode);
    }

    public static void resume(ServerPlayer player, IvyTheDragonMerchant ivy, ResourceLocation dialogueId, String nodeId) {
        DialogueDefinition dialogue = DialogueRegistry.get(dialogueId);
        if (dialogue == null) {
            return;
        }
        DialogueDefinition.Node node = dialogue.nodes().get(nodeId);
        if (node == null || !isValidSpeaker(player, ivy)) {
            return;
        }
        endForEntity(ivy);
        SESSIONS.put(player.getUUID(), new DialogueSession(ivy.getId(), dialogue.id(), nodeId));
        sendNode(player, ivy.getId(), dialogue, nodeId, node);
    }

    public static void end(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
        NetworkHandler.sendToPlayer(player, MessageDialogueClose.INSTANCE);
    }

    public static void end(ServerPlayer player, int entityId) {
        DialogueSession session = SESSIONS.get(player.getUUID());
        if (session == null || session.entityId() != entityId) {
            return;
        }
        end(player);
    }

    public static void endForEntity(IvyTheDragonMerchant ivy) {
        for (Map.Entry<UUID, DialogueSession> entry : SESSIONS.entrySet()) {
            if (entry.getValue().entityId() != ivy.getId()) {
                continue;
            }
            ServerPlayer player = getPlayer(ivy, entry.getKey());
            if (player != null) {
                end(player);
            } else {
                SESSIONS.remove(entry.getKey());
            }
        }
    }

    public static ServerPlayer getSpeaker(IvyTheDragonMerchant ivy) {
        for (Map.Entry<UUID, DialogueSession> entry : SESSIONS.entrySet()) {
            DialogueSession session = entry.getValue();
            if (session.entityId() != ivy.getId()) {
                continue;
            }
            ServerPlayer player = getPlayer(ivy, entry.getKey());
            if (player == null || !isValidSpeaker(player, ivy)) {
                if (player != null) {
                    end(player);
                } else {
                    SESSIONS.remove(entry.getKey());
                }
                continue;
            }
            return player;
        }
        return null;
    }

    public static boolean hasSpeaker(IvyTheDragonMerchant ivy) {
        return getSpeaker(ivy) != null;
    }

    private static ServerPlayer getPlayer(IvyTheDragonMerchant ivy, UUID uuid) {
        if (ivy.level().getServer() == null) {
            return null;
        }
        return ivy.level().getServer().getPlayerList().getPlayer(uuid);
    }

    private static boolean isValidSpeaker(ServerPlayer player, IvyTheDragonMerchant ivy) {
        return player.isAlive()
                && !player.isSpectator()
                && ivy.isAlive()
                && player.level() == ivy.level()
                && player.distanceToSqr(ivy) <= MAX_INTERACTION_DISTANCE_SQR;
    }

    private static void sendNode(ServerPlayer player, int entityId, DialogueDefinition dialogue, String nodeId, DialogueDefinition.Node node) {
        NetworkHandler.sendToPlayer(player, MessageDialogueOpen.fromNode(entityId, dialogue.id(), nodeId, node));
    }
}
