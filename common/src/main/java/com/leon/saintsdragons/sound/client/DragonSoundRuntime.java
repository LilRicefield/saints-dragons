package com.leon.saintsdragons.sound.client;

import com.leon.saintsdragons.client.sound.DragonMovingSoundController;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public final class DragonSoundRuntime {
    private DragonSoundRuntime() {
    }

    public static void playMoving(int entityId, String soundId, float volume, float pitch, int durationTicks) {
        DragonMovingSoundController.play(entityId, soundId, volume, pitch, durationTicks);
    }

    public static void tick(Minecraft minecraft) {
        DragonMovingSoundController.tick(minecraft);
    }
}
