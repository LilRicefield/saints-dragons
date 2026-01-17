package com.leon.saintsdragons.client.renderer.ignivorus;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusNovaEntity;
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

public class IgnivorusNovaRenderer extends EntityRenderer<IgnivorusNovaEntity> {

    private static final int TOTAL_FRAMES = 8;
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[TOTAL_FRAMES];

    static {
        for (int i = 0; i < TOTAL_FRAMES; i++) {
            TEXTURES[i] = SaintsDragonsCommon.rl("textures/entity/ignivorus/nova" + i + ".png");
        }
    }

    public IgnivorusNovaRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull IgnivorusNovaEntity entity, float entityYaw, float partialTicks,
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
        Matrix3f normal = pose.normal();

        float s = scale * 16.0F;

        addFace(consumer, matrix, normal, -s, s, s, s, s, s, s, -s, s, -s, -s, s, 0, 0, 1, opacity);
        addFace(consumer, matrix, normal, s, s, -s, -s, s, -s, -s, -s, -s, s, -s, -s, 0, 0, -1, opacity);
        addFace(consumer, matrix, normal, s, s, s, s, s, -s, s, -s, -s, s, -s, s, 1, 0, 0, opacity);
        addFace(consumer, matrix, normal, -s, s, -s, -s, s, s, -s, -s, s, -s, -s, -s, -1, 0, 0, opacity);
        addFace(consumer, matrix, normal, -s, s, -s, s, s, -s, s, s, s, -s, s, s, 0, 1, 0, opacity);
        addFace(consumer, matrix, normal, s, -s, -s, -s, -s, -s, -s, -s, s, s, -s, s, 0, -1, 0, opacity);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private void addFace(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         float nx, float ny, float nz, float opacity) {

        consumer.vertex(matrix, x1, y1, z1)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normalMatrix, nx, ny, nz)
                .endVertex();

        consumer.vertex(matrix, x2, y2, z2)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normalMatrix, nx, ny, nz)
                .endVertex();

        consumer.vertex(matrix, x3, y3, z3)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normalMatrix, nx, ny, nz)
                .endVertex();

        consumer.vertex(matrix, x4, y4, z4)
                .color(1.0F, 1.0F, 1.0F, opacity)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(normalMatrix, nx, ny, nz)
                .endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull IgnivorusNovaEntity entity) {
        return TEXTURES[0];
    }
}
