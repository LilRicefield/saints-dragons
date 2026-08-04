package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.renderer.vfx.BloodTempestAfterimageRenderContext;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class BloodTempestAfterimageLivingRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Inject(
            method = "getRenderType(Lnet/minecraft/world/entity/LivingEntity;ZZZ)Lnet/minecraft/client/renderer/RenderType;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void saintsdragons$useAfterimageRenderType(T entity, boolean bodyVisible, boolean translucent,
                                                       boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (BloodTempestAfterimageRenderContext.isActive() && entity instanceof AbstractClientPlayer player) {
            cir.setReturnValue(RenderType.entityTranslucent(player.getSkinTextureLocation()));
        }
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            index = 4,
            require = 0
    )
    private float saintsdragons$tintAfterimageRed(float original) {
        return BloodTempestAfterimageRenderContext.isActive()
                ? BloodTempestAfterimageRenderContext.red() : original;
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            index = 5,
            require = 0
    )
    private float saintsdragons$tintAfterimageGreen(float original) {
        return BloodTempestAfterimageRenderContext.isActive()
                ? BloodTempestAfterimageRenderContext.green() : original;
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            index = 6,
            require = 0
    )
    private float saintsdragons$tintAfterimageBlue(float original) {
        return BloodTempestAfterimageRenderContext.isActive()
                ? BloodTempestAfterimageRenderContext.blue() : original;
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            index = 7,
            require = 0
    )
    private float saintsdragons$fadeAfterimage(float original) {
        return BloodTempestAfterimageRenderContext.isActive()
                ? BloodTempestAfterimageRenderContext.alpha() : original;
    }

    @Inject(
            method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void saintsdragons$hideAfterimageName(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (BloodTempestAfterimageRenderContext.isActive()) {
            cir.setReturnValue(false);
        }
    }
}
