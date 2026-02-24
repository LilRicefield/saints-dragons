package com.leon.saintsdragons.forge.client.accessor;

public interface CameraAccessor {
    void saintsdragons$invokeMove(double distance, double yaw, double pitch);
    double saintsdragons$invokeGetMaxZoom(double distance);
    void saintsdragons$invokeSetPosition(double x, double y, double z);
}
