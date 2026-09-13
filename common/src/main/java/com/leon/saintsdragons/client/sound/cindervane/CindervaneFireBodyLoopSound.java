package com.leon.saintsdragons.client.sound.cindervane;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public final class CindervaneFireBodyLoopSound extends AbstractTickableSoundInstance {
    private static final float VOLUME = 1.2F;
    private static final int FADE_OUT_TICKS = 10;
    private final Cindervane dragon;
    private float gain;

    public CindervaneFireBodyLoopSound(Cindervane dragon) {
        super(ModSounds.CINDERVANE_FIRE_BODY_LOOP.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.dragon = dragon;
        looping = true;
        delay = 0;
        volume = 0.001F;
        pitch = 1.0F;
        attenuation = Attenuation.LINEAR;
        updatePosition();
    }

    public Cindervane getDragon() {
        return dragon;
    }

    public static boolean isFireBodyActive(Cindervane dragon) {
        return dragon.isAlive() && !dragon.isRemoved() && !dragon.isSilent()
                && dragon.isBreathingFire() && !dragon.isInWaterOrBubble() && !dragon.isFireBodySuppressed();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().level != dragon.level() || dragon.isRemoved() || !dragon.isAlive()) {
            stop();
            return;
        }
        updatePosition();
        boolean active = isFireBodyActive(dragon);
        gain = Mth.clamp(gain + (active ? 0.25F : -1.0F / FADE_OUT_TICKS), 0.0F, 1.0F);
        volume = VOLUME * gain * gain * (3.0F - 2.0F * gain);
        if (!active && gain <= 0.0001F) stop();
    }

    private void updatePosition() {
        x = dragon.getX();
        y = dragon.getY() + dragon.getBbHeight() * 0.5;
        z = dragon.getZ();
    }
}
