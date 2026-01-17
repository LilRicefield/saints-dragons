package com.leon.saintsdragons.client.renderer.ignivorus;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusNovaRingEntity;
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
import org.joml.Vector3f;

public class IgnivorusNovaRingRenderer extends EntityRenderer<IgnivorusNovaRingEntity> {

    private static final int TOTAL_FRAMES = 5;
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[TOTAL_FRAMES];

    static {
        for (int i = 0; i < TOTAL_FRAMES; i++) {
            TEXTURES[i] = SaintsDragonsCommon.rl("textures/entity/ignivorus/ring" + i + ".png");
        }
    }

    public IgnivorusNovaRingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull IgnivorusNovaRingEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {

        float scale = entity.getScale(partialTicks);
        float opacity = entity.getOpacity(partialTicks);

        if (opacity <= 0.001F) {
            return;
        }

        poseStack.pushPose();

        int frame = (entity.getAge() * TOTAL_FRAMES) / entity.getDuration();
        frame = Math.min(frame, TOTAL_FRAMES - 1);
        ResourceLocation texture = TEXTURES[frame];

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        float size = scale * 16.0F;

        renderHorizontalSquare(consumer, matrix, normalMatrix, size, opacity);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private void renderHorizontalSquare(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                        float size, float opacity) {
        float half = size;

        Vector3f normalVec = new Vector3f(0.0F, 1.0F, 0.0F);
        normalMatrix.transform(normalVec);

        float y = 0.1F;

        consumer.vertex(matrix, -half, y, -half)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normalVec.x(), normalVec.y(), normalVec.z())
                .endVertex();

        consumer.vertex(matrix, -half, y, half)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normalVec.x(), normalVec.y(), normalVec.z())
                .endVertex();

        consumer.vertex(matrix, half, y, half)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normalVec.x(), normalVec.y(), normalVec.z())
                .endVertex();

        consumer.vertex(matrix, half, y, -half)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normalVec.x(), normalVec.y(), normalVec.z())
                .endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull IgnivorusNovaRingEntity entity) {
        return TEXTURES[0];
    }
}
