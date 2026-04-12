package com.leon.saintsdragons.server.entity.dragons.varasuchus.handlers;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.sound.api.DragonSoundSpec;
import net.minecraft.sounds.SoundSource;

import java.util.Map;

public final class VarasuchusSoundProfile implements DragonSoundProfile {

    public static final VarasuchusSoundProfile INSTANCE = new VarasuchusSoundProfile();
    private static final Map<String, Boolean> BABY_ALLOWED_KEYS = Map.ofEntries(
            Map.entry("varasuchus_eat", true),
            Map.entry("varasuchus_hurt", true),
            Map.entry("varasuchus_die", true)
    );

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.ofEntries(
            Map.entry("grumble1", 161),
            Map.entry("grumble2", 136),
            Map.entry("grumble3", 135),
            Map.entry("roar", 140),
            Map.entry("phase1", 85),
            Map.entry("phase2", 60),
            Map.entry("phase2_start", 60),
            Map.entry("phase2_end", 70),
            Map.entry("hurt", 20),
            Map.entry("die", 90)
    );

    private VarasuchusSoundProfile() {}

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        if (dragon.isBaby() && key.startsWith("varasuchus_") && !BABY_ALLOWED_KEYS.containsKey(key)) {
            return true;
        }
        return key.startsWith("varasuchus_");
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
        if (dragon.isBaby()) {
            return null;
        }
        int duration = switch (key) {
            case "grumble1", "varasuchus_grumble1" -> 161;
            case "grumble2", "varasuchus_grumble2" -> 136;
            case "grumble3", "varasuchus_grumble3" -> 135;
            case "roar", "varasuchus_roar" -> 140;
            case "phase1", "varasuchus_phase1" -> 85;
            case "phase2_start", "varasuchus_phase2_start" -> 38;
            case "phase2", "varasuchus_phase2" -> 60;
            case "phase2_end", "varasuchus_phase2_end" -> 17;
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
