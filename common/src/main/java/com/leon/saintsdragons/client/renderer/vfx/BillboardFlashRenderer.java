package com.leon.saintsdragons.client.renderer.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class BillboardFlashRenderer {
    private static final long CYCLE_SEED = 0xD1B54A32D192ED03L;

    private BillboardFlashRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers,
                              ResourceLocation texture, float centerX, float centerY, float centerZ,
                              float visibility, float ageInTicks, long seed, Style style,
                              float red, float green, float blue) {
        if (texture == null || style == null || visibility <= 0.01F) {
            return;
        }
        visibility = Mth.clamp(visibility, 0.0F, 1.0F);
        float lifetime = Math.max(0.5F, style.lifetimeTicks());
        float period = lifetime + Math.max(0.0F, style.delayTicks());
        RandomSource phaseRandom = RandomSource.create(seed);
        float cycleTime = Math.max(0.0F, ageInTicks) + phaseRandom.nextFloat() * period;
        long cycle = Mth.floor(cycleTime / period);
        float timeInCycle = Mth.frac(cycleTime / period) * period;
        if (timeInCycle >= lifetime) {
            return;
        }

        float life = timeInCycle / lifetime;
        float halfSize = style.halfSize() * visibility * (1.0F - smoothStep(life));
        float alpha = Mth.clamp(style.alpha(), 0.0F, 1.0F) * visibility
                * smoothStep(life / 0.12F) * smoothStep(1.0F - life);
        if (alpha <= 0.01F || halfSize <= 0.001F) {
            return;
        }
        RandomSource random = RandomSource.create(seed ^ CYCLE_SEED * (cycle + 1L));
        float startingAngle = random.nextFloat() * Mth.TWO_PI;
        float turns = random.nextBoolean() ? style.turns() : -style.turns();
        float angle = startingAngle + turns * Mth.TWO_PI * life;

        renderQuad(poseStack, buffers, texture, centerX, centerY, centerZ,
                halfSize, angle, red, green, blue, alpha);
    }

    /** Shared camera-facing geometry for flashes with externally controlled motion and lifetime.
     * Supply an unrotated entity/world pose; size and center are in world units, angle in radians. */
    public static void renderQuad(PoseStack poseStack, MultiBufferSource buffers,
                                  ResourceLocation texture, float centerX, float centerY, float centerZ,
                                  float halfSize, float angle,
                                  float red, float green, float blue, float alpha) {
        renderAnchoredQuad(poseStack, buffers, texture, centerX, centerY, centerZ,
                halfSize, angle, red, green, blue, alpha, 0.5F, 0.5F);
    }

    /** Keep a chosen texture point at the supplied position while rotating the billboard.
     * Texture coordinates run from (0,0) at the top-left to (1,1) at the bottom-right. */
    public static void renderAnchoredQuad(PoseStack poseStack, MultiBufferSource buffers,
                                          ResourceLocation texture, float centerX, float centerY, float centerZ,
                                          float halfSize, float angle,
                                          float red, float green, float blue, float alpha,
                                          float anchorU, float anchorV) {
        if (texture == null || halfSize <= 0.001F || alpha <= 0.01F) {
            return;
        }
        poseStack.pushPose();
        try {
            poseStack.translate(centerX, centerY, centerZ);
            // Position first, face the viewer, then rotate only within the billboard plane.
            poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            poseStack.mulPose(Axis.ZP.rotation(angle));
            poseStack.translate(-(anchorU * 2.0F - 1.0F) * halfSize,
                    -(1.0F - anchorV * 2.0F) * halfSize, 0.0F);
            PoseStack.Pose pose = poseStack.last();
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(texture));
            vertex(consumer, pose.pose(), pose.normal(), -halfSize, -halfSize, 0, 1, red, green, blue, alpha);
            vertex(consumer, pose.pose(), pose.normal(), -halfSize, halfSize, 0, 0, red, green, blue, alpha);
            vertex(consumer, pose.pose(), pose.normal(), halfSize, halfSize, 1, 0, red, green, blue, alpha);
            vertex(consumer, pose.pose(), pose.normal(), halfSize, -halfSize, 1, 1, red, green, blue, alpha);
        } finally {
            poseStack.popPose();
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float u, float v,
                               float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, x, y, 0.0F)
                .color(red, green, blue, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
    }

    private static float smoothStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public record Style(float halfSize, float lifetimeTicks, float delayTicks, float alpha, float turns) {
    }
}
