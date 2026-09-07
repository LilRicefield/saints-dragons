package com.leon.saintsdragons.client.renderer.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import org.joml.Vector3f;

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

        PoseStack.Pose pose = poseStack.last();
        Matrix3f worldToLocal = new Matrix3f(pose.pose()).invert();
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f right = worldToLocal.transform(new Vector3f(camera.getLeftVector()).negate());
        Vector3f up = worldToLocal.transform(new Vector3f(camera.getUpVector()));
        Vector3f rotatedRight = new Vector3f(right).mul(Mth.cos(angle)).fma(Mth.sin(angle), up);
        Vector3f rotatedUp = new Vector3f(up).mul(Mth.cos(angle)).fma(-Mth.sin(angle), right);
        Vector3f normal = new Vector3f(up).cross(right).normalize();
        pose.normal().transform(normal).normalize();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(texture));
        vertex(consumer, pose.pose(), normal, rotatedRight, rotatedUp,
                centerX, centerY, centerZ, -halfSize, -halfSize, 0, 1, red, green, blue, alpha);
        vertex(consumer, pose.pose(), normal, rotatedRight, rotatedUp,
                centerX, centerY, centerZ, -halfSize, halfSize, 0, 0, red, green, blue, alpha);
        vertex(consumer, pose.pose(), normal, rotatedRight, rotatedUp,
                centerX, centerY, centerZ, halfSize, halfSize, 1, 0, red, green, blue, alpha);
        vertex(consumer, pose.pose(), normal, rotatedRight, rotatedUp,
                centerX, centerY, centerZ, halfSize, -halfSize, 1, 1, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vector3f normal,
                               Vector3f right, Vector3f up, float centerX, float centerY, float centerZ,
                               float x, float y, float u, float v,
                               float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, centerX + right.x() * x + up.x() * y,
                        centerY + right.y() * x + up.y() * y,
                        centerZ + right.z() * x + up.z() * y)
                .color(red, green, blue, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal.x(), normal.y(), normal.z()).endVertex();
    }

    private static float smoothStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /** halfSize is in world units; timing is in ticks; turns is signed randomly per flash. */
    public record Style(float halfSize, float lifetimeTicks, float delayTicks, float alpha, float turns) {
    }
}
