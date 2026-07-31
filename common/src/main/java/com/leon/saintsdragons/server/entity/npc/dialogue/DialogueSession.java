package com.leon.saintsdragons.server.entity.npc.dialogue;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public record DialogueSession(int entityId, ResourceLocation dialogueId, String nodeId, String chosenName,
                              Set<String> flags, boolean interruptionIntro) {
    public DialogueSession(int entityId, ResourceLocation dialogueId, String nodeId, String chosenName) {
        this(entityId, dialogueId, nodeId, chosenName, Set.of(), false);
    }

    public DialogueSession(int entityId, ResourceLocation dialogueId, String nodeId, String chosenName, Set<String> flags) {
        this(entityId, dialogueId, nodeId, chosenName, flags, false);
    }

    public DialogueSession {
        flags = Set.copyOf(flags);
    }
}
