package com.leon.saintsdragons.forge.mixin.client;

import com.leon.saintsdragons.forge.client.accessor.CameraAccessor;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraMixin extends CameraAccessor {
    @Invoker("move")
    void saintsdragons$invokeMove(double distance, double yaw, double pitch);

    @Invoker("getMaxZoom")
    double saintsdragons$invokeGetMaxZoom(double distance);

    @Invoker("setPosition")
    void saintsdragons$invokeSetPosition(double x, double y, double z);
}
