package com.leon.saintsdragons.forge.mixin.client;

import com.leon.saintsdragons.forge.client.camera.DragonCameraState;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public class CameraRollMixin {
    @ModifyArg(
            method = "setRotation",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;",
                    remap = false
            ),
            index = 2
    )
    private float saintsdragons$injectRollIntoCamera(float originalRoll) {
        float roll = DragonCameraState.getCurrentRoll();
        return roll != 0.0f ? roll * ((float) Math.PI / 180.0f) : originalRoll;
    }
}
