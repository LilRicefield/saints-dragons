package com.leon.saintsdragons.server.entity.interfaces;

import com.leon.saintsdragons.server.entity.handler.HumanSoundHandler;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public interface HumanSoundProfile {
    HumanSoundProfile EMPTY = new HumanSoundProfile() {};
    default boolean handleSound(HumanSoundHandler handler, Mob entity, String soundKey,
                                String locator, float volume, float pitch) {
        return false;
    }
    default Vec3 resolveLocator(HumanSoundHandler handler, Mob entity, String locator) {
        return entity.position();
    }
}