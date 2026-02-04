package com.leon.saintsdragons.client.ui.codex;

import net.minecraft.network.chat.Component;

public enum CodexTab {
    PHYSIOLOGY("saintsdragons.gui.draconic_codex.tab.physiology",
            "saintsdragons.gui.draconic_codex.placeholder.physiology"),
    ECOLOGY("saintsdragons.gui.draconic_codex.tab.ecology",
            "saintsdragons.gui.draconic_codex.placeholder.ecology"),
    ALLY("saintsdragons.gui.draconic_codex.tab.ally",
            "saintsdragons.gui.draconic_codex.placeholder.ally");

    private final String labelKey;
    private final String descKey;

    CodexTab(String labelKey, String descKey) {
        this.labelKey = labelKey;
        this.descKey = descKey;
    }

    public Component label() {
        return Component.translatable(labelKey);
    }

    public Component description() {
        return Component.translatable(descKey);
    }
}
