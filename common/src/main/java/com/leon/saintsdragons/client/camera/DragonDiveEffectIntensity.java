package com.leon.saintsdragons.client.camera;

import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.flight.DragonRiderFlightController;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class DragonDiveEffectIntensity {
    private static final double DIVE_START_SPEED = 0.75D;
    private static final double DIVE_FULL_SPEED = 4.00D;
    private static final double DIVE_START_DOWNWARD_SPEED = 0.10D;
    private static final double DIVE_FULL_DOWNWARD_SPEED = 1.35D;

    private DragonDiveEffectIntensity() {
    }

    public static float get(Entity entity) {
        if (!(entity instanceof RideableFlyingDragon dragon) || !dragon.isFlying() || dragon.isInWaterOrBubble()) {
            return 0.0F;
        }

        Vec3 velocity = dragon.getDeltaMovement();
        Vec3 positionDelta = new Vec3(
                dragon.getX() - dragon.xo,
                dragon.getY() - dragon.yo,
                dragon.getZ() - dragon.zo
        );
        double speed = Math.max(velocity.length(), positionDelta.length());
        double downwardSpeed = Math.max(-velocity.y, -positionDelta.y);
        double speedFactor = normalize(speed, DIVE_START_SPEED, DIVE_FULL_SPEED);

        float diveIntensity = 0.0F;
        if (downwardSpeed > DIVE_START_DOWNWARD_SPEED) {
            double downwardFactor = normalize(downwardSpeed, DIVE_START_DOWNWARD_SPEED, DIVE_FULL_DOWNWARD_SPEED);
            diveIntensity = (float) (speedFactor * downwardFactor);
        }

        float holdIntensity = 0.0F;
        int holdTicks = dragon.getRiderDiveBoostHoldTicks();
        if (holdTicks > 0) {
            float holdFactor = Mth.clamp(holdTicks / (float) DragonRiderFlightController.DIVE_EXIT_BOOST_HOLD_TICKS, 0.0F, 1.0F);
            holdIntensity = (float) speedFactor * holdFactor;
        }

        return Math.max(diveIntensity, holdIntensity);
    }

    private static double normalize(double value, double start, double end) {
        return Mth.clamp((value - start) / (end - start), 0.0D, 1.0D);
    }
}
