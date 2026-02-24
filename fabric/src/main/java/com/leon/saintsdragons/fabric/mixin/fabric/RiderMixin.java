package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.render.RiderConfig;
import com.leon.saintsdragons.client.render.RiderBullcrap;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class RiderMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void saintsdragons$transformRiderOnDragon(AbstractClientPlayer player, float entityYaw, float partialTick,
                                                       PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                       CallbackInfo ci) {
        Entity entity = player.getVehicle();
        if (!(entity instanceof RideableDragonBase dragon)) {
            return;
        }
        RiderConfig.RiderSpec riderSpec = RiderConfig.getSpec(dragon);
        if (riderSpec == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastRender = RiderBullcrap.getLastRenderTime(dragon.getId());
        if (now - lastRender > riderSpec.staleMs) {
            return;
        }

        Matrix4f viewMatrix = RiderBullcrap.get(dragon.getId());
        if (viewMatrix == null) {
            return;
        }

        long lastUpdate = RiderBullcrap.getTimestamp(dragon.getId());
        if (now - lastUpdate > riderSpec.staleMs) {
            return;
        }

        Vector3f seatOffset = RiderConfig.getSeatOffset(dragon);
        float seatOffsetX = seatOffset.x();
        float seatOffsetY = seatOffset.y();
        float seatOffsetZ = seatOffset.z();

        Matrix4f playerMatrix = new Matrix4f((Matrix4fc) viewMatrix);
        playerMatrix.normalize3x3();
        poseStack.last().pose().set((Matrix4fc) playerMatrix);

        poseStack.translate(seatOffsetX, seatOffsetY, seatOffsetZ);
        float dragonYaw = Mth.rotLerp(partialTick, dragon.yBodyRotO, dragon.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(dragonYaw + riderSpec.yawOffsetDeg));
    }
}
