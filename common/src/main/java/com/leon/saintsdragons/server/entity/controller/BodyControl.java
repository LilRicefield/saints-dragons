package com.leon.saintsdragons.server.entity.controller;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;

public class BodyControl extends BodyRotationControl {
    private static final double MOVING_EPSILON_SQ = 1.0E-4;
    private static final int HISTORY_SIZE = 10;
    private final Mob entity;
    private float targetYawHead;
    private final double[] histPosX = new double[HISTORY_SIZE];
    private final double[] histPosZ = new double[HISTORY_SIZE];
    private final float turnSpeed;
    private final float maxHeadBodyDiff;
    private final float headLagSpeed;
    private final float bodyLagStillSpeed;
    private final float bodyMaxDelta;

    public BodyControl(Mob entity, float turnSpeed) {
        this(entity, turnSpeed, 50.0f, 0.3f, 0.10f, 45.0f);
    }

    public BodyControl(Mob entity, float turnSpeed, float maxHeadBodyDiff,
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
        if (this.entity.isVehicle()) {
            return;
        }
        if (shouldLockForSitting()) {
            freezeSeatedRotation();
            return;
        }

        for (int i = this.histPosX.length - 1; i > 0; --i) {
            this.histPosX[i] = this.histPosX[i - 1];
            this.histPosZ[i] = this.histPosZ[i - 1];
        }
        this.histPosX[0] = this.entity.getX();
        this.histPosZ[0] = this.entity.getZ();
        double dx = this.delta(this.histPosX);
        double dz = this.delta(this.histPosZ);
        double distSq = dx * dx + dz * dz;

        if (distSq > MOVING_EPSILON_SQ) {
            double moveAngle = Math.toDegrees(Mth.atan2(dz, dx)) - 90.0;
            this.entity.yBodyRot = approach((float)moveAngle, this.entity.yBodyRot, this.bodyMaxDelta, this.turnSpeed);

            this.targetYawHead = this.entity.yHeadRot;
        }
        else {
            this.targetYawHead = smooth(this.targetYawHead, this.entity.yHeadRot, this.headLagSpeed);
            this.entity.yBodyRot = approach(this.targetYawHead, this.entity.yBodyRot, this.bodyMaxDelta, this.bodyLagStillSpeed);
        }

        clampHeadBodyDifference();
    }

    public void serverTick() {
        if (this.entity.isVehicle()) {
            return;
        }
        if (shouldLockForSitting()) {
            freezeSeatedRotation();
            return;
        }
        for (int i = this.histPosX.length - 1; i > 0; --i) {
            this.histPosX[i] = this.histPosX[i - 1];
            this.histPosZ[i] = this.histPosZ[i - 1];
        }
        this.histPosX[0] = this.entity.getX();
        this.histPosZ[0] = this.entity.getZ();
        double dx = this.delta(this.histPosX);
        double dz = this.delta(this.histPosZ);
        double distSq = dx * dx + dz * dz;

        if (distSq > MOVING_EPSILON_SQ) {
            double moveAngle = Math.toDegrees(Mth.atan2(dz, dx)) - 90.0;
            this.entity.yBodyRot = approach((float)moveAngle, this.entity.yBodyRot, this.bodyMaxDelta, this.turnSpeed);

            this.targetYawHead = this.entity.yHeadRot;
        }
        else {
            this.targetYawHead = smooth(this.targetYawHead, this.entity.yHeadRot, this.headLagSpeed);
            this.entity.yBodyRot = approach(this.targetYawHead, this.entity.yBodyRot, this.bodyMaxDelta, this.bodyLagStillSpeed);
        }
        clampHeadBodyDifference();
    }

    private void clampHeadBodyDifference() {
        float diff = Mth.wrapDegrees(this.entity.yHeadRot - this.entity.yBodyRot);
        float clamped = Mth.clamp(diff, -this.maxHeadBodyDiff, this.maxHeadBodyDiff);
        this.entity.yHeadRot = this.entity.yBodyRot + clamped;
    }

    private double delta(double[] arr) {
        return this.mean(arr, 0) - this.mean(arr, 5);
    }
    private double mean(double[] arr, int start) {
        double sum = 0.0;
        int count = arr.length / 2;
        for (int i = 0; i < count; ++i) {
            sum += arr[i + start];
        }
        return sum / (double) count;
    }

    private static float smooth(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    private static float approach(float target, float current, float maxDelta, float speed) {
        float delta = Mth.wrapDegrees(target - current);
        delta = Mth.clamp(delta, -maxDelta, maxDelta);
        return current + delta * speed;
    }

    private boolean shouldLockForSitting() {
        if (!(this.entity instanceof DragonEntity dragon)) {
            return false;
        }
        return dragon.isOrderedToSit() || dragon.getSitProgress() > 0.0f;
    }

    private void freezeSeatedRotation() {
        float yaw = this.entity.getYRot();
        this.entity.yBodyRot = yaw;
        this.entity.yHeadRot = yaw;
        this.targetYawHead = yaw;
    }
}
