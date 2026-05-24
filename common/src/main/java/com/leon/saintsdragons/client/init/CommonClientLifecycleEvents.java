package com.leon.saintsdragons.client.init;

import com.leon.saintsdragons.client.camera.DragonRideCameraTuning;
import com.leon.saintsdragons.client.sound.DragonDiveSoundController;
import com.leon.saintsdragons.client.sound.ignivorus.IgnivorusFireBreathSoundController;
import com.leon.saintsdragons.client.sound.raevyx.RaevyxDiveSoundController;
import com.leon.saintsdragons.client.sound.raevyx.RaevyxLightningBeamSoundController;
import com.leon.saintsdragons.client.sound.volitans.VolitansBreathSoundController;
import com.leon.saintsdragons.client.sound.volitans.VolitansBurrowSoundController;
import com.leon.saintsdragons.sound.client.DragonSoundRuntime;
import net.minecraft.client.Minecraft;

public final class CommonClientLifecycleEvents {
    private CommonClientLifecycleEvents() {
    }

    public static void bootstrap() {
        DragonRideCameraTuning.bootstrap();
    }

    public static void onEndClientTick(Minecraft minecraft) {
        DragonSoundRuntime.tick(minecraft);
        DragonDiveSoundController.tick(minecraft);
        RaevyxDiveSoundController.tick(minecraft);
        RaevyxLightningBeamSoundController.tick(minecraft);
        IgnivorusFireBreathSoundController.tick(minecraft);
        VolitansBreathSoundController.tick(minecraft);
        VolitansBurrowSoundController.tick(minecraft);
    }
}
