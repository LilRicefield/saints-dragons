package com.leon.saintsdragons.client.sound;

import com.leon.saintsdragons.client.camera.DragonDiveEffectIntensity;
import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public final class DragonDiveSoundController {
    private static DragonDiveLoopSound activeSound;
    private static int activeDragonId = -1;

    private DragonDiveSoundController() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft == null || minecraft.isPaused()) {
            return;
        }

        Entity player = minecraft.player;
        Entity vehicle = player == null ? null : player.getVehicle();
        if (!(vehicle instanceof RideableDragonBase dragon)) {
            stopCurrent(minecraft);
            return;
        }
        if (!Services.PLATFORM.isGenericDiveLoopEnabled()) {
            stopCurrent(minecraft);
            return;
        }
        if (dragon instanceof Raevyx raevyx && raevyx.isCustomDiveLoopEnabled()) {
            stopCurrent(minecraft);
            return;
        }

        if (activeSound != null && activeSound.isStopped()) {
            activeSound = null;
            activeDragonId = -1;
        }

        if (activeSound != null && !minecraft.getSoundManager().isActive(activeSound)) {
            activeSound = null;
            activeDragonId = -1;
        }

        if (activeSound != null && activeDragonId != dragon.getId()) {
            stopCurrent(minecraft);
        }

        if (getDiveSoundIntensity(dragon) <= 0.0F) {
            return;
        }

        if (activeSound == null || activeDragonId != dragon.getId()) {
            activeSound = new DragonDiveLoopSound(dragon);
            activeDragonId = dragon.getId();
            minecraft.getSoundManager().play(activeSound);
        }
    }

    public static float getDiveSoundIntensity(Entity entity) {
        return DragonDiveEffectIntensity.get(entity);
    }

    private static void stopCurrent(Minecraft minecraft) {
        if (activeSound != null) {
            minecraft.getSoundManager().stop(activeSound);
            activeSound = null;
        }
        activeDragonId = -1;
    }

}
