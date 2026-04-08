package com.leon.saintsdragons.server.ai.goals.base;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

public final class DragonTargetingHelper {
    private DragonTargetingHelper() {
    }

    public static boolean isTargetAirborne(LivingEntity target, double minHeightAboveGround) {
        if (target == null || target.onGround()) {
            return false;
        }
        if (target.getVehicle() instanceof LivingEntity vehicle) {
            return !vehicle.onGround();
        }
        if (target instanceof Player player && player.isFallFlying()) {
            return true;
        }

        double groundY = target.level()
                .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target.blockPosition())
                .getY();
        return target.getY() - groundY > minHeightAboveGround;
    }
}
