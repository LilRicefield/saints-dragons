package com.leon.saintsdragons.server.entity.controller;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;

/**
 * Custom body rotation control for dragons based on The Dawn Era mod.
 *
 * Key behavior:
 * - When MOVING: Body aligns to movement direction, head can look around freely
 * - When STANDING: Body gradually follows head rotation
 * - Prevents jitter by locking body rotation during movement
 */
public class DragonBodyControl extends BodyRotationControl {
    private static final int HISTORY_SIZE = 10;
    private final Mob entity;
    private float targetYawHead;
    private final double[] histPosX = new double[HISTORY_SIZE];
    private final double[] histPosZ = new double[HISTORY_SIZE];
    private final float turnSpeed;

    public DragonBodyControl(Mob entity) {
        this(entity, 0.6f); // Default turn speed
    }

    public DragonBodyControl(Mob entity, float turnSpeed) {
        super(entity);
        this.entity = entity;
        this.turnSpeed = turnSpeed;
    }

    @Override
    public void clientTick() {
        // Skip if ridden (rider controls rotation)
        if (this.entity.isVehicle()) {
            return;
        }

        // Shift history
        for (int i = this.histPosX.length - 1; i > 0; --i) {
            this.histPosX[i] = this.histPosX[i - 1];
            this.histPosZ[i] = this.histPosZ[i - 1];
        }
        this.histPosX[0] = this.entity.getX();
        this.histPosZ[0] = this.entity.getZ();

        // Calculate movement velocity by comparing position history
        double dx = this.delta(this.histPosX);
        double dz = this.delta(this.histPosZ);
        double distSq = dx * dx + dz * dz;

        // If moving (velocity detected)
        if (distSq > 2.5E-7) {
            // Calculate movement direction
            double moveAngle = Math.toDegrees(Mth.atan2(dz, dx)) - 90.0;

            // Check if moving backward (movement direction is opposite to body facing)
            float angleDiff = Math.abs(Mth.wrapDegrees((float)moveAngle - this.entity.yBodyRot));
            boolean movingBackward = angleDiff > 135.0f; // ~135-180° = backing up

            if (!movingBackward) {
                // Normal movement: align body to movement direction
                this.entity.yBodyRot = (float)(this.entity.yBodyRot + Mth.wrapDegrees(moveAngle - this.entity.yBodyRot) * this.turnSpeed);
            }
            // If moving backward, keep current body rotation (don't try to turn around)

            this.targetYawHead = this.entity.yHeadRot;
        }
        // If standing still
        else {
            // Body gradually follows head
            this.targetYawHead = smooth(this.targetYawHead, this.entity.yHeadRot, 0.3f);
            this.entity.yBodyRot = approach(this.targetYawHead, this.entity.yBodyRot, 75.0f);
        }
    }

    /**
     * Calculate velocity delta by comparing recent vs older positions.
     */
    private double delta(double[] arr) {
        return this.mean(arr, 0) - this.mean(arr, 5);
    }

    /**
     * Calculate mean of 5 consecutive values starting at index.
     */
    private double mean(double[] arr, int start) {
        double mean = 0.0;
        for (int i = 0; i < 5; ++i) {
            mean += arr[i + start];
        }
        return mean / (double)arr.length;
    }

    /**
     * Smoothly approach target value.
     */
    private static float smooth(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    /**
     * Approach target rotation with a maximum delta limit.
     */
    private static float approach(float target, float current, float limit) {
        float delta = Mth.wrapDegrees(current - target);
        if (delta < -limit) {
            delta = -limit;
        } else if (delta >= limit) {
            delta = limit;
        }
        return target + delta * 0.55f;
    }
}
