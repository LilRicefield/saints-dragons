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

    // CRITICAL: Max head rotation relative to body (prevents neck-crunching and 360 spins)
    // When head tries to rotate beyond this, body MUST turn instead
    private final float maxHeadBodyDiff;

    // Rotation speed parameters
    private final float headLagSpeed;        // How fast head follows target while standing
    private final float bodyLagStillSpeed;   // How fast body follows head while standing
    private final float bodyMaxDelta;        // Max degrees body can rotate per tick

    public DragonBodyControl(Mob entity, float turnSpeed) {
        this(entity, turnSpeed, 50.0f, 0.3f, 0.05f, 45.0f);
    }

    public DragonBodyControl(Mob entity, float turnSpeed, float maxHeadBodyDiff,
                           float headLagSpeed, float bodyLagStillSpeed, float bodyMaxDelta) {
        super(entity);
        this.entity = entity;
        this.turnSpeed = turnSpeed;
        this.maxHeadBodyDiff = maxHeadBodyDiff;
        this.headLagSpeed = headLagSpeed;
        this.bodyLagStillSpeed = bodyLagStillSpeed;
        this.bodyMaxDelta = bodyMaxDelta;
    }

    @Override
    public void clientTick() {
        // Skip if ridden (rider controls rotation, synced from server)
        // CRITICAL: Also skip for observers - let vanilla sync handle body rotation!
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

            // ALWAYS align body to movement direction to prevent backwards walking
            // This forces the dragon to turn around instead of walking backwards with bent neck
            this.entity.yBodyRot = (float)(this.entity.yBodyRot + Mth.wrapDegrees(moveAngle - this.entity.yBodyRot) * this.turnSpeed);

            this.targetYawHead = this.entity.yHeadRot;
        }
        // If standing still
        else {
            // Body gradually follows head (SLOW for natural behavior)
            this.targetYawHead = smooth(this.targetYawHead, this.entity.yHeadRot, this.headLagSpeed);
            this.entity.yBodyRot = approach(this.targetYawHead, this.entity.yBodyRot, this.bodyMaxDelta, this.bodyLagStillSpeed);
        }

        // CRITICAL: Clamp head rotation relative to body to prevent neck-crunching
        // This forces the body to turn when the head looks too far back
        clampHeadBodyDifference();
    }

    /**
     * Server-side body rotation update.
     * Called from DragonEntity.tick() on server side to keep body aligned with head/movement.
     * CRITICAL: This prevents neck "crunching" when dragon looks around while standing.
     */
    public void serverTick() {
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

            // ALWAYS align body to movement direction to prevent backwards walking
            // This forces the dragon to turn around instead of walking backwards with bent neck
            this.entity.yBodyRot = (float)(this.entity.yBodyRot + Mth.wrapDegrees(moveAngle - this.entity.yBodyRot) * this.turnSpeed);

            this.targetYawHead = this.entity.yHeadRot;
        }
        // If standing still
        else {
            // Body gradually follows head (SLOW for natural behavior)
            this.targetYawHead = smooth(this.targetYawHead, this.entity.yHeadRot, this.headLagSpeed);
            this.entity.yBodyRot = approach(this.targetYawHead, this.entity.yBodyRot, this.bodyMaxDelta, this.bodyLagStillSpeed);
        }

        // CRITICAL: Clamp head rotation relative to body to prevent neck-crunching
        // This forces the body to turn when the head looks too far back
        clampHeadBodyDifference();
    }

    /**
     * CRITICAL FIX: Clamps head rotation relative to body.
     * Without this, the head can rotate 180° from the body, causing:
     * 1. Neck bones to "crunch" visually
     * 2. Body to do a full 360° spin to catch up
     * With this, when the head reaches maxHeadBodyDiff, the body is FORCED to turn,
     * preventing the 360 spin and keeping the neck bones natural.
     */
    private void clampHeadBodyDifference() {
        float diff = Mth.wrapDegrees(this.entity.yHeadRot - this.entity.yBodyRot);
        float clamped = Mth.clamp(diff, -this.maxHeadBodyDiff, this.maxHeadBodyDiff);
        this.entity.yHeadRot = this.entity.yBodyRot + clamped;
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
     * Approach target rotation with a maximum delta limit and speed factor.
     */
    private static float approach(float target, float current, float maxDelta, float speed) {
        float delta = Mth.wrapDegrees(current - target);
        delta = Mth.clamp(delta, -maxDelta, maxDelta);
        return target + delta * speed;
    }
}
