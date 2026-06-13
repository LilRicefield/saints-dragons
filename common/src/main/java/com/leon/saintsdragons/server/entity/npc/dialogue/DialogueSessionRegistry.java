package com.leon.saintsdragons.server.entity.npc.dialogue;

import com.leon.saintsdragons.common.network.MessageDialogueClose;
import com.leon.saintsdragons.common.network.MessageDialogueOpen;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DialogueSessionRegistry {
    private static final double MAX_INTERACTION_DISTANCE_SQR = 64.0D;
    private static final Map<UUID, DialogueSession> SESSIONS = new ConcurrentHashMap<>();

    private DialogueSessionRegistry() {
    }

    public static void start(ServerPlayer player, IvyTheDragonMerchant ivy, DialogueDefinition dialogue) {
        start(player, ivy, dialogue, null);
    }

    public static void start(ServerPlayer player, IvyTheDragonMerchant ivy, DialogueDefinition dialogue, String chosenName) {
        DialogueDefinition.Node startNode = dialogue.startNode();
        if (startNode == null) {
            NetworkHandler.sendToPlayer(player, MessageDialogueClose.INSTANCE);
            return;
        }
        endForEntity(ivy);
        Set<String> flags = ivy.getRememberedDialogueFlags(player);
        SESSIONS.put(player.getUUID(), new DialogueSession(ivy.getId(), dialogue.id(), dialogue.start(), chosenName, flags));
        sendNode(player, ivy.getId(), dialogue, dialogue.start(), startNode, chosenName, flags);
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
        if (currentNode != null && currentNode.requestsNameInput()) {
            return;
        }
        if (currentNode == null || currentNode.isEnd()) {
            end(player);
            return;
        }
        Set<String> currentFlags = mergedFlags(session.flags(), ivy.getRememberedDialogueFlags(player));
        List<DialogueDefinition.Choice> visibleChoices = visibleChoices(currentNode, currentFlags);
        if (choiceIndex < 0 || choiceIndex >= visibleChoices.size()) {
            end(player);
            return;
        }
        DialogueDefinition.Choice choice = visibleChoices.get(choiceIndex);
        Set<String> nextFlags = withChoiceFlag(currentFlags, choice);
        if (!choice.setFlag().isBlank()) {
            ivy.rememberDialogueFlag(player, choice.setFlag());
        }
        if (!choice.impression().isBlank()) {
            ivy.rememberDialogueImpression(player, choice.impression());
        }
        ResolvedTarget target = resolveTarget(dialogue, choice.next());
        if (target == null) {
            end(player);
            return;
        }
        if (currentNode.opensTrade()) {
            SESSIONS.remove(player.getUUID());
            NetworkHandler.sendToPlayer(player, MessageDialogueClose.INSTANCE);
            ivy.openDialogueTrade(player, target.dialogue().id(), target.nodeId());
            return;
        }
        if (target.node().usesPlayerName()) {
            String chosenName = player.getGameProfile().getName();
            ivy.rememberDialogueName(player, chosenName);
            continueWithChosenName(player, entityId, target.dialogue(), target.node(), chosenName, nextFlags);
            return;
        }
        SESSIONS.put(player.getUUID(), new DialogueSession(entityId, target.dialogue().id(), target.nodeId(), session.chosenName(), nextFlags));
        sendNode(player, entityId, target.dialogue(), target.nodeId(), target.node(), session.chosenName(), nextFlags);
    }

    public static void submitName(ServerPlayer player, int entityId, String rawName) {
        DialogueSession session = SESSIONS.get(player.getUUID());
        if (session == null || session.entityId() != entityId) {
            return;
        }
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof IvyTheDragonMerchant ivy) || !isValidSpeaker(player, ivy)) {
            end(player);
            return;
        }
        DialogueDefinition dialogue = DialogueRegistry.get(session.dialogueId());
        if (dialogue == null) {
            end(player);
            return;
        }
        DialogueDefinition.Node currentNode = dialogue.nodes().get(session.nodeId());
        if (currentNode == null || !currentNode.requestsNameInput() || currentNode.choices().isEmpty()) {
            end(player);
            return;
        }
        String name = sanitizeName(rawName);
        if (name.isBlank()) {
            name = player.getGameProfile().getName();
        }
        ivy.rememberDialogueName(player, name);
        continueWithChosenName(player, entityId, dialogue, currentNode, name, session.flags());
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
        String chosenName = ivy.getRememberedDialogueName(player);
        Set<String> flags = ivy.getRememberedDialogueFlags(player);
        SESSIONS.put(player.getUUID(), new DialogueSession(ivy.getId(), dialogue.id(), nodeId, chosenName, flags));
        sendNode(player, ivy.getId(), dialogue, nodeId, node, chosenName, flags);
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

    private static void continueWithChosenName(ServerPlayer player,
                                               int entityId,
                                               DialogueDefinition dialogue,
                                               DialogueDefinition.Node actionNode,
                                               String chosenName,
                                               Set<String> flags) {
        List<DialogueDefinition.Choice> choices = visibleChoices(actionNode, flags);
        if (choices.isEmpty()) {
            end(player);
            return;
        }
        DialogueDefinition.Choice choice = choices.get(0);
        Set<String> nextFlags = withChoiceFlag(flags, choice);
        Entity entity = player.level().getEntity(entityId);
        if (entity instanceof IvyTheDragonMerchant ivy && !choice.setFlag().isBlank()) {
            ivy.rememberDialogueFlag(player, choice.setFlag());
        }
        ResolvedTarget target = resolveTarget(dialogue, choice.next());
        if (target == null) {
            end(player);
            return;
        }
        SESSIONS.put(player.getUUID(), new DialogueSession(entityId, target.dialogue().id(), target.nodeId(), chosenName, nextFlags));
        sendNode(player, entityId, target.dialogue(), target.nodeId(), target.node(), chosenName, nextFlags);
    }

    private static ResolvedTarget resolveTarget(DialogueDefinition currentDialogue, String next) {
        DialogueTargetReference reference = DialogueTargetReference.parse(currentDialogue.id(), next);
        if (reference == null) {
            return null;
        }
        DialogueDefinition dialogue = reference.external() ? DialogueRegistry.get(reference.dialogueId()) : currentDialogue;
        if (dialogue == null) {
            return null;
        }
        DialogueDefinition.Node node = dialogue.nodes().get(reference.nodeId());
        return node == null ? null : new ResolvedTarget(dialogue, reference.nodeId(), node);
    }

    private static void sendNode(ServerPlayer player,
                                 int entityId,
                                 DialogueDefinition dialogue,
                                 String nodeId,
                                 DialogueDefinition.Node node,
                                 String chosenName,
                                 Set<String> flags) {
        Component selectedText = node.selectText();
        DialogueDefinition.Node resolvedNode = new DialogueDefinition.Node(
                node.speaker(),
                chosenName == null ? selectedText : resolveName(selectedText, chosenName),
                visibleChoices(node, flags),
                node.type()
        );
        NetworkHandler.sendToPlayer(player, MessageDialogueOpen.fromNode(entityId, dialogue.id(), nodeId, resolvedNode));
    }

    private static List<DialogueDefinition.Choice> visibleChoices(DialogueDefinition.Node node, Set<String> flags) {
        return node.choices().stream()
                .filter(choice -> choice.requiresFlag().isBlank() || flags.contains(choice.requiresFlag()))
                .filter(choice -> choice.hiddenIfFlag().isBlank() || !flags.contains(choice.hiddenIfFlag()))
                .filter(choice -> choice.hiddenIfAllFlags().isEmpty() || !flags.containsAll(choice.hiddenIfAllFlags()))
                .toList();
    }

    private static Set<String> withChoiceFlag(Set<String> flags, DialogueDefinition.Choice choice) {
        if (choice.setFlag().isBlank() || flags.contains(choice.setFlag())) {
            return flags;
        }
        Set<String> nextFlags = new HashSet<>(flags);
        nextFlags.add(choice.setFlag());
        return nextFlags;
    }

    private static Set<String> mergedFlags(Set<String> sessionFlags, Set<String> rememberedFlags) {
        if (rememberedFlags.isEmpty()) {
            return sessionFlags;
        }
        if (sessionFlags.isEmpty()) {
            return rememberedFlags;
        }
        Set<String> flags = new HashSet<>(sessionFlags);
        flags.addAll(rememberedFlags);
        return flags;
    }

    private static Component resolveName(Component component, String chosenName) {
        String text = component.getString();
        if (!text.contains("{name}")) {
            return component;
        }
        return Component.literal(text.replace("{name}", chosenName));
    }

    private static String sanitizeName(String rawName) {
        StringBuilder builder = new StringBuilder();
        int nonWhitespace = 0;
        for (int offset = 0; offset < rawName.length(); ) {
            int codePoint = rawName.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (!Character.isISOControl(codePoint)) {
                builder.appendCodePoint(codePoint);
                if (!Character.isWhitespace(codePoint)) {
                    nonWhitespace++;
                    if (nonWhitespace >= 50) {
                        break;
                    }
                }
            }
        }
        return builder.toString().trim().replaceAll("\\s+", " ");
    }

    private record ResolvedTarget(DialogueDefinition dialogue, String nodeId, DialogueDefinition.Node node) {
    }
}
