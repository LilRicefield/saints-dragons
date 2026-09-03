package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DragonAsyncAirMovementHelper {
    private DragonAsyncAirMovementHelper() {
    }

    public static void chasePredictedTarget(
            RideableFlyingDragon dragon,
            LivingEntity target,
            double predictionTicks,
            double heightOffset,
            double bobFrequency,
            double bobAmplitude,
            double speedScale
    ) {
        Entity movementAnchor = DragonTargetingHelper.movementAnchor(target);
        Vec3 targetVelocity = movementAnchor.getDeltaMovement();
        double targetX = movementAnchor.getX() + targetVelocity.x * predictionTicks;
        double targetZ = movementAnchor.getZ() + targetVelocity.z * predictionTicks;
        double targetY = movementAnchor.getY() + movementAnchor.getBbHeight() + heightOffset
                + Math.sin(dragon.tickCount * bobFrequency) * bobAmplitude;
        moveToward(dragon, new Vec3(targetX, targetY, targetZ), speedScale);
    }

    public static void holdPosition(RideableFlyingDragon dragon) {
        dragon.getAIMovement().stop();
    }

    public static void moveToward(RideableFlyingDragon dragon, Vec3 destination, double speedScale) {
        if (dragon.isTakeoff() && dragon.onGround()) {
            return;
        }
        dragon.beginAiFlight();
        dragon.getAIMovement().setWaypoint(destination, speedScale);
    }
}
