package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.fabric.client.camera.DragonCameraState;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraRollMixin {
    @Unique
    private float saintsdragons$tempRoll = 0.0f;

    @Inject(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            ),
            require = 0
    )
    private void saintsdragons$prepareFirstRotationRoll(
            BlockGetter area,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float partialTick,
            CallbackInfo ci
    ) {
        this.saintsdragons$tempRoll = DragonCameraState.getCurrentRoll();
    }

    @Inject(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE
            ),
            require = 0
    )
    private void saintsdragons$prepareSecondRotationRoll(
            BlockGetter area,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float partialTick,
            CallbackInfo ci
    ) {
        this.saintsdragons$tempRoll = -DragonCameraState.getCurrentRoll();
    }

    @Inject(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
                    ordinal = 2,
                    shift = At.Shift.BEFORE
            ),
            require = 0
    )
    private void saintsdragons$prepareThirdRotationRoll(
            BlockGetter area,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float partialTick,
            CallbackInfo ci
    ) {
        this.saintsdragons$tempRoll = 0.0f;
    }

    @ModifyArg(
            method = "setRotation",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;"
            ),
            index = 2
    )
    private float saintsdragons$injectRollIntoCamera(float originalRoll) {
        float roll = this.saintsdragons$tempRoll;
        return roll != 0.0f ? roll * ((float) Math.PI / 180.0f) : originalRoll;
    }
}
