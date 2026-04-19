package com.leon.saintsdragons.fabric.client.camera;

import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class NulljawFirstPersonCamera {
    public static final double Y_OFFSET = -2.0D;

    private NulljawFirstPersonCamera() {
    }

    public static boolean isActive(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || entity == null) {
            return false;
        }
        if (entity != minecraft.player || minecraft.getCameraEntity() != entity) {
            return false;
        }
        if (!minecraft.options.getCameraType().isFirstPerson()) {
            return false;
        }
        return entity.getVehicle() instanceof Nulljaw;
    }
}
