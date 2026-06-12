package com.leon.saintsdragons.server.entity.npc.dialogue;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

record DialogueTargetReference(ResourceLocation dialogueId, String nodeId, boolean external) {
    @Nullable
    static DialogueTargetReference parse(ResourceLocation currentDialogueId, String value) {
        int split = value.indexOf('#');
        if (split < 0) {
            return new DialogueTargetReference(currentDialogueId, value, false);
        }
        if (split == 0 || split == value.length() - 1) {
            return null;
        }
        ResourceLocation dialogueId = ResourceLocation.tryParse(value.substring(0, split));
        if (dialogueId == null) {
            return null;
        }
        return new DialogueTargetReference(dialogueId, value.substring(split + 1), true);
    }
}
