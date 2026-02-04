package com.leon.saintsdragons.client.ui.codex;

public record CodexDragonEntry(java.util.UUID entityId, String displayName, double currentHealth, double maxHealth,
                               double armor, double hunger, double happiness, int variantId, byte genderId,
                               boolean genderKnown, String dragonType, boolean isBaby) {
}
