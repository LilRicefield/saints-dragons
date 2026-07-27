package com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.sound.api.DragonSoundSpec;
import net.minecraft.sounds.SoundSource;

import java.util.Map;

public final class AtroxiiaSoundProfile implements DragonSoundProfile {
    public static final AtroxiiaSoundProfile INSTANCE = new AtroxiiaSoundProfile();

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.of(
            "grumble1", 20,
            "grumble2", 20,
            "grumble3", 30,
            "atroxiia_flex", 80
    );

    private AtroxiiaSoundProfile() {
    }

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon,
                                        String key, String locator) {
        return "atroxiia_flex".equals(key);
    }

    @Override
    public int getVocalAnimationWindowTicks(String key) {
        return VOCAL_WINDOWS.getOrDefault(key, -1);
    }

    @Override
    public DragonSoundSpec getVocalSpec(DragonSoundHandler handler, DragonEntity dragon,
                                        String key, DragonEntity.VocalEntry entry) {
        if (entry == null || entry.soundSupplier() == null) {
            return null;
        }
        int duration = VOCAL_WINDOWS.getOrDefault(key, -1);
        if (duration < 0) {
            return null;
        }
        float pitch = entry.basePitch();
        if (entry.pitchVariance() != 0.0F) {
            pitch += dragon.getRandom().nextFloat() * entry.pitchVariance();
        }
        return DragonSoundSpec.moving(
                entry.soundSupplier().get(), SoundSource.NEUTRAL, entry.volume(), pitch, duration);
    }
}
