package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.GroundCrackEntity;
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

public class GroundCrackRenderer extends EntityRenderer<GroundCrackEntity> {
    private static final ResourceLocation STEGONAUT_TEXTURE = SaintsDragonsCommon.rl("textures/particle/ground_crack.png");
    private static final ResourceLocation DRAGONLORD_FISSURE_TEXTURE = SaintsDragonsCommon.rl("textures/particle/ground_crack_fissure.png");

    public GroundCrackRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull GroundCrackEntity entity, float entityYaw, float partialTicks,
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
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));
        renderHorizontalSquare(consumer, matrix, normalMatrix, entity.getScale(partialTicks), opacity);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private void renderHorizontalSquare(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                        float size, float opacity) {
        Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);
        normalMatrix.transform(normal);
        float y = GroundCrackEntity.RENDER_PLANE_Y;

        consumer.vertex(matrix, -size, y, -size)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
        consumer.vertex(matrix, -size, y, size)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
        consumer.vertex(matrix, size, y, size)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
        consumer.vertex(matrix, size, y, -size)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull GroundCrackEntity entity) {
        return entity.isDragonlordFissure() ? DRAGONLORD_FISSURE_TEXTURE : STEGONAUT_TEXTURE;
    }
}
