package com.leon.saintsdragons.server.entity.util;

import com.leon.saintsdragons.server.entity.base.RideableGroundDragon;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class GroundDragonJumpHelper {
    private GroundDragonJumpHelper() {
    }

    public static void jump(RideableGroundDragon dragon,
                            float jumpScale,
                            Vec3 travelVector,
                            double minVertical,
                            double maxVertical,
                            double forwardBoost) {
        if (jumpScale <= 0.0F) {
            return;
        }

        float charge = Mth.clamp(jumpScale, 0.0F, 1.0F);
        double vertical = Mth.lerp(charge, minVertical, maxVertical);
        Vec3 current = dragon.getDeltaMovement();

        dragon.setDeltaMovement(current.x, vertical, current.z);
        if (travelVector.z > 0.0D) {
            float yawRad = dragon.getYRot() * Mth.DEG_TO_RAD;
            float sin = Mth.sin(yawRad);
            float cos = Mth.cos(yawRad);
            dragon.setDeltaMovement(dragon.getDeltaMovement().add(
                    -forwardBoost * sin * charge,
                    0.0D,
                    forwardBoost * cos * charge
            ));
        }
        dragon.hasImpulse = true;
        dragon.hurtMarked = true;
        dragon.fallDistance = 0.0F;
    }
}