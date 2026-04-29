package com.leon.saintsdragons.server.entity.interfaces;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.sound.api.DragonSoundSpec;
import net.minecraft.world.phys.Vec3;

public interface DragonSoundProfile {
    DragonSoundProfile EMPTY = new DragonSoundProfile() {};
    default boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        return false;
    }
    default boolean handleSoundByName(DragonSoundHandler handler, DragonEntity dragon, String key) {
        return false;
    }
    default DragonEntity.VocalEntry getFallbackVocalEntry(String key) {
        return null;
    }
    default int getVocalAnimationWindowTicks(String key) {
        return -1;
    }
    default boolean handleWingFlapSound(DragonSoundHandler handler, DragonEntity dragon, String key) {
        return false;
    }
    default boolean handleStepSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator,
                                   double x, double y, double z, float volume, float pitch) {
        return false;
    }
    default boolean handleVocal(DragonSoundHandler handler, DragonEntity dragon, String key) {
        return false;
    }
    default DragonSoundSpec getVocalSpec(DragonSoundHandler handler, DragonEntity dragon, String key, DragonEntity.VocalEntry entry) {
        return null;
    }
}
