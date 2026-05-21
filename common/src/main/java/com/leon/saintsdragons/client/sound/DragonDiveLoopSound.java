package com.leon.saintsdragons.client.sound;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

@Environment(EnvType.CLIENT)
public class DragonDiveLoopSound extends AbstractTickableSoundInstance {
    private static final float MAX_VOLUME = 0.95F;
    private static final float FADE_IN_RATE = 0.12F;
    private static final float FADE_OUT_RATE = 0.08F;

    private final RideableDragonBase dragon;
    private float fade;

    public DragonDiveLoopSound(RideableDragonBase dragon) {
        super(ModSounds.DRAGON_DIVE_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.dragon = dragon;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (dragon == null || dragon.isRemoved() || !dragon.isAlive()) {
            stop();
            return;
        }

        float target = DragonDiveSoundController.getDiveSoundIntensity(dragon);
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
        this.x = dragon.getX();
        this.y = dragon.getY() + dragon.getBbHeight() * 0.5D;
        this.z = dragon.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
