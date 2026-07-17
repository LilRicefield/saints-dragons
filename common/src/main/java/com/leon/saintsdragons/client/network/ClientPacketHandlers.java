package com.leon.saintsdragons.client.network;

import com.leon.saintsdragons.client.camera.ClientCameraImpulse;
import com.leon.saintsdragons.client.camera.BloodTempestKatanaVisuals;
import com.leon.saintsdragons.client.renderer.vfx.BloodTempestSonicRingTrail;
import com.leon.saintsdragons.sound.client.DragonSoundRuntime;
import com.leon.saintsdragons.client.sound.SwarmBattleMusicController;
import com.leon.saintsdragons.client.ui.DragonUIRegistry;
import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.client.ui.dialogue.IvyDialogueScreen;
import com.leon.saintsdragons.client.ui.dialogue.IvyDialogueResumeQueue;
import com.leon.saintsdragons.client.ui.codex.CodexDragonEntry;
import com.leon.saintsdragons.common.network.BloodTempestAfterimageProfile;
import com.leon.saintsdragons.common.network.MessageDraconicCodexList;
import com.leon.saintsdragons.common.network.MessageDialogueOpen;
import com.leon.saintsdragons.common.network.MessageGlobalAllyDelta;
import com.leon.saintsdragons.common.network.MessageGlobalAllyList;
import com.leon.saintsdragons.common.network.MessageDragonAbilityDebugBox;
import com.leon.saintsdragons.common.network.MessageDragonPathDebug;
import com.leon.saintsdragons.common.network.MessageDragonMeleeMode;
import com.leon.saintsdragons.common.network.MessageDragonMovingSound;
import com.leon.saintsdragons.common.network.MessageBloodTempestAfterimage;
import com.leon.saintsdragons.common.network.MessageCameraImpulse;
import com.leon.saintsdragons.common.network.MessageSwarmBattleMusic;
import com.leon.saintsdragons.common.network.MessageSwarmWaveBar;
import com.leon.saintsdragons.client.debug.DragonAbilityDebugClient;
import com.leon.saintsdragons.client.debug.DragonPathDebugClient;
import com.leon.saintsdragons.client.ui.SwarmWaveBarOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
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
                        entry.variantResourceId(),
                        entry.genderId(),
                        entry.genderKnown(),
                        entry.dragonType(),
                        entry.isBaby(),
                        entry.brushingAvailable(),
                        entry.brushingProgressPercent(),
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

    public static void handleDragonMovingSound(MessageDragonMovingSound message) {
        DragonSoundRuntime.playMoving(
                message.entityId(),
                message.soundId(),
                message.volume(),
                message.pitch(),
                message.durationTicks()
        );
    }

    public static void handleBloodTempestAfterimage(MessageBloodTempestAfterimage message) {
        com.leon.saintsdragons.client.renderer.vfx.BloodTempestAfterimageTrail.start(
                message.entityId(), message.profile(), message.origin(), message.destination());

        Minecraft minecraft = Minecraft.getInstance();
        if (message.profile() == BloodTempestAfterimageProfile.KATANA_DASH
                && message.destination() != null
                && message.origin() != null) {
            BloodTempestSonicRingTrail.start(
                    message.entityId(), message.origin(), message.destination());
            if (minecraft.player != null && minecraft.player.getId() == message.entityId()) {
                BloodTempestKatanaVisuals.startZip();
                ClientCameraImpulse.trigger(1.0F, 7);
            }
        }
    }

    public static void handleCameraImpulse(MessageCameraImpulse message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || message.radius() <= 0.0F) {
            return;
        }

        double distance = minecraft.player.position().distanceTo(message.origin());
        if (distance > message.radius()) {
            return;
        }
        float proximity = 1.0F - Mth.clamp((float) (distance / message.radius()), 0.0F, 1.0F);
        ClientCameraImpulse.trigger(message.intensity() * (0.35F + proximity * 0.65F), message.durationTicks());
    }

    public static void handleSwarmBattleMusic(MessageSwarmBattleMusic message) {
        SwarmBattleMusicController.signal(message.active(), message.durationTicks());
    }

    public static void handleSwarmWaveBar(MessageSwarmWaveBar message) {
        SwarmWaveBarOverlay.signal(message.active(), message.wave(), message.progress(), message.durationTicks());
    }

    public static void handleAbilityDebugBox(MessageDragonAbilityDebugBox message) {
        DragonAbilityDebugClient.addBox(message.box(), message.colorRgb(), message.lifetimeTicks());
    }

    public static void handleDragonPathDebug(MessageDragonPathDebug message) {
        DragonPathDebugClient.apply(message);
    }

    public static void handleDialogueOpen(MessageDialogueOpen message) {
        IvyDialogueResumeQueue.openOrQueue(message);
    }

    public static void handleDialogueClose() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof IvyDialogueScreen) {
            minecraft.setScreen(null);
        }
    }
}
