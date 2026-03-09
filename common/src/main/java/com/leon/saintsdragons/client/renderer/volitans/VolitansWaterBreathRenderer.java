package com.leon.saintsdragons.client.renderer.volitans;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansWaterBreathEntity;
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

public class VolitansWaterBreathRenderer extends EntityRenderer<VolitansWaterBreathEntity> {
    private static final float SPRITE_WIDTH_PX = 32.0F;
    private static final float SPRITE_HEIGHT_PX = 32.0F;
    private static final int TOTAL_FRAMES = 5;
    private static final ResourceLocation[] WATER_TEXTURES = new ResourceLocation[TOTAL_FRAMES];
    private static final ResourceLocation[] POISON_TEXTURES = new ResourceLocation[TOTAL_FRAMES];

    static {
        for (int i = 0; i < TOTAL_FRAMES; i++) {
            WATER_TEXTURES[i] = SaintsDragonsCommon.rl("textures/entity/volitans/water" + i + ".png");
            POISON_TEXTURES[i] = SaintsDragonsCommon.rl("textures/entity/volitans/poison" + i + ".png");
        }
    }

    public VolitansWaterBreathRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull VolitansWaterBreathEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        float age = entity.getAge() + partialTicks;
        float life = Math.max(1.0F, entity.getMaxAge());
        float normalized = Mth.clamp(age / life, 0.0F, 1.0F);
        int frame = ((int) (age / 3.0F)) % TOTAL_FRAMES;
        if (frame < 0) {
            frame += TOTAL_FRAMES;
        }
        ResourceLocation texture = entity.isPoisonMode() ? POISON_TEXTURES[frame] : WATER_TEXTURES[frame];
        float alpha = Mth.lerp(normalized, 1.0F, 0.82F);
        float scale = Mth.lerp(normalized, 0.34F, 0.72F);

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(texture));

        float halfWidth = SPRITE_WIDTH_PX / 32.0F;
        float halfHeight = SPRITE_HEIGHT_PX / 32.0F;
        addVertex(vc, matrix4f, matrix3f, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, alpha);
        addVertex(vc, matrix4f, matrix3f, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, alpha);
        addVertex(vc, matrix4f, matrix3f, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, alpha);
        addVertex(vc, matrix4f, matrix3f, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, alpha);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private void addVertex(VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f,
                           float x, float y, float z, float u, float v, float alpha) {
        consumer.vertex(matrix4f, x, y, z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull VolitansWaterBreathEntity entity) {
        return entity.isPoisonMode() ? POISON_TEXTURES[0] : WATER_TEXTURES[0];
    }
}
