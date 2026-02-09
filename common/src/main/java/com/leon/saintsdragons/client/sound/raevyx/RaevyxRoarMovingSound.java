package com.leon.saintsdragons.client.sound.raevyx;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class RaevyxRoarMovingSound extends AbstractTickableSoundInstance {
    private static final int MAX_LIFETIME_TICKS = 74;

    private final Raevyx raevyx;
    private int lifeTicks = 0;

    public RaevyxRoarMovingSound(Raevyx raevyx, float volume, float pitch) {
        super(ModSounds.RAEVYX_ROAR.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.raevyx = raevyx;
        this.looping = false;
        this.delay = 0;
        this.volume = volume;
        this.pitch = pitch;
        this.attenuation = Attenuation.LINEAR;
        updatePosition();
    }

    public Raevyx getRaevyx() {
        return raevyx;
    }

    @Override
    public void tick() {
        if (raevyx == null || raevyx.isRemoved() || !raevyx.isAlive()) {
            stop();
            return;
        }
        lifeTicks++;
        if (lifeTicks >= MAX_LIFETIME_TICKS) {
            stop();
            return;
        }
        updatePosition();
    }

    private void updatePosition() {
        Vec3 mouth = raevyx.getClientLocatorPosition("mouth_origin");
        if (mouth == null) {
            mouth = raevyx.computeHeadMouthOrigin(1.0f);
        }
        if (mouth == null) {
            mouth = raevyx.position().add(0.0D, raevyx.getBbHeight() * 0.65D, 0.0D);
        }
        this.x = mouth.x;
        this.y = mouth.y;
        this.z = mouth.z;
    }
}

