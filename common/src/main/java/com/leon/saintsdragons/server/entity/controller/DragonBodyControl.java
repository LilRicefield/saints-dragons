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
    private static final double MOVING_EPSILON_SQ = 1.0E-4;
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
        // Slightly faster standing spin (bodyLagStillSpeed 0.1 -> body catches up quicker when idle)
        this(entity, turnSpeed, 50.0f, 0.3f, 0.10f, 45.0f);
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
        if (distSq > MOVING_EPSILON_SQ) {
            // Calculate movement direction
            double moveAngle = Math.toDegrees(Mth.atan2(dz, dx)) - 90.0;

            // Gradually turn body toward movement direction (like Naturalist)
            // This prevents instant snapping while still preventing backwards walking
            this.entity.yBodyRot = approach((float)moveAngle, this.entity.yBodyRot, this.bodyMaxDelta, this.turnSpeed);

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
        if (distSq > MOVING_EPSILON_SQ) {
            // Calculate movement direction
            double moveAngle = Math.toDegrees(Mth.atan2(dz, dx)) - 90.0;

            // Gradually turn body toward movement direction (like Naturalist)
            // This prevents instant snapping while still preventing backwards walking
            this.entity.yBodyRot = approach((float)moveAngle, this.entity.yBodyRot, this.bodyMaxDelta, this.turnSpeed);

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
        double sum = 0.0;
        int count = arr.length / 2; // always average 5 entries (half of HISTORY_SIZE)
        for (int i = 0; i < count; ++i) {
            sum += arr[i + start];
        }
        return sum / (double) count;
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
        float delta = Mth.wrapDegrees(target - current);
        delta = Mth.clamp(delta, -maxDelta, maxDelta);
        return current + delta * speed;
    }
}
