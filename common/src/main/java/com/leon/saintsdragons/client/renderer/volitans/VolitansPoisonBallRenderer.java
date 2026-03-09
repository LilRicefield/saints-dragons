package com.leon.saintsdragons.client.renderer.volitans;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansPoisonBallEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class VolitansPoisonBallRenderer extends EntityRenderer<VolitansPoisonBallEntity> {
    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/volitans/poison_ball.png");

    public VolitansPoisonBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull VolitansPoisonBallEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getVisualScale() * 0.45F;

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));

        addVertex(vc, matrix4f, matrix3f, -1.0F, -1.0F, 0.0F, 0.0F, 1.0F);
        addVertex(vc, matrix4f, matrix3f, -1.0F, 1.0F, 0.0F, 0.0F, 0.0F);
        addVertex(vc, matrix4f, matrix3f, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F);
        addVertex(vc, matrix4f, matrix3f, 1.0F, -1.0F, 0.0F, 1.0F, 1.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void addVertex(VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f,
                           float x, float y, float z, float u, float v) {
        consumer.vertex(matrix4f, x, y, z)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull VolitansPoisonBallEntity entity) {
        return TEXTURE;
    }
}

