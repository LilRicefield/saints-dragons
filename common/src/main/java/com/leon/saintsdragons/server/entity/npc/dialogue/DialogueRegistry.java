package com.leon.saintsdragons.server.entity.npc.dialogue;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DialogueRegistry {
    private static final Map<ResourceLocation, DialogueDefinition> DEFINITIONS = new ConcurrentHashMap<>();

    private DialogueRegistry() {
    }

    public static void replaceDatapackDialogues(Map<ResourceLocation, DialogueDefinition> dialogues) {
        DEFINITIONS.clear();
        DEFINITIONS.putAll(dialogues);
    }

    @Nullable
    public static DialogueDefinition get(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }
}
