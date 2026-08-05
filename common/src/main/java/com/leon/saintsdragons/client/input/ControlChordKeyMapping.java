package com.leon.saintsdragons.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ControlChordKeyMapping extends KeyMapping {
    public ControlChordKeyMapping(String name, InputConstants.Type type, int keyCode, String category) {
        super(name, type, keyCode, category);
    }

    @Override
    public boolean isDown() {
        if (!super.isDown()) {
            return false;
        }
        if (!usesDefaultChord()) {
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }

        long window = minecraft.getWindow().getWindow();
        boolean controlDown = InputConstants.isKeyDown(window, InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(window, InputConstants.KEY_RCONTROL);
        return controlDown;
    }

    @Override
    public Component getTranslatedKeyMessage() {
        Component keyName = InputConstants.getKey(saveString()).getDisplayName();
        if (!usesDefaultChord()) {
            return keyName;
        }
        return Component.literal("CTRL + ")
                .append(keyName);
    }

    @Override
    public boolean same(KeyMapping other) {
        if (!usesDefaultChord()) {
            return saveString().equals(other.saveString());
        }
        return other instanceof ControlChordKeyMapping && super.same(other);
    }

    private boolean usesDefaultChord() {
        return saveString().equals(getDefaultKey().getName());
    }
}
