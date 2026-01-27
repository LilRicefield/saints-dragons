package com.leon.saintsdragons.client.renderer.ignivorus;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusFlameEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renders Ignivorus flame projectiles as camera-facing billboards with animated textures.
 */
public class IgnivorusFlameRenderer extends EntityRenderer<IgnivorusFlameEntity> {

    private static final int TOTAL_FRAMES = 16;
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[TOTAL_FRAMES];

    static {
        for (int i = 0; i < TOTAL_FRAMES; i++) {
            TEXTURES[i] = SaintsDragonsCommon.rl("textures/entity/ignivorus/fireparticle" + i + ".png");
        }
    }

    public IgnivorusFlameRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull IgnivorusFlameEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        int age = entity.getAge();
        int frame = (age / 2) % TOTAL_FRAMES;

        ResourceLocation texture = TEXTURES[frame];

        // Progressive scale growth - starts at base scale, grows to 2x by end
        int lifetime = entity.getLifetime();
        float ageRatio = Mth.clamp((float) age / (float) lifetime, 0.0F, 1.0F);
        float baseScale = entity.getScale();
        float growthMultiplier = 1.0F + ageRatio; // 1.0 at start, 2.0 at end
        float scale = baseScale * growthMultiplier;
        float alpha = 1.0F;

        poseStack.pushPose();

        // Scale the billboard
        poseStack.scale(scale, scale, scale);

        // Apply camera-facing rotation (billboard effect)
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        // Get matrix for rendering
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();

        // Get vertex consumer - use entityCutoutNoCull for better visibility during testing
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));

        // Render quad (camera-facing billboard)
        renderBillboard(vertexConsumer, matrix4f, matrix3f, packedLight, alpha);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private void renderBillboard(VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f, int packedLight, float alpha) {
        float size = 1.0F;

        // Render both front and back faces to ensure visibility
        // Front face (counter-clockwise when viewed from front)
        addVertex(consumer, matrix4f, matrix3f, -size, -size, 0.0F, 0.0F, 1.0F, alpha);
        addVertex(consumer, matrix4f, matrix3f, -size, size, 0.0F, 0.0F, 0.0F, alpha);
        addVertex(consumer, matrix4f, matrix3f, size, size, 0.0F, 1.0F, 0.0F, alpha);
        addVertex(consumer, matrix4f, matrix3f, size, -size, 0.0F, 1.0F, 1.0F, alpha);
    }

    private void addVertex(VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f,
                          float x, float y, float z, float u, float v, float alpha) {
        consumer.vertex(matrix4f, x, y, z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240) // Full brightness
                .normal(matrix3f, 0.0F, 0.0F, 1.0F) // Normal pointing forward
                .endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull IgnivorusFlameEntity entity) {
        return TEXTURES[0]; // Default texture (not actually used in render method)
    }
}
