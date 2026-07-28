package com.leon.saintsdragons.server.entity.dragons.cindervane.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.sound.api.DragonSoundSpec;
import net.minecraft.sounds.SoundSource;

import java.util.Map;

public final class CindervaneSoundProfile implements DragonSoundProfile {

    public static final CindervaneSoundProfile INSTANCE = new CindervaneSoundProfile();

    private static final Map<String, Boolean> BABY_ALLOWED_KEYS = Map.ofEntries(
            Map.entry("cindervane_eat", true),
            Map.entry("cindervane_hurt", true),
            Map.entry("cindervane_die", true),
            Map.entry("cindervane_flap", true)
    );

    private CindervaneSoundProfile() {
    }

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        if (dragon.isBaby() && key.startsWith("cindervane_") && !BABY_ALLOWED_KEYS.containsKey(key)) {
            return true;
        }
        if (key.startsWith("cindervane_flap") || key.startsWith("flap")) {
            playWingFlap(handler, dragon);
            return true;
        }
        return key.startsWith("cindervane_");
    }

    @Override
    public boolean handleSoundByName(DragonSoundHandler handler, DragonEntity dragon, String key) {
        if (dragon.isBaby()) {
            return true;
        }
        if (key.startsWith("cindervane_flap") || key.startsWith("flap")) {
            playWingFlap(handler, dragon);
            return true;
        }
        return false;
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
            case "grumble1", "cindervane_grumble1" -> 37;
            case "grumble2", "cindervane_grumble2" -> 27;
            case "grumble3", "cindervane_grumble3" -> 39;
            case "cindervane_flex" -> 100;
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

    @Override
    public boolean handleWingFlapSound(DragonSoundHandler handler, DragonEntity dragon, String key) {
        playWingFlap(handler, dragon);
        return true;
    }

    private void playWingFlap(DragonSoundHandler handler, DragonEntity dragon) {
        float volume = dragon.isBaby() ? 0.6f : 1.1f;
        float pitch = 0.98f + (dragon.getRandom().nextFloat() - 0.5f) * 0.1f;
        handler.playClientSound(dragon, dragon.position(), ModSounds.CINDERVANE_FLAP.get(), volume, pitch);
    }
}
