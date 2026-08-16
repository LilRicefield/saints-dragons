package com.leon.saintsdragons.server.entity.dragons.volitans.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.sound.api.DragonSoundSpec;
import net.minecraft.sounds.SoundSource;

import java.util.Map;

public final class VolitansSoundProfile implements DragonSoundProfile {

    public static final VolitansSoundProfile INSTANCE = new VolitansSoundProfile();

    private static final Map<String, Boolean> BABY_ALLOWED_KEYS = Map.ofEntries(
            Map.entry("volitans_eat", true),
            Map.entry("volitans_hurt", true),
            Map.entry("volitans_die", true),
            Map.entry("volitans_flap", true)
    );

    private VolitansSoundProfile() {
    }

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        if (dragon.isBaby() && key.startsWith("volitans_") && !BABY_ALLOWED_KEYS.containsKey(key)) {
            return true;
        }
        if (key.startsWith("volitans_flap") || key.startsWith("flap")) {
            playWingFlap(handler, dragon);
            return true;
        }
        return key.startsWith("volitans_");
    }

    @Override
    public boolean handleVocal(DragonSoundHandler handler, DragonEntity dragon, String key) {
        if (!"investigating".equals(key)) {
            return false;
        }
        if (!dragon.isBaby()) {
            handler.playWorldSound(ModSounds.VOLITANS_INVESTIGATING.get(), 1.0F, 1.0F);
        }
        return true;
    }

    @Override
    public boolean handleSoundByName(DragonSoundHandler handler, DragonEntity dragon, String key) {
        if (dragon.isBaby()) {
            return true;
        }
        if (key.startsWith("volitans_flap") || key.startsWith("flap")) {
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
        int duration = switch (key) {
            case "grumble1", "volitans_grumble1" -> 67;
            case "grumble2", "volitans_grumble2" -> 29;
            case "grumble3", "volitans_grumble3" -> 25;
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
        float volume = dragon.isBaby() ? 0.7f : 1.0f;
        float pitch = 0.96f + (dragon.getRandom().nextFloat() - 0.5f) * 0.10f;
        handler.playClientSound(dragon, dragon.position(), ModSounds.VOLITANS_FLAP.get(), volume, pitch);
    }
}
