package com.leon.saintsdragons.client.network;

import com.leon.saintsdragons.client.ui.DragonUIRegistry;
import com.leon.saintsdragons.client.ui.DragonAllyScreen;
import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.client.ui.codex.CodexDragonEntry;
import com.leon.saintsdragons.common.network.MessageDraconicCodexList;
import com.leon.saintsdragons.common.network.MessageGlobalAllyDelta;
import com.leon.saintsdragons.common.network.MessageGlobalAllyList;
import com.leon.saintsdragons.common.network.MessageDragonAllyDelta;
import com.leon.saintsdragons.common.network.MessageDragonAllyList;
import com.leon.saintsdragons.common.network.MessageDragonMeleeMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void handleAllyList(MessageDragonAllyList message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DragonAllyScreen allyScreen) {
            allyScreen.updateAllyList(message.allyList());
        }
    }

    public static void handleAllyDelta(MessageDragonAllyDelta message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DragonAllyScreen allyScreen) {
            if (message.isAdd()) {
                allyScreen.addAlly(message.username());
            } else {
                allyScreen.removeAlly(message.username());
            }
        }
    }

    public static void handleDraconicCodexList(MessageDraconicCodexList message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DraconicCodexScreen codexScreen) {
            java.util.List<CodexDragonEntry> entries = new java.util.ArrayList<>();
            for (MessageDraconicCodexList.Entry entry : message.entries()) {
                entries.add(new CodexDragonEntry(
                        entry.entityId(),
                        entry.displayName(),
                        entry.currentHealth(),
                        entry.maxHealth(),
                        entry.armor(),
                        entry.hunger(),
                        entry.happiness(),
                        entry.variantId(),
                        entry.genderId(),
                        entry.genderKnown(),
                        entry.dragonType(),
                        entry.isBaby(),
                        entry.posX(),
                        entry.posY(),
                        entry.posZ(),
                        entry.biomeId()
                ));
            }
            codexScreen.updateDragonList(entries);
        }
    }

    public static void handleGlobalAllyList(MessageGlobalAllyList message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DraconicCodexScreen codexScreen) {
            codexScreen.updateAllyList(message.allyList());
        }
    }

    public static void handleGlobalAllyDelta(MessageGlobalAllyDelta message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DraconicCodexScreen codexScreen) {
            if (message.isAdd()) {
                codexScreen.addAlly(message.username());
            } else {
                codexScreen.removeAlly(message.username());
            }
        }
    }

    public static void handleMeleeMode(MessageDragonMeleeMode message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        DragonUIRegistry.getMeleeModeNotification()
                .showNotification(message.mode());
    }
}
