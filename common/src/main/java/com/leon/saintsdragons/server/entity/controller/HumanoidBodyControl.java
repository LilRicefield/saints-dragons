package com.leon.saintsdragons.server.entity.controller;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;

public class HumanoidBodyControl extends BodyRotationControl {
    private static final double MOVING_EPSILON_SQ = 1.0E-4;
    private static final int HISTORY_SIZE = 10;

    private final Mob entity;
    private final double[] histPosX = new double[HISTORY_SIZE];
    private final double[] histPosZ = new double[HISTORY_SIZE];
    private final float movingTurnSpeed;
    private final float stillTurnSpeed;
    private final float maxTurnDelta;
    private final float maxHeadBodyDiff;

    public HumanoidBodyControl(Mob entity, float movingTurnSpeed, float stillTurnSpeed, float maxTurnDelta, float maxHeadBodyDiff) {
        super(entity);
        this.entity = entity;
        this.movingTurnSpeed = movingTurnSpeed;
        this.stillTurnSpeed = stillTurnSpeed;
        this.maxTurnDelta = maxTurnDelta;
        this.maxHeadBodyDiff = maxHeadBodyDiff;
    }

    @Override
    public void clientTick() {
        tickBody();
    }

    public void serverTick() {
        tickBody();
    }

    protected void tickBody() {
        if (this.entity.isVehicle()) {
            return;
        }

        for (int i = this.histPosX.length - 1; i > 0; --i) {
            this.histPosX[i] = this.histPosX[i - 1];
            this.histPosZ[i] = this.histPosZ[i - 1];
        }
        this.histPosX[0] = this.entity.getX();
        this.histPosZ[0] = this.entity.getZ();

        double dx = delta(this.histPosX);
        double dz = delta(this.histPosZ);
        double distSq = dx * dx + dz * dz;
        if (distSq > MOVING_EPSILON_SQ) {
            float moveYaw = (float) Math.toDegrees(Mth.atan2(dz, dx)) - 90.0F;
            this.entity.yBodyRot = approach(moveYaw, this.entity.yBodyRot, this.maxTurnDelta, this.movingTurnSpeed);
        } else {
            this.entity.yBodyRot = approach(this.entity.yHeadRot, this.entity.yBodyRot, this.maxTurnDelta, this.stillTurnSpeed);
        }

        float diff = Mth.wrapDegrees(this.entity.yHeadRot - this.entity.yBodyRot);
        this.entity.yHeadRot = this.entity.yBodyRot + Mth.clamp(diff, -this.maxHeadBodyDiff, this.maxHeadBodyDiff);
    }

    public void lockBodyToYaw(float yaw, float turnSpeed) {
        this.entity.yBodyRot = approach(yaw, this.entity.yBodyRot, this.maxTurnDelta, turnSpeed);
        this.entity.yHeadRot = approach(yaw, this.entity.yHeadRot, this.maxTurnDelta, turnSpeed);
    }

    public void snapBodyToYaw(float yaw) {
        this.entity.yBodyRot = yaw;
        this.entity.yHeadRot = yaw;
    }

    private double delta(double[] arr) {
        return mean(arr, 0) - mean(arr, 5);
    }

    private double mean(double[] arr, int start) {
        double sum = 0.0;
        int count = arr.length / 2;
        for (int i = 0; i < count; ++i) {
            sum += arr[i + start];
        }
        return sum / (double) count;
    }

    private static float approach(float target, float current, float maxDelta, float speed) {
        float delta = Mth.wrapDegrees(target - current);
        delta = Mth.clamp(delta, -maxDelta, maxDelta);
        return current + delta * speed;
    }
}
