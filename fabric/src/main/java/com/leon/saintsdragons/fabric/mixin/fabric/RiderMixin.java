package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.renderer.EntityPreviewRenderContext;
import com.leon.saintsdragons.client.renderer.DragonRiderAttachments;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class RiderMixin {
    @Inject(
            method = "setModelProperties(Lnet/minecraft/client/player/AbstractClientPlayer;)V",
            at = @At("TAIL")
    )
    private void saintsdragons$nulljawForceStandingPose(AbstractClientPlayer player, CallbackInfo ci) {
        Entity entity = player.getVehicle();
        if (entity instanceof Nulljaw) {
            ((PlayerRenderer) (Object) this).getModel().riding = false;
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void saintsdragons$transformRiderOnDragon(AbstractClientPlayer player, float entityYaw, float partialTick,
                                                       PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                       CallbackInfo ci) {
        if (EntityPreviewRenderContext.isRendering()) {
            return;
        }
        DragonRiderAttachments.transformRider(player, partialTick, poseStack,
                ((PlayerRenderer) (Object) this).getRenderOffset(player, partialTick));
    }
}
