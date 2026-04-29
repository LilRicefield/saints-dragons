package com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.sound.api.DragonSoundSpec;
import net.minecraft.sounds.SoundSource;

import java.util.Map;

public final class IgnivorusSoundProfile implements DragonSoundProfile {

    public static final IgnivorusSoundProfile INSTANCE = new IgnivorusSoundProfile();

    private static final Map<String, Boolean> BABY_ALLOWED_KEYS = Map.ofEntries(
            Map.entry("ignivorus_eat", true),
            Map.entry("ignivorus_hurt", true),
            Map.entry("ignivorus_die", true),
            Map.entry("ignivorus_flap", true)
    );

    private IgnivorusSoundProfile() {
    }

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        if (dragon.isBaby() && key.startsWith("ignivorus_") && !BABY_ALLOWED_KEYS.containsKey(key)) {
            return true;
        }
        if (key.startsWith("ignivorus_flap") || key.startsWith("flap")) {
            playWingFlap(handler, dragon);
            return true;
        }
        return key.startsWith("ignivorus_");
    }

    @Override
    public boolean handleSoundByName(DragonSoundHandler handler, DragonEntity dragon, String key) {
        if (dragon.isBaby()) {
            return true;
        }
        if (key.startsWith("ignivorus_flap") || key.startsWith("flap")) {
            playWingFlap(handler, dragon);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleWingFlapSound(DragonSoundHandler handler, DragonEntity dragon, String key) {
        playWingFlap(handler, dragon);
        return true;
    }

    @Override
    public DragonSoundSpec getVocalSpec(DragonSoundHandler handler, DragonEntity dragon, String key, DragonEntity.VocalEntry entry) {
        if (entry == null || entry.soundSupplier() == null) {
            return null;
        }
        if (dragon.isBaby()) {
            return null;
        }
        int duration = switch (key) {
            case "ignivorus_grumble1", "grumble1" -> 56;
            case "ignivorus_grumble2", "grumble2" -> 61;
            case "ignivorus_grumble3", "grumble3" -> 55;
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

    private void playWingFlap(DragonSoundHandler handler, DragonEntity dragon) {
        float volume = dragon.isBaby() ? 0.7f : 1.2f;
        float pitch = 0.95f + (dragon.getRandom().nextFloat() - 0.5f) * 0.1f;
        handler.playClientSound(dragon, dragon.position(), ModSounds.IGNIVORUS_FLAP.get(), volume, pitch);
    }
}