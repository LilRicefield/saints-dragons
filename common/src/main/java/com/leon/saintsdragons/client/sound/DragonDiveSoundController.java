package com.leon.saintsdragons.client.sound;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class DragonDiveSoundController {
    private static final double DIVE_START_SPEED = 0.75D;
    private static final double DIVE_FULL_SPEED = 4.00D;
    private static final double DIVE_START_DOWNWARD_SPEED = 0.10D;
    private static final double DIVE_FULL_DOWNWARD_SPEED = 1.35D;

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

        if (activeSound != null && activeSound.isStopped()) {
            activeSound = null;
            activeDragonId = -1;
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

    static float getDiveSoundIntensity(RideableDragonBase dragon) {
        if (dragon == null || !dragon.isFlying() || dragon.isInWaterOrBubble()) {
            return 0.0F;
        }

        Vec3 velocity = dragon.getDeltaMovement();
        double downwardSpeed = -velocity.y;
        if (downwardSpeed <= DIVE_START_DOWNWARD_SPEED) {
            return 0.0F;
        }

        double speedFactor = normalize(velocity.length(), DIVE_START_SPEED, DIVE_FULL_SPEED);
        double downwardFactor = normalize(downwardSpeed, DIVE_START_DOWNWARD_SPEED, DIVE_FULL_DOWNWARD_SPEED);
        return (float) (speedFactor * downwardFactor);
    }

    private static void stopCurrent(Minecraft minecraft) {
        if (activeSound != null) {
            minecraft.getSoundManager().stop(activeSound);
            activeSound = null;
        }
        activeDragonId = -1;
    }

    private static double normalize(double value, double start, double end) {
        return Mth.clamp((value - start) / (end - start), 0.0D, 1.0D);
    }
}
