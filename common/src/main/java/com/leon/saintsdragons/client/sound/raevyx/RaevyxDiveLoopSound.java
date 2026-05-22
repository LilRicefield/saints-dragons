package com.leon.saintsdragons.client.sound.raevyx;

import com.leon.saintsdragons.client.camera.DragonDiveEffectIntensity;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

@Environment(EnvType.CLIENT)
public class RaevyxDiveLoopSound extends AbstractTickableSoundInstance {
    private static final float MAX_VOLUME = 1.35F;
    private static final float FADE_IN_RATE = 0.14F;
    private static final float FADE_OUT_RATE = 0.09F;

    private final Raevyx raevyx;
    private float fade;

    public RaevyxDiveLoopSound(Raevyx raevyx) {
        super(ModSounds.RAEVYX_DIVE_LOOP.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.raevyx = raevyx;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.attenuation = Attenuation.LINEAR;
        updatePosition();
    }

    @Override
    public void tick() {
        if (raevyx == null || raevyx.isRemoved() || !raevyx.isAlive()) {
            stop();
            return;
        }

        float target = DragonDiveEffectIntensity.get(raevyx);
        if (target > 0.0F) {
            fade = Math.min(target, fade + FADE_IN_RATE);
        } else {
            fade = Math.max(0.0F, fade - FADE_OUT_RATE);
        }

        if (fade <= 0.0F && target <= 0.0F) {
            stop();
            return;
        }

        this.volume = fade * MAX_VOLUME;
        this.pitch = 0.92F + fade * 0.18F;
        updatePosition();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    private void updatePosition() {
        this.x = raevyx.getX();
        this.y = raevyx.getY() + raevyx.getBbHeight() * 0.55D;
        this.z = raevyx.getZ();
    }
}
