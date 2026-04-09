package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.fabric.client.camera.DragonCameraState;
import com.leon.saintsdragons.fabric.config.FabricClientConfigAccess;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    private Minecraft minecraft;

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;",
                    ordinal = 2
            )
    )
    private void saintsdragons$applyDragonFirstPersonRoll(float partialTick, long finishNanoTime, PoseStack poseStack, CallbackInfo ci) {
        if (this.minecraft == null || this.minecraft.options == null) {
            return;
        }
        if (this.minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }
        if (!FabricClientConfigAccess.isFirstPersonBankingCameraEnabled()) {
            return;
        }

        Entity cameraEntity = this.minecraft.getCameraEntity();
        if (cameraEntity == null) {
            return;
        }

        Entity vehicle = cameraEntity.getVehicle();
        if (!(vehicle instanceof RideableDragonBase dragon) || !saintsdragons$usesFirstPersonDragonRoll(dragon)) {
            return;
        }

        if (dragon instanceof Raevyx raevyx && raevyx.isBeaming()) {
            return;
        }

        float roll = DragonCameraState.getCurrentRoll();
        if (Math.abs(roll) < 0.01f) {
            return;
        }

        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
    }

    private static boolean saintsdragons$usesFirstPersonDragonRoll(RideableDragonBase dragon) {
        return dragon instanceof Raevyx
                || dragon instanceof Cindervane
                || dragon instanceof Ignivorus
                || dragon instanceof Volitans;
    }
}
