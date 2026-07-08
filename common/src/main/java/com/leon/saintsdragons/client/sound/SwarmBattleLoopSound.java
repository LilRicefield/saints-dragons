package com.leon.saintsdragons.client.sound;

import com.leon.saintsdragons.common.registry.ModSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

@Environment(EnvType.CLIENT)
public class SwarmBattleLoopSound extends AbstractTickableSoundInstance {
    private static final float MAX_VOLUME = 0.8F;
    private static final float FADE_IN_RATE = 0.035F;
    private static final float FADE_OUT_RATE = 0.025F;

    private float fade;

    public SwarmBattleLoopSound() {
        super(ModSounds.SWARM_BATTLE_LOOP.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stop();
            return;
        }

        float target = SwarmBattleMusicController.isBattleActive(minecraft) ? 1.0F : 0.0F;
        if (target > 0.0F) {
            this.fade = Math.min(1.0F, this.fade + FADE_IN_RATE);
        } else {
            this.fade = Math.max(0.0F, this.fade - FADE_OUT_RATE);
        }

        if (this.fade <= 0.0F && target <= 0.0F) {
            stop();
            return;
        }

        this.volume = this.fade * MAX_VOLUME;
        this.pitch = 1.0F;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
