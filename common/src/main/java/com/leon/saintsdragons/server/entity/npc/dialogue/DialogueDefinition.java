package com.leon.saintsdragons.server.entity.npc.dialogue;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

public record DialogueDefinition(ResourceLocation id, String start, Map<String, Node> nodes) {
    public Node startNode() {
        return nodes.get(start);
    }

    public record Node(Component speaker, Component text, List<Choice> choices, Type type) {
        public Node {
            choices = List.copyOf(choices);
        }

        public boolean isEnd() {
            return type == Type.END_DIALOGUE;
        }
    }

    public record Choice(Component text, String next) {
    }

    public enum Type {
        DEFAULT,
        END_DIALOGUE;

        public static Type byName(String name) {
            if ("end_dialogue".equalsIgnoreCase(name)) {
                return END_DIALOGUE;
            }
            return DEFAULT;
        }
    }
}
