package com.leon.saintsdragons.client.network;

import com.leon.saintsdragons.client.DragonStatusUIManager;
import com.leon.saintsdragons.client.ui.DragonAllyScreen;
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

    public static void handleMeleeMode(MessageDragonMeleeMode message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        DragonStatusUIManager.getInstance()
                .getDragonStatusUI()
                .getMeleeModeNotification()
                .showNotification(message.mode());
    }
}
