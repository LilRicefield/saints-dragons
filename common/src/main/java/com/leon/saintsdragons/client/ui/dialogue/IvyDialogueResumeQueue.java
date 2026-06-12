package com.leon.saintsdragons.client.ui.dialogue;

import com.leon.saintsdragons.common.network.MessageDialogueOpen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@Environment(EnvType.CLIENT)
public final class IvyDialogueResumeQueue {
    private static final int CLEAR_SCREEN_TICKS_REQUIRED = 2;
    private static final int MAX_WAIT_TICKS = 80;

    private static MessageDialogueOpen pending;
    private static int clearScreenTicks;
    private static int waitTicks;

    private IvyDialogueResumeQueue() {
    }

    public static void openOrQueue(MessageDialogueOpen message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof IvyDialogueScreen dialogueScreen) {
            dialogueScreen.update(message);
            clearPending();
            return;
        }
        if (minecraft.screen instanceof AbstractContainerScreen<?>) {
            pending = message;
            clearScreenTicks = 0;
            waitTicks = 0;
            return;
        }
        clearPending();
        minecraft.setScreen(new IvyDialogueScreen(message));
    }

    public static void tick(Minecraft minecraft) {
        if (pending == null) {
            return;
        }
        waitTicks++;
        if (waitTicks > MAX_WAIT_TICKS) {
            clearPending();
            return;
        }
        if (minecraft.screen instanceof IvyDialogueScreen dialogueScreen) {
            dialogueScreen.update(pending);
            clearPending();
            return;
        }
        if (minecraft.screen != null) {
            clearScreenTicks = 0;
            return;
        }
        clearScreenTicks++;
        if (clearScreenTicks < CLEAR_SCREEN_TICKS_REQUIRED) {
            return;
        }
        MessageDialogueOpen message = pending;
        clearPending();
        minecraft.setScreen(new IvyDialogueScreen(message));
    }

    private static void clearPending() {
        pending = null;
        clearScreenTicks = 0;
        waitTicks = 0;
    }
}
