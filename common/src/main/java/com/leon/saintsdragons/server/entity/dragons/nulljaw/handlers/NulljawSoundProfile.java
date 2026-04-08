package com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.sound.api.DragonSoundSpec;
import net.minecraft.sounds.SoundSource;

public final class NulljawSoundProfile implements DragonSoundProfile {
    public static final NulljawSoundProfile INSTANCE = new NulljawSoundProfile();

    private NulljawSoundProfile() {
    }

    @Override
    public DragonSoundSpec getVocalSpec(DragonSoundHandler handler, DragonEntity dragon, String key, DragonEntity.VocalEntry entry) {
        if (entry == null || entry.soundSupplier() == null) {
            return null;
        }

        int duration = switch (key) {
            case "grumble1", "nulljaw_grumble1" -> 81;
            case "grumble2", "nulljaw_grumble2" -> 73;
            case "grumble3", "nulljaw_grumble3" -> 35;
            case "eat", "nulljaw_eat" -> 51;
            case "nulljaw_hurt" -> 13;
            case "nulljaw_die" -> 44;
            default -> -1;
        };

        if (duration < 0) {
            return null;
        }

        float pitch = entry.basePitch();
        if (entry.pitchVariance() != 0f) {
            pitch += (dragon.getRandom().nextFloat() - 0.5f) * entry.pitchVariance();
        }
        return DragonSoundSpec.moving(entry.soundSupplier().get(), SoundSource.NEUTRAL, entry.volume(), pitch, duration);
    }
}
