package com.leon.saintsdragons.client.init;

import com.leon.saintsdragons.client.camera.DragonRideCameraTuning;
import com.leon.saintsdragons.client.input.DragonlordDoubleJumpInput;
import com.leon.saintsdragons.client.renderer.vfx.BloodTempestAfterimageTrail;
import com.leon.saintsdragons.client.sound.DragonDiveSoundController;
import com.leon.saintsdragons.client.sound.SwarmBattleMusicController;
import com.leon.saintsdragons.client.sound.ignivorus.IgnivorusFireBreathSoundController;
import com.leon.saintsdragons.client.sound.raevyx.RaevyxDiveSoundController;
import com.leon.saintsdragons.client.sound.raevyx.RaevyxLightningBeamSoundController;
import com.leon.saintsdragons.client.sound.volitans.VolitansBreathSoundController;
import com.leon.saintsdragons.client.sound.volitans.VolitansBurrowSoundController;
import com.leon.saintsdragons.client.ui.SwarmWaveBarOverlay;
import com.leon.saintsdragons.client.ui.dialogue.IvyDialogueResumeQueue;
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
        SwarmBattleMusicController.tick(minecraft);
        SwarmWaveBarOverlay.tick();
        DragonDiveSoundController.tick(minecraft);
        RaevyxDiveSoundController.tick(minecraft);
        RaevyxLightningBeamSoundController.tick(minecraft);
        IgnivorusFireBreathSoundController.tick(minecraft);
        VolitansBreathSoundController.tick(minecraft);
        VolitansBurrowSoundController.tick(minecraft);
        IvyDialogueResumeQueue.tick(minecraft);
        DragonlordDoubleJumpInput.clientTick(minecraft);
        BloodTempestAfterimageTrail.tick(minecraft);
    }
}
