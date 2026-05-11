package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
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

    public static boolean isBiteOnlyPreyTarget(LivingEntity target) {
        if (target == null || target instanceof Player || target instanceof DragonEntity) {
            return false;
        }
        if (target instanceof Animal) {
            return true;
        }

        MobCategory category = target.getType().getCategory();
        return category == MobCategory.CREATURE
                || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE
                || category == MobCategory.AMBIENT
                || category == MobCategory.AXOLOTLS;
    }
}
