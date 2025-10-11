package com.leon.saintsdragons.server.ai.navigation;

import com.leon.saintsdragons.server.entity.interfaces.AquaticDragon;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/**
 * Smooth aquatic movement controller used while a dragon relies on {@link DragonAmphibiousNavigation}.
 * Based on Critters & Companions' Otter movement system - uses movement intention values (zza/yya)
 * instead of direct velocity manipulation to prevent jittering.
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
        // Only control movement when in water and not being ridden
        if (this.operation != Operation.MOVE_TO || this.mob.isPassenger() || !this.mob.isInWater()) {
            this.operation = Operation.WAIT;
            this.mob.setZza(0.0F);
            this.mob.setYya(0.0F);
            this.mob.setXxa(0.0F);
            return;
        }

        // Calculate direction to target
        double dx = this.wantedX - this.mob.getX();
        double dy = this.wantedY - this.mob.getY();
        double dz = this.wantedZ - this.mob.getZ();
        double distanceSqr = dx * dx + dy * dy + dz * dz;

        // If very close to target, stop moving
        if (distanceSqr < 2.5000003E-7F) {
            this.mob.setZza(0.0F);
            this.mob.setYya(0.0F);
            this.operation = Operation.WAIT;
            return;
        }

        // Calculate target yaw (horizontal rotation)
        float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.mob.setYRot(rotlerp(this.mob.getYRot(), targetYaw, yawLimit));
        this.mob.yBodyRot = this.mob.getYRot();
        this.mob.yHeadRot = this.mob.getYRot();

        // Calculate speed - amplify for swimming (dragons are large and need more power)
        float baseSpeed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
        this.mob.setSpeed(baseSpeed * 0.2F);  // Entity speed (for physics)

        // Calculate pitch (vertical rotation) - angle from horizontal to target
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (Math.abs(dy) > 1.0E-5F || Math.abs(horizontalDistance) > 1.0E-5F) {
            float targetPitch = -((float) (Mth.atan2(dy, horizontalDistance) * Mth.RAD_TO_DEG));
            targetPitch = Mth.clamp(Mth.wrapDegrees(targetPitch), -85.0F, 85.0F);
            this.mob.setXRot(rotlerp(this.mob.getXRot(), targetPitch, yawLimit * 0.5F));
        }

        // Set movement intention values using pitch
        // BOOST the intention values significantly for large aquatic creatures
        // Otters are small, dragons are MASSIVE and need more power to move through water
        float pitchRad = this.mob.getXRot() * Mth.DEG_TO_RAD;
        float cosXRot = Mth.cos(pitchRad);
        float sinXRot = Mth.sin(pitchRad);

        // Apply significant boost to movement intention (5x multiplier for large creature)
        float amplifiedSpeed = baseSpeed * 5.0F;
        this.mob.zza = cosXRot * amplifiedSpeed;      // Horizontal forward component
        this.mob.yya = -sinXRot * amplifiedSpeed;     // Vertical component

        // Add small upward push when target is above to help fight gravity
        if (dy > 0.5D && horizontalDistance < 2.0D) {
            this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0D, 0.015D, 0.0D));
        }
    }

    private double getNaturalSwimSpeed(Mob mob) {
        if (mob instanceof AquaticDragon dragon) {
            return dragon.getSwimSpeed() + speedBoost;
        }
        return 0.6D + speedBoost;
    }

    @Override
    protected float rotlerp(float current, float target, float maxDelta) {
        float delta = Mth.wrapDegrees(target - current);
        delta = Mth.clamp(delta, -maxDelta, maxDelta);
        return current + delta;
    }
}

