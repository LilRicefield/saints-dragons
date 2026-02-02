package com.leon.saintsdragons.client.camera;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.RideableDragon;
import net.minecraft.world.entity.Entity;

public final class DragonFovHelper {
    public static final double GROUND_SPRINT_MULTIPLIER = 1.050;
    public static final double FLY_SWIM_SPRINT_MULTIPLIER = 1.075;

    private DragonFovHelper() {}

    public static boolean shouldApply(Entity vehicle) {
        return vehicle instanceof DragonEntity && vehicle instanceof RideableDragon;
    }

    public static double getTargetMultiplier(Entity vehicle) {
        if (!(vehicle instanceof DragonEntity dragon) || !(vehicle instanceof RideableDragon rideable)) {
            return 1.0;
        }
        if (!rideable.isAccelerating()) {
            return 1.0;
        }
        if (dragon.isFlying() || dragon.isInWaterOrBubble()) {
            return FLY_SWIM_SPRINT_MULTIPLIER;
        }
        return GROUND_SPRINT_MULTIPLIER;
    }
}
