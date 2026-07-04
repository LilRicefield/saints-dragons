package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
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
        Vec3 targetVelocity = target.getDeltaMovement();
        double targetX = target.getX() + targetVelocity.x * predictionTicks;
        double targetZ = target.getZ() + targetVelocity.z * predictionTicks;
        double targetY = target.getY() + target.getBbHeight() + heightOffset
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
