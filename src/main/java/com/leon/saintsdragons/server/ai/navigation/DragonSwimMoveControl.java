package com.leon.saintsdragons.server.ai.navigation;

import com.leon.saintsdragons.server.entity.interfaces.AquaticDragon;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/**
 * Smooth aquatic movement controller used while a dragon relies on {@link DragonAmphibiousNavigation}.
 * It accelerates toward the navigation's wanted position and dampens sideways drift to keep
 * the body aligned with the current swim direction.
 */
public class DragonSwimMoveControl extends MoveControl {

    private final Mob mob;
    private final float yawLimit;
    private final double accelerationScale;
    private final double speedBoost;

    public DragonSwimMoveControl(Mob mob, float yawLimitDegrees, double accelerationScale, double speedBoost) {
        super(mob);
        this.mob = mob;
        this.yawLimit = yawLimitDegrees;
        this.accelerationScale = accelerationScale;
        this.speedBoost = speedBoost;
    }

    @Override
    public void tick() {
        if (this.operation != Operation.MOVE_TO || this.mob.isPassenger()) {
            this.operation = Operation.WAIT;
            return;
        }

        Vec3 toTarget = new Vec3(this.wantedX - mob.getX(), this.wantedY - mob.getY(), this.wantedZ - mob.getZ());
        double distance = toTarget.length();
        if (distance < 1.0E-4D) {
            this.operation = Operation.WAIT;
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.8D));
            return;
        }

        Vec3 direction = toTarget.scale(1.0D / distance);
        Vec3 velocity = mob.getDeltaMovement();
        double baseSpeed = getNaturalSwimSpeed(mob);
        double speedModifier = Math.max(0.15D, this.speedModifier);

        double targetSpeed = clampSpeed(baseSpeed * speedModifier);
        Vec3 desired = velocity.add(direction.scale(accelerationScale * baseSpeed * speedModifier));

        if (desired.lengthSqr() > targetSpeed * targetSpeed) {
            desired = desired.normalize().scale(targetSpeed);
        }

        mob.setDeltaMovement(desired);

        float yaw = (float) (Mth.atan2(desired.z, desired.x) * Mth.RAD_TO_DEG) - 90.0F;
        mob.setYRot(Mth.approachDegrees(mob.getYRot(), yaw, yawLimit));
        mob.yBodyRot = mob.getYRot();

        float pitch = (float) -(Mth.atan2(desired.y, Math.sqrt(desired.x * desired.x + desired.z * desired.z)) * Mth.RAD_TO_DEG);
        mob.setXRot(Mth.approachDegrees(mob.getXRot(), pitch, yawLimit));
    }

    private double getNaturalSwimSpeed(Mob mob) {
        if (mob instanceof AquaticDragon dragon) {
            return dragon.getSwimSpeed() + speedBoost;
        }
        return 0.6D + speedBoost;
    }

    private double clampSpeed(double value) {
        double min = Math.max(0.2D, value * 0.45D);
        double max = Math.max(0.5D, value * 1.6D);
        return Mth.clamp(value, min, max);
    }
}

