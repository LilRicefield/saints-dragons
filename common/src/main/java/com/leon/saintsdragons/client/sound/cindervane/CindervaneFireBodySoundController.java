package com.leon.saintsdragons.client.sound.cindervane;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public final class CindervaneFireBodySoundController {
    private static final Map<Integer, CindervaneFireBodyLoopSound> SOUNDS = new HashMap<>();
    private static ClientLevel lastLevel;

    private CindervaneFireBodySoundController() {}

    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level != lastLevel) {
            for (var sound : SOUNDS.values()) minecraft.getSoundManager().stop(sound);
            SOUNDS.clear();
            lastLevel = level;
        }
        if (level == null || minecraft.isPaused()) return;

        var iterator = SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var sound = entry.getValue();
            if (sound.isStopped() || sound.getDragon().isRemoved() || !sound.getDragon().isAlive()
                    || level.getEntity(entry.getKey()) != sound.getDragon()) {
                minecraft.getSoundManager().stop(sound);
                iterator.remove();
            }
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Cindervane dragon && CindervaneFireBodyLoopSound.isFireBodyActive(dragon)
                    && !SOUNDS.containsKey(dragon.getId())) {
                var sound = new CindervaneFireBodyLoopSound(dragon);
                SOUNDS.put(dragon.getId(), sound);
                minecraft.getSoundManager().play(sound);
            }
        }
    }
}
