package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.entity.npc.dialogue.DialogueDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record MessageDialogueOpen(int entityId, ResourceLocation dialogueId, String nodeId, String nodeType,
                                  Component speaker, Component text,
                                  List<Choice> choices) {
    public MessageDialogueOpen {
        choices = List.copyOf(choices);
    }

    public static MessageDialogueOpen fromNode(int entityId, ResourceLocation dialogueId, String nodeId, DialogueDefinition.Node node) {
        List<Choice> choices = new ArrayList<>();
        for (DialogueDefinition.Choice choice : node.choices()) {
            choices.add(new Choice(choice.text(), choice.next()));
        }
        return new MessageDialogueOpen(entityId, dialogueId, nodeId, node.type().name(), node.speaker(), node.text(), choices);
    }

    public static void encode(MessageDialogueOpen message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeResourceLocation(message.dialogueId);
        buffer.writeUtf(message.nodeId, 128);
        buffer.writeUtf(message.nodeType, 32);
        buffer.writeComponent(message.speaker);
        buffer.writeComponent(message.text);
        buffer.writeInt(message.choices.size());
        for (Choice choice : message.choices) {
            buffer.writeComponent(choice.text());
            buffer.writeUtf(choice.next(), 128);
        }
    }

    public static MessageDialogueOpen decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        ResourceLocation dialogueId = buffer.readResourceLocation();
        String nodeId = buffer.readUtf(128);
        String nodeType = buffer.readUtf(32);
        Component speaker = buffer.readComponent();
        Component text = buffer.readComponent();
        int size = buffer.readInt();
        List<Choice> choices = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            choices.add(new Choice(buffer.readComponent(), buffer.readUtf(128)));
        }
        return new MessageDialogueOpen(entityId, dialogueId, nodeId, nodeType, speaker, text, choices);
    }

    public static void handle(MessageDialogueOpen message) {
        Services.PLATFORM.runOnClient(() -> com.leon.saintsdragons.client.network.ClientPacketHandlers.handleDialogueOpen(message));
    }

    public record Choice(Component text, String next) {
    }
}
