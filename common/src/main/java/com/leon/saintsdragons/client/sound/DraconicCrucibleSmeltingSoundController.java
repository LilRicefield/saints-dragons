package com.leon.saintsdragons.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class DraconicCrucibleSmeltingSoundController {
    private static final Map<SoundKey, DraconicCrucibleSmeltingSound> ACTIVE_SOUNDS = new HashMap<>();

    private DraconicCrucibleSmeltingSoundController() {
    }

    public static void update(Level level, BlockPos pos, boolean smelting) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        SoundKey key = new SoundKey(clientLevel, pos.asLong());
        DraconicCrucibleSmeltingSound current = ACTIVE_SOUNDS.get(key);
        if (!smelting) {
            stop(minecraft, key, current);
            return;
        }
        if (current == null || current.isStopped()) {
            DraconicCrucibleSmeltingSound sound =
                    new DraconicCrucibleSmeltingSound(clientLevel, pos);
            ACTIVE_SOUNDS.put(key, sound);
            minecraft.getSoundManager().play(sound);
        }
    }

    public static void tick(Minecraft minecraft) {
        Iterator<Map.Entry<SoundKey, DraconicCrucibleSmeltingSound>> iterator =
                ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SoundKey, DraconicCrucibleSmeltingSound> entry = iterator.next();
            DraconicCrucibleSmeltingSound sound = entry.getValue();
            if (entry.getKey().level() != minecraft.level || sound == null || sound.isStopped()) {
                if (sound != null) {
                    minecraft.getSoundManager().stop(sound);
                }
                iterator.remove();
            }
        }
    }

    private static void stop(Minecraft minecraft, SoundKey key,
                             DraconicCrucibleSmeltingSound sound) {
        ACTIVE_SOUNDS.remove(key);
        if (sound != null) {
            minecraft.getSoundManager().stop(sound);
        }
    }

    private record SoundKey(ClientLevel level, long pos) {
    }
}
