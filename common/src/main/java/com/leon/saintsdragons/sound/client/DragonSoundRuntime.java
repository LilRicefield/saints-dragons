package com.leon.saintsdragons.sound.client;

import com.leon.saintsdragons.client.sound.DragonMovingSoundController;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class DragonSoundRuntime {
    private DragonSoundRuntime() {
    }

    public static void playMoving(
            UUID playbackId,
            int entityId,
            UUID entityUuid,
            String soundId,
            float volume,
            float pitch,
            int durationTicks
    ) {
        DragonMovingSoundController.play(
                playbackId,
                entityId,
                entityUuid,
                soundId,
                volume,
                pitch,
                durationTicks
        );
    }

    public static void tick(Minecraft minecraft) {
        DragonMovingSoundController.tick(minecraft);
    }
}
