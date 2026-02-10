package com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.sound.api.DragonSoundSpec;
import net.minecraft.sounds.SoundSource;

import java.util.Map;

public final class StegonautSoundProfile implements DragonSoundProfile {

    public static final StegonautSoundProfile INSTANCE = new StegonautSoundProfile();

    private static final Map<String, Boolean> BABY_ALLOWED_KEYS = Map.ofEntries(
            Map.entry("stegonaut_eat", true),
            Map.entry("stegonaut_hurt", true),
            Map.entry("stegonaut_die", true)
    );

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.ofEntries(
            Map.entry("grumble1", 54),
            Map.entry("grumble2", 83),
            Map.entry("grumble3", 60),
            Map.entry("stegonaut_hurt", 30),
            Map.entry("stegonaut_die", 55)
    );

    private StegonautSoundProfile() {
    }

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        if (dragon.isBaby() && key.startsWith("stegonaut_") && !BABY_ALLOWED_KEYS.containsKey(key)) {
            return true;
        }
        return key.startsWith("stegonaut_");
    }

    @Override
    public boolean handleSoundByName(DragonSoundHandler handler, DragonEntity dragon, String key) {
        if (dragon.isBaby()) {
            return true;
        }
        return false;
    }

    @Override
    public int getVocalAnimationWindowTicks(String key) {
        return VOCAL_WINDOWS.getOrDefault(key, -1);
    }

    @Override
    public DragonSoundSpec getVocalSpec(DragonSoundHandler handler, DragonEntity dragon, String key, DragonEntity.VocalEntry entry) {
        if (entry == null || entry.soundSupplier() == null) {
            return null;
        }
        int duration = switch (key) {
            case "grumble1", "stegonaut_grumble1" -> 54;
            case "grumble2", "stegonaut_grumble2" -> 83;
            case "grumble3", "stegonaut_grumble3" -> 60;
            default -> -1;
        };
        if (duration < 0) {
            return null;
        }
        float pitch = entry.basePitch();
        if (entry.pitchVariance() != 0f) {
            pitch += dragon.getRandom().nextFloat() * entry.pitchVariance();
        }
        return DragonSoundSpec.moving(entry.soundSupplier().get(), SoundSource.NEUTRAL, entry.volume(), pitch, duration);
    }
}
