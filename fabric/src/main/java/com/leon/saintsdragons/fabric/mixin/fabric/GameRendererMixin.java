package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.fabric.client.event.FabricClientEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(
            method = "renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareCullFrustum(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;Lorg/joml/Matrix4f;)V"
            )
    )
    private void saintsdragons$applyFirstPersonRollToView(
            float partialTick,
            long nanoTime,
            PoseStack poseStack,
            CallbackInfo ci
    ) {
        float roll = FabricClientEventHandler.getCurrentFirstPersonRoll();
        if (Math.abs(roll) < 0.0001f) {
            return;
        }

        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        Matrix3f inverseView = new Matrix3f(poseStack.last().normal()).invert();
        RenderSystem.setInverseViewRotationMatrix(inverseView);
    }
}
