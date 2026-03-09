package com.leon.saintsdragons.client.sound.volitans;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

@Environment(EnvType.CLIENT)
public class VolitansBurrowLoopSound extends AbstractTickableSoundInstance {
    private static final int LOOP_SEGMENT_TICKS = 400; // 20s

    public enum Mode {
        IDLE,
        MOVE
    }

    private final Volitans dragon;
    private final Mode mode;
    private int ageTicks;

    public VolitansBurrowLoopSound(Volitans dragon, Mode mode) {
        super(resolveSound(mode), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.dragon = dragon;
        this.mode = mode;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.8f;
        this.pitch = 1.0f;
        this.attenuation = Attenuation.LINEAR;
        updatePosition();
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public void tick() {
        if (dragon == null || dragon.isRemoved() || !dragon.isAlive()) {
            stop();
            return;
        }
        if (!dragon.isBurrowing()) {
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
        this.x = dragon.getX();
        this.y = dragon.getY() + dragon.getBbHeight() * 0.55D;
        this.z = dragon.getZ();
    }

    private static SoundEvent resolveSound(Mode mode) {
        return mode == Mode.MOVE ? ModSounds.VOLITANS_BURROW_MOVE.get() : ModSounds.VOLITANS_BURROW_IDLE.get();
    }
}
