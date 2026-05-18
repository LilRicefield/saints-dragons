package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.sound.api.DragonSoundSpec;
import net.minecraft.sounds.SoundSource;

import java.util.Map;

public final class RaevyxSoundProfile implements DragonSoundProfile {

    public static final RaevyxSoundProfile INSTANCE = new RaevyxSoundProfile();
    private static final float BABY_PITCH_MULTIPLIER = 1.6f;

    private static final Map<String, Boolean> BABY_ALLOWED_KEYS = Map.ofEntries(
            Map.entry("raevyx_eat", true),
            Map.entry("raevyx_hurt", true),
            Map.entry("raevyx_die", true)
    );

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.ofEntries(
            Map.entry("grumble1", 52),
            Map.entry("grumble2", 72),
            Map.entry("grumble3", 43),
            Map.entry("roar", 112),
            Map.entry("roar_ground", 112),
            Map.entry("roar_air", 112),
            Map.entry("flex", 80),
            Map.entry("raevyx_hurt", 20),
            Map.entry("raevyx_die", 62)
    );

    private RaevyxSoundProfile() {
    }

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        if (dragon.isBaby() && !BABY_ALLOWED_KEYS.containsKey(key)) {
            return true;
        }
        if (key.startsWith("raevyx_flap") || key.startsWith("flap")) {
            playWingFlap(handler, dragon);
            return true;
        }
        return key.startsWith("raevyx_");
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
        if (dragon.isBaby()) {
            return null;
        }
        float pitch = entry.basePitch();
        if (entry.pitchVariance() != 0f) {
            pitch += dragon.getRandom().nextFloat() * entry.pitchVariance();
        }
        if (dragon.isBaby()) {
            pitch *= BABY_PITCH_MULTIPLIER;
        }
        if ("flex".equals(key)) {
            return DragonSoundSpec.world(entry.soundSupplier().get(), SoundSource.NEUTRAL, entry.volume(), pitch);
        }
        int duration = switch (key) {
            case "roar", "roar_ground", "roar_air" -> 112;
            case "grumble1" -> 52;
            case "grumble2" -> 72;
            case "grumble3" -> 43;
            case "excited" -> 89;
            default -> -1;
        };
        if (duration < 0) {
            return null;
        }
        return DragonSoundSpec.moving(entry.soundSupplier().get(), SoundSource.NEUTRAL, entry.volume(), pitch, duration);
    }

    @Override
    public boolean handleWingFlapSound(DragonSoundHandler handler, DragonEntity dragon, String key) {
        playWingFlap(handler, dragon);
        return true;
    }

    private void playWingFlap(DragonSoundHandler handler, DragonEntity dragon) {
        double flightSpeed = dragon.getCachedHorizontalSpeed();
        float pitch = 1.0f + (float) (flightSpeed * 0.3f);
        float volume = Math.max(0.6f, 0.9f + (float) (flightSpeed * 0.2f));
        handler.playClientSound(dragon, dragon.position(), ModSounds.RAEVYX_FLAP.get(), volume, pitch);
    }
}
