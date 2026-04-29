package com.leon.saintsdragons.client.sound.volitans;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class VolitansBreathLoopSound extends AbstractTickableSoundInstance {
    private static final int LOOP_SEGMENT_TICKS = 800; // 40s at 20 TPS
    private final Volitans dragon;
    private int ageTicks;

    public VolitansBreathLoopSound(Volitans dragon) {
        super(ModSounds.VOLITANS_BREATHING.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.dragon = dragon;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.4f;
        this.pitch = 1.0f;
        this.attenuation = Attenuation.LINEAR;
        updatePosition();
    }

    @Override
    public void tick() {
        if (dragon == null || dragon.isRemoved() || !dragon.isAlive()) {
            stop();
            return;
        }
        if (!dragon.isBreathing()) {
            stop();
            return;
        }
        if (++ageTicks >= LOOP_SEGMENT_TICKS) {
            stop();
            return;
        }
        updatePosition();
    }

    private void updatePosition() {
        Vec3 start = dragon.getBreathOrigin();
        if (start == null) {
            start = dragon.position().add(0.0D, dragon.getBbHeight() * 0.72D, 0.9D);
        }
        this.x = start.x;
        this.y = start.y;
        this.z = start.z;
    }
}
