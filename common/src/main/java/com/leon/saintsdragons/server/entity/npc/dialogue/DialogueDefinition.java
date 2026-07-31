package com.leon.saintsdragons.server.entity.npc.dialogue;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public record DialogueDefinition(ResourceLocation id, String start, Map<String, Node> nodes, Resume resume) {
    public DialogueDefinition(ResourceLocation id, String start, Map<String, Node> nodes) {
        this(id, start, nodes, Resume.DISABLED);
    }

    public Node startNode() {
        return nodes.get(start);
    }

    public record Resume(List<Component> texts, List<String> requiresAllFlags) {
        private static final Resume DISABLED = new Resume(List.of(), List.of());

        public Resume {
            texts = List.copyOf(texts);
            requiresAllFlags = List.copyOf(requiresAllFlags);
        }

        public boolean enabled() {
            return !texts.isEmpty();
        }

        public Component selectText() {
            return texts.get(ThreadLocalRandom.current().nextInt(texts.size()));
        }
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

        public boolean opensInventory() {
            return type == Type.OPEN_INVENTORY;
        }

        public boolean usesPlayerName() {
            return type == Type.USE_PLAYER_NAME;
        }

        public boolean requestsNameInput() {
            return type == Type.NAME_INPUT;
        }

        public boolean recruitsIvy() {
            return type == Type.RECRUIT_IVY;
        }

        public boolean givesIvyPlushie() {
            return type == Type.GIVE_IVY_PLUSHIE;
        }

        public String ivyAnimationTrigger() {
            return type.ivyAnimationTrigger();
        }
    }

    public record Choice(Component text, String next, String impression, String setFlag, String requiresFlag, String hiddenIfFlag,
                         List<String> hiddenIfAllFlags, List<String> requiresAllFlags) {
        public Choice(Component text, String next) {
            this(text, next, "");
        }

        public Choice(Component text, String next, String impression) {
            this(text, next, impression, "", "", "", List.of(), List.of());
        }

        public Choice {
            hiddenIfAllFlags = List.copyOf(hiddenIfAllFlags);
            requiresAllFlags = List.copyOf(requiresAllFlags);
        }
    }

    public enum Type {
        DEFAULT,
        END_DIALOGUE,
        OPEN_TRADE,
        OPEN_INVENTORY,
        USE_PLAYER_NAME,
        NAME_INPUT,
        RECRUIT_IVY,
        GIVE_IVY_PLUSHIE,
        IVY_EMBARRASSED,
        IVY_SIGH,
        IVY_HMM_TRADER,
        IVY_HMM_GARDENER,
        IVY_HMM_DRAGON_ADVICE,
        IVY_HMM_EXIT_TO_IDLE;

        public String ivyAnimationTrigger() {
            return switch (this) {
                case IVY_EMBARRASSED -> "embarrassed";
                case IVY_SIGH -> "sigh";
                case IVY_HMM_TRADER -> "hmm_trader";
                case IVY_HMM_GARDENER -> "hmm_gardener";
                case IVY_HMM_DRAGON_ADVICE -> "hmm_dragon_advice";
                case IVY_HMM_EXIT_TO_IDLE -> "hmm_dragon_advice_exit_to_idle";
                default -> "";
            };
        }

        public static Type byName(String name) {
            if ("end_dialogue".equalsIgnoreCase(name)) {
                return END_DIALOGUE;
            }
            if ("open_trade".equalsIgnoreCase(name)) {
                return OPEN_TRADE;
            }
            if ("open_inventory".equalsIgnoreCase(name)) {
                return OPEN_INVENTORY;
            }
            if ("use_player_name".equalsIgnoreCase(name)) {
                return USE_PLAYER_NAME;
            }
            if ("name_input".equalsIgnoreCase(name)) {
                return NAME_INPUT;
            }
            if ("recruit_ivy".equalsIgnoreCase(name)) {
                return RECRUIT_IVY;
            }
            if ("give_ivy_plushie".equalsIgnoreCase(name)) {
                return GIVE_IVY_PLUSHIE;
            }
            if ("ivy_embarrassed".equalsIgnoreCase(name)) {
                return IVY_EMBARRASSED;
            }
            if ("ivy_sigh".equalsIgnoreCase(name)) {
                return IVY_SIGH;
            }
            if ("ivy_hmm_trader".equalsIgnoreCase(name)) {
                return IVY_HMM_TRADER;
            }
            if ("ivy_hmm_gardener".equalsIgnoreCase(name)) {
                return IVY_HMM_GARDENER;
            }
            if ("ivy_hmm_dragon_advice".equalsIgnoreCase(name)) {
                return IVY_HMM_DRAGON_ADVICE;
            }
            if ("ivy_hmm_exit_to_idle".equalsIgnoreCase(name)) {
                return IVY_HMM_EXIT_TO_IDLE;
            }
            return DEFAULT;
        }
    }
}
