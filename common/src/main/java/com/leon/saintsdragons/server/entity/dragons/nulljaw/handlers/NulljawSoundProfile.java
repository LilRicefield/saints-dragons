package com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.sound.api.DragonSoundSpec;
import net.minecraft.sounds.SoundSource;

import java.util.Map;

public final class NulljawSoundProfile implements DragonSoundProfile {

    public static final NulljawSoundProfile INSTANCE = new NulljawSoundProfile();
    private static final Map<String, Boolean> BABY_ALLOWED_KEYS = Map.ofEntries(
            Map.entry("nulljaw_eat", true),
            Map.entry("nulljaw_hurt", true),
            Map.entry("nulljaw_die", true)
    );

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.ofEntries(
            Map.entry("grumble1", 161),
            Map.entry("grumble2", 136),
            Map.entry("grumble3", 135),
            Map.entry("roar", 140),
            Map.entry("roar2", 180),
            Map.entry("phase1", 85),
            Map.entry("phase2", 180),
            Map.entry("phase2_start", 60),
            Map.entry("phase2_end", 70),
            Map.entry("hurt", 20),
            Map.entry("die", 90)
    );

    private NulljawSoundProfile() {}

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        if (dragon.isBaby() && key.startsWith("nulljaw_") && !BABY_ALLOWED_KEYS.containsKey(key)) {
            return true;
        }
        return key.startsWith("nulljaw_");
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
            case "grumble1", "nulljaw_grumble1" -> 161;
            case "grumble2", "nulljaw_grumble2" -> 136;
            case "grumble3", "nulljaw_grumble3" -> 135;
            case "roar", "nulljaw_roar" -> 140;
            case "roar2", "nulljaw_roar2" -> 180;
            case "phase1", "nulljaw_phase1" -> 85;
            case "phase2_start", "nulljaw_phase2_start" -> 38;
            case "phase2", "nulljaw_phase2" -> 67;
            case "phase2_end", "nulljaw_phase2_end" -> 17;
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
