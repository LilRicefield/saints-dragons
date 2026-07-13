package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.client.renderer.vfx.BloodTempestAfterimageRenderContext;
import com.leon.saintsdragons.client.renderer.vfx.BloodTempestAfterimageTrail;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class BloodTempestAfterimagePlayerRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL")
    )
    private void saintsdragons$renderBloodTempestAfterimages(AbstractClientPlayer player, float entityYaw,
                                                             float partialTick, PoseStack poseStack,
                                                             MultiBufferSource bufferSource, int packedLight,
                                                             CallbackInfo ci) {
        if (BloodTempestAfterimageRenderContext.isActive() || ShaderPassCompatibility.isIrisShadowPass()) {
            return;
        }

        PlayerRenderer renderer = (PlayerRenderer) (Object) this;
        for (BloodTempestAfterimageTrail.RenderedSnapshot snapshot
                : BloodTempestAfterimageTrail.snapshotsFor(player, partialTick)) {
            Vec3 offset = snapshot.offset();
            poseStack.pushPose();
            poseStack.translate(offset.x, offset.y, offset.z);
            BloodTempestAfterimageRenderContext.begin(snapshot.alpha());
            try {
                renderer.render(player, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            } finally {
                BloodTempestAfterimageRenderContext.end();
                poseStack.popPose();
            }
        }
    }
}
