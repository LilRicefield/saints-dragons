package com.leon.saintsdragons.server.entity.npc.dialogue;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public record DialogueDefinition(ResourceLocation id, String start, Map<String, Node> nodes) {
    public Node startNode() {
        return nodes.get(start);
    }

    public record Node(Component speaker, Component text, List<Component> texts, List<Choice> choices, Type type) {
        public Node(Component speaker, Component text, List<Choice> choices, Type type) {
            this(speaker, text, List.of(), choices, type);
        }

        public Node {
            texts = List.copyOf(texts);
            choices = List.copyOf(choices);
        }

        public Component selectText() {
            if (texts.isEmpty()) {
                return text;
            }
            return texts.get(ThreadLocalRandom.current().nextInt(texts.size()));
        }

        public boolean isEnd() {
            return type == Type.END_DIALOGUE;
        }

        public boolean opensTrade() {
            return type == Type.OPEN_TRADE;
        }

        public boolean usesPlayerName() {
            return type == Type.USE_PLAYER_NAME;
        }

        public boolean requestsNameInput() {
            return type == Type.NAME_INPUT;
        }
    }

    public record Choice(Component text, String next, String impression) {
        public Choice(Component text, String next) {
            this(text, next, "");
        }
    }

    public enum Type {
        DEFAULT,
        END_DIALOGUE,
        OPEN_TRADE,
        USE_PLAYER_NAME,
        NAME_INPUT;

        public static Type byName(String name) {
            if ("end_dialogue".equalsIgnoreCase(name)) {
                return END_DIALOGUE;
            }
            if ("open_trade".equalsIgnoreCase(name)) {
                return OPEN_TRADE;
            }
            if ("use_player_name".equalsIgnoreCase(name)) {
                return USE_PLAYER_NAME;
            }
            if ("name_input".equalsIgnoreCase(name)) {
                return NAME_INPUT;
            }
            return DEFAULT;
        }
    }
}
