package com.leon.saintsdragons.server.entity.npc.dialogue;

public record DialogueValidationResult(boolean valid, String message) {
    public static DialogueValidationResult success() {
        return new DialogueValidationResult(true, "");
    }

    public static DialogueValidationResult error(String message) {
        return new DialogueValidationResult(false, message);
    }
}
