package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.DragonWaterSplashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class DragonWaterSplashRenderer extends EntityRenderer<DragonWaterSplashEntity> {
    private static final ResourceLocation[] TEXTURES = {
            SaintsDragonsCommon.rl("textures/particle/watersplash0.png"),
            SaintsDragonsCommon.rl("textures/particle/watersplash1.png"),
            SaintsDragonsCommon.rl("textures/particle/watersplash2.png"),
            SaintsDragonsCommon.rl("textures/particle/watersplash3.png")
    };

    public DragonWaterSplashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull DragonWaterSplashEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        float opacity = entity.getOpacity(partialTicks);
        if (opacity <= 0.001F) {
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        int frame = entity.getAnimationFrame(partialTicks);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURES[frame]));
        renderFrame(consumer, matrix, normalMatrix, frame, entity.getScale(partialTicks), opacity);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private void renderFrame(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                             int frame, float size, float opacity) {
        if (frame == 0) {
            renderHorizontalSquare(consumer, matrix, normalMatrix, 0.0F, 0.0F, size * 0.85F, opacity);
            return;
        }

        float spread = size * (0.35F + frame * 0.38F);
        float frameSize = size * (0.78F + frame * 0.08F);
        renderHorizontalSquare(consumer, matrix, normalMatrix, -spread, 0.0F, frameSize, opacity);
        renderHorizontalSquare(consumer, matrix, normalMatrix, spread, 0.0F, frameSize, opacity);
    }

    private void renderHorizontalSquare(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                        float centerX, float centerZ, float size, float opacity) {
        Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);
        normalMatrix.transform(normal);
        float y = 0.035F;

        consumer.vertex(matrix, centerX - size, y, centerZ - size)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
        consumer.vertex(matrix, centerX - size, y, centerZ + size)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
        consumer.vertex(matrix, centerX + size, y, centerZ + size)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
        consumer.vertex(matrix, centerX + size, y, centerZ - size)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull DragonWaterSplashEntity entity) {
        return TEXTURES[0];
    }
}
