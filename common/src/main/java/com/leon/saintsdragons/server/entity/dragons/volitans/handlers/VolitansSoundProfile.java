package com.leon.saintsdragons.server.entity.dragons.volitans.handlers;

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
            Map.entry("volitans_die", true)
    );

    private VolitansSoundProfile() {
    }

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        return dragon.isBaby() && key.startsWith("volitans_") && !BABY_ALLOWED_KEYS.containsKey(key);
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
}
