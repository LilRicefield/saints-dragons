package com.leon.saintsdragons.client.renderer.ignivorus;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusFireSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class IgnivorusFireSlashRenderer extends EntityRenderer<IgnivorusFireSlashEntity> {

    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/ignivorus/fire_slash.png");

    public IgnivorusFireSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull IgnivorusFireSlashEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {

        int age = entity.getAge();
        int lifetime = Math.max(entity.getLifetime(), 1);
        float ageRatio = Mth.clamp((age + partialTicks) / (float) lifetime, 0.0F, 1.0F);
        float alpha = 1.0F;
        if (ageRatio < 0.1F) {
            alpha = ageRatio / 0.1F;
        } else if (ageRatio > 0.8F) {
            alpha = (1.0F - ageRatio) / 0.2F;
        }
        alpha = Mth.clamp(alpha, 0.0F, 1.0F);

        if (alpha <= 0.001F) {
            return;
        }

        poseStack.pushPose();
        poseStack.scale(entity.getScale(), entity.getScale(), entity.getScale());
        // Orient the slash to its movement direction instead of camera-facing.
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6) {
            float yaw = (float) (Mth.atan2(motion.z, motion.x) * (180.0F / Math.PI)) - 90.0F;
            float pitch = (float) (-(Mth.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z)) * (180.0F / Math.PI)));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
        }

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        renderBillboard(consumer, matrix4f, matrix3f, alpha);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private void renderBillboard(VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f, float alpha) {
        float size = 1.0F;

        consumer.vertex(matrix4f, -size, -size, 0.0F)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();

        consumer.vertex(matrix4f, -size, size, 0.0F)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();

        consumer.vertex(matrix4f, size, size, 0.0F)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();

        consumer.vertex(matrix4f, size, -size, 0.0F)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull IgnivorusFireSlashEntity entity) {
        return TEXTURE;
    }
}
