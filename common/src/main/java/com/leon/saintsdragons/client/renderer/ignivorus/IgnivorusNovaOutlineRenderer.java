package com.leon.saintsdragons.client.renderer.ignivorus;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusNovaOutlineEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class IgnivorusNovaOutlineRenderer extends EntityRenderer<IgnivorusNovaOutlineEntity> {

    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/ignivorus/nova0.png");

    public IgnivorusNovaOutlineRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull IgnivorusNovaOutlineEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {

        float scale = entity.getScale(partialTicks);
        float opacity = entity.getOpacity(partialTicks);

        if (opacity <= 0.001F) {
            return;
        }

        poseStack.pushPose();

        float age = entity.getAge() + partialTicks;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(age * 5.0F));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(age * 3.5F));

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        float s = scale * 16.0F;

        renderCubeOutline(consumer, matrix, normal, s, opacity);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private void renderCubeOutline(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float s, float opacity) {
        addLine(consumer, matrix, normal, -s, -s, -s, s, -s, -s, opacity);
        addLine(consumer, matrix, normal, s, -s, -s, s, s, -s, opacity);
        addLine(consumer, matrix, normal, s, s, -s, -s, s, -s, opacity);
        addLine(consumer, matrix, normal, -s, s, -s, -s, -s, -s, opacity);

        addLine(consumer, matrix, normal, -s, -s, s, s, -s, s, opacity);
        addLine(consumer, matrix, normal, s, -s, s, s, s, s, opacity);
        addLine(consumer, matrix, normal, s, s, s, -s, s, s, opacity);
        addLine(consumer, matrix, normal, -s, s, s, -s, -s, s, opacity);

        addLine(consumer, matrix, normal, -s, -s, -s, -s, -s, s, opacity);
        addLine(consumer, matrix, normal, s, -s, -s, s, -s, s, opacity);
        addLine(consumer, matrix, normal, s, s, -s, s, s, s, opacity);
        addLine(consumer, matrix, normal, -s, s, -s, -s, s, s, opacity);
    }

    private void addLine(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2, float opacity) {
        consumer.vertex(matrix, x1, y1, z1)
                .color(1.0F, 1.0F, 0.8F, opacity)
                .normal(normal, 0, 1, 0)
                .endVertex();
        consumer.vertex(matrix, x2, y2, z2)
                .color(1.0F, 1.0F, 0.8F, opacity)
                .normal(normal, 0, 1, 0)
                .endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull IgnivorusNovaOutlineEntity entity) {
        return TEXTURE;
    }
}
