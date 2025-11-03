package com.leon.saintsdragons.server.entity.interfaces;

import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;

/**
 * Marker interface for dragons that expose a sound handler instance.
 */
public interface SoundHandledDragon {
    DragonSoundHandler getSoundHandler();
}
