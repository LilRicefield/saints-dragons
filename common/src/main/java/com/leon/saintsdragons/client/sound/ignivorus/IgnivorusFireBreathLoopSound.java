package com.leon.saintsdragons.client.sound.ignivorus;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side looping sound that tracks the Ignivorus fire breath cone.
 * Starts when the fire breath flag toggles on and stops as soon as the flag clears.
 */
@Environment(EnvType.CLIENT)
public class IgnivorusFireBreathLoopSound extends AbstractTickableSoundInstance {
    private final Ignivorus dragon;

    public IgnivorusFireBreathLoopSound(Ignivorus dragon) {
        super(ModSounds.IGNIVORUS_FIRE_BREATHING.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.dragon = dragon;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.45f;
        this.pitch = 1.0f;
        this.attenuation = Attenuation.LINEAR;
        updatePosition();
    }

    public Ignivorus getDragon() {
        return dragon;
    }

    @Override
    public void tick() {
        if (dragon == null || dragon.isRemoved() || !dragon.isAlive()) {
            stop();
            return;
        }
        if (!dragon.isBreathingFire()) {
            stop();
            return;
        }
        updatePosition();
    }

    private void updatePosition() {
        Vec3 start = dragon.getFireBreathStart();
        if (start == null) {
            start = dragon.position().add(0.0D, dragon.getBbHeight() * 0.65D, 0.0D);
        }
        this.x = start.x;
        this.y = start.y;
        this.z = start.z;

        // Scale volume slightly with dragon size so bigger variants sound heavier.
        float sizeScale = (float) Math.max(1.0D, dragon.getBbWidth());
        this.volume = 1.2f + (sizeScale * 0.2f);
    }
}
