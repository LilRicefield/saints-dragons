package com.leon.saintsdragons.forge.mixin.client;

import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Shadow
    protected M model;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;prepareMobModel(Lnet/minecraft/world/entity/Entity;FFF)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void saintsdragons$forceNulljawStandingPose(LivingEntity entity, float entityYaw, float partialTick,
                                                        PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                                        CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player && player.getVehicle() instanceof Nulljaw) {
            this.model.riding = false;
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void saintsdragons$stabilizeNulljawPassengerPose(LivingEntity entity, float entityYaw, float partialTick,
                                                             PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                                             CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player) || !(player.getVehicle() instanceof Nulljaw)) {
            return;
        }
        if (!(this.model instanceof HumanoidModel<?> humanoid)) {
            return;
        }

        humanoid.crouching = false;
        humanoid.rightLeg.xRot = 0.0F;
        humanoid.rightLeg.yRot = 0.0F;
        humanoid.rightLeg.zRot = 0.0F;
        humanoid.leftLeg.xRot = 0.0F;
        humanoid.leftLeg.yRot = 0.0F;
        humanoid.leftLeg.zRot = 0.0F;
    }
}
