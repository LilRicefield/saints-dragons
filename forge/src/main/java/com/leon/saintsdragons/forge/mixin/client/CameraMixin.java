package com.leon.saintsdragons.forge.mixin.client;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraMixin {
    @Invoker("move")
    void invokeMove(double distance, double yaw, double pitch);

    @Invoker("getMaxZoom")
    double invokeGetMaxZoom(double distance);
}
