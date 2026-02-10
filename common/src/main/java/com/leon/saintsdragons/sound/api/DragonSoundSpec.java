package com.leon.saintsdragons.sound.api;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public record DragonSoundSpec(
        DragonSoundMode mode,
        SoundEvent sound,
        SoundSource source,
        float volume,
        float pitch,
        int durationTicks
) {
    public static DragonSoundSpec world(SoundEvent sound, SoundSource source, float volume, float pitch) {
        return new DragonSoundSpec(DragonSoundMode.WORLD, sound, source, volume, pitch, 0);
    }

    public static DragonSoundSpec moving(SoundEvent sound, SoundSource source, float volume, float pitch, int durationTicks) {
        return new DragonSoundSpec(DragonSoundMode.MOVING, sound, source, volume, pitch, Math.max(1, durationTicks));
    }
}
