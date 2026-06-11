package com.leon.saintsdragons.server.entity.npc.dialogue;

import net.minecraft.resources.ResourceLocation;

public record DialogueSession(int entityId, ResourceLocation dialogueId, String nodeId) {
}
