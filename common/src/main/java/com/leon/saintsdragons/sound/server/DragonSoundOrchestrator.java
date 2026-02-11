package com.leon.saintsdragons.sound.server;

import com.leon.saintsdragons.common.network.MessageDragonMovingSound;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.sound.api.DragonSoundMode;
import com.leon.saintsdragons.sound.api.DragonSoundSpec;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DragonSoundOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonSoundOrchestrator.class);

    private DragonSoundOrchestrator() {
    }

    public static void play(DragonEntity dragon, DragonSoundSpec spec) {
        if (dragon == null || spec == null || spec.sound() == null) {
            return;
        }
        if (spec.mode() == DragonSoundMode.MOVING) {
            playMoving(dragon, spec);
            return;
        }
        playWorld(dragon, spec);
    }

    private static void playWorld(DragonEntity dragon, DragonSoundSpec spec) {
        if (dragon.level().isClientSide) {
            dragon.level().playLocalSound(
                    dragon.getX(), dragon.getY(), dragon.getZ(),
                    spec.sound(), spec.source(), spec.volume(), spec.pitch(), false
            );
            return;
        }
        dragon.level().playSound(
                null, dragon.getX(), dragon.getY(), dragon.getZ(),
                spec.sound(), spec.source(), spec.volume(), spec.pitch()
        );
    }

    private static void playMoving(DragonEntity dragon, DragonSoundSpec spec) {
        if (dragon.level().isClientSide) {
            return;
        }
        var soundId = BuiltInRegistries.SOUND_EVENT.getKey(spec.sound());
        if (soundId == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sound event not registered for dragon {}: {}", dragon.getId(), spec.sound());
            }
            return;
        }
        MessageDragonMovingSound packet = new MessageDragonMovingSound(
                dragon.getId(),
                soundId.toString(),
                spec.volume(),
                spec.pitch(),
                spec.durationTicks()
        );
        // Tracking already includes nearby rider/owner and avoids duplicate client restarts.
        NetworkHandler.sendToTracking(dragon, packet);
    }
}
