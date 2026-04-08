package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public final class DragonDirectAirCombatMovementHelper {
    private DragonDirectAirCombatMovementHelper() {
    }

    public static <T extends Mob & DragonFlightCapable> void chasePredictedTarget(
            T dragon,
            LivingEntity target,
            double predictionTicks,
            double heightOffset,
            double bobFrequency,
            double bobAmplitude,
            double speedScale,
            double accel,
            double drag
    ) {
        Vec3 targetVelocity = target.getDeltaMovement();
        double targetX = target.getX() + targetVelocity.x * predictionTicks;
        double targetZ = target.getZ() + targetVelocity.z * predictionTicks;
        double targetY = target.getY() + target.getBbHeight() + heightOffset
                + Math.sin(dragon.tickCount * bobFrequency) * bobAmplitude;
        flyToward(dragon, new Vec3(targetX, targetY, targetZ), speedScale, accel, drag);
    }

    public static <T extends Mob & DragonFlightCapable> void holdPosition(
            T dragon,
            double drag
    ) {
        Vec3 slowed = dragon.getDeltaMovement().scale(drag);
        if (slowed.lengthSqr() < 1.0E-4D) {
            slowed = Vec3.ZERO;
        }

        dragon.setDeltaMovement(slowed);
        dragon.move(MoverType.SELF, slowed);
        dragon.hasImpulse = true;
        updateRotation(dragon, slowed);
    }

    public static <T extends Mob & DragonFlightCapable> void flyToward(
            T dragon,
            Vec3 destination,
            double speedScale,
            double accel,
            double drag
    ) {
        if (dragon.isTakeoff() && dragon.onGround()) {
            return;
        }

        Vec3 toDest = destination.subtract(dragon.position());
        if (toDest.lengthSqr() < 1.0E-4D) {
            holdPosition(dragon, drag);
            return;
        }

        Vec3 targetDir = toDest.normalize();
        Vec3 current = dragon.getDeltaMovement();
        double flightSpeed = Math.max(0.18D, dragon.getFlightSpeed() * speedScale);
        Vec3 targetVel = targetDir.scale(flightSpeed);
        Vec3 blended = new Vec3(
                current.x + (targetVel.x - current.x) * accel,
                current.y + (targetVel.y - current.y) * accel,
                current.z + (targetVel.z - current.z) * accel
        ).scale(drag);

        dragon.setSpeed((float) flightSpeed);
        dragon.setDeltaMovement(blended);
        dragon.move(MoverType.SELF, blended);
        dragon.hasImpulse = true;
        updateRotation(dragon, blended);
    }

    private static <T extends Mob & DragonFlightCapable> void updateRotation(T dragon, Vec3 velocity) {
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontal > 1.0E-4D) {
            float targetYaw = (float) (Math.atan2(velocity.z, velocity.x) * (180.0D / Math.PI)) - 90.0F;
            dragon.yRotO = dragon.getYRot();
            dragon.yBodyRotO = dragon.yBodyRot;
            dragon.yHeadRotO = dragon.yHeadRot;
            dragon.setYRot(targetYaw);
            dragon.yBodyRot = targetYaw;
            dragon.setYHeadRot(targetYaw);
        }
        if (velocity.lengthSqr() > 1.0E-4D) {
            float targetPitch = (float) (-(Math.atan2(velocity.y, horizontal) * (180.0D / Math.PI)));
            dragon.xRotO = dragon.getXRot();
            dragon.setXRot(Mth.clamp(targetPitch, -45.0F, 45.0F));
        }
    }
}
