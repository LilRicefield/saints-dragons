package com.leon.saintsdragons.client.renderer.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

public final class BeamGrainRenderer {
    private static final long SLOT_SEED = 0x9E3779B97F4A7C15L;
    private static final long CYCLE_SEED = 0xD1B54A32D192ED03L;

    private BeamGrainRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource,
                              ResourceLocation[] textures, float beamLength, float visibility,
                              float ageInTicks, long seed, Style style,
                              float red, float green, float blue) {
        if (textures == null || textures.length == 0 || style == null
                || beamLength <= 0.05F || visibility <= 0.01F) {
            return;
        }

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        int frame = textures.length == 1
                ? 0
                : Mth.floor(Math.max(0.0F, ageInTicks)
                        / Math.max(style.frameDurationTicks(), 0.05F)) % textures.length;
        ResourceLocation texture = textures[frame];
        if (texture == null) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));

        for (int slot = 0; slot < style.count(); slot++) {
            long slotSeed = seed ^ SLOT_SEED * (slot + 1L);
            RandomSource slotRandom = RandomSource.create(slotSeed);
            float lifetime = Math.max(1.0F, Mth.lerp(slotRandom.nextFloat(),
                    style.minimumLifetimeTicks(), style.maximumLifetimeTicks()));
            float cycleTime = Math.max(0.0F, ageInTicks)
                    + slotRandom.nextFloat() * lifetime;
            long cycle = Mth.floor(cycleTime / lifetime);
            float life = Mth.frac(cycleTime / lifetime);

            RandomSource cycleRandom = RandomSource.create(slotSeed ^ CYCLE_SEED * (cycle + 1L));
            double angle = cycleRandom.nextDouble() * Mth.TWO_PI;
            float radius = Mth.sqrt(cycleRandom.nextFloat()) * style.maximumOffset() * visibility;
            float offsetX = Mth.cos((float)angle) * radius;
            float offsetY = Mth.sin((float)angle) * radius;
            float randomizedScale = Mth.lerp(cycleRandom.nextFloat(),
                    style.minimumScale(), style.maximumScale());

            float fadeIn = smoothStep(Mth.clamp(life / 0.30F, 0.0F, 1.0F));
            float fadeOut = smoothStep(Mth.clamp((1.0F - life) / 0.35F, 0.0F, 1.0F));
            float alpha = style.alpha() * visibility * fadeIn * fadeOut;
            float halfWidth = style.halfWidth() * randomizedScale * visibility;
            float halfLength = style.halfLength() * style.longitudinalStretch()
                    * randomizedScale * visibility;
            if (alpha <= 0.01F || halfWidth <= 0.001F || halfLength <= 0.001F) {
                continue;
            }

            // Hold each grain at one random beam position for its entire fade cycle.
            // Its next position is chosen only after it has faded completely away.
            float centerZ = cycleRandom.nextFloat() * beamLength;
            float startZ = Math.max(0.0F, centerZ - halfLength);
            float endZ = Math.min(beamLength, centerZ + halfLength);
            if (endZ - startZ <= 0.002F) {
                continue;
            }

            int face = cycleRandom.nextInt(4);
            renderSquareFaceGrain(consumer, matrix, normalMatrix,
                    offsetX, offsetY, startZ, endZ, halfWidth, face,
                    red, green, blue, alpha);
        }
    }

    public static void renderConveyor(PoseStack poseStack, MultiBufferSource bufferSource,
                                      ResourceLocation[] textures, float beamLength, float visibility,
                                      float ageInTicks, long seed, ConveyorStyle style,
                                      float red, float green, float blue) {
        if (textures == null || textures.length == 0 || style == null
                || beamLength <= 0.05F || visibility <= 0.01F) {
            return;
        }

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        // Keep cadence tied to the full-range travel distance. Short beams clip
        // completed grains instead of recycling every slot at a faster rate.
        float travelLength = Math.max(Math.max(
                style.referenceTravelLength(), beamLength), 1.0F);
        float renderLength = beamLength * Mth.clamp(style.beamCoverage(), 0.0F, 1.0F);
        if (renderLength <= 0.002F) {
            return;
        }
        int frame = textures.length == 1
                ? 0
                : Mth.floor(Math.max(0.0F, ageInTicks)
                        / Math.max(style.frameDurationTicks(), 0.05F)) % textures.length;
        ResourceLocation texture = textures[frame];
        if (texture == null) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));

        for (int slot = 0; slot < style.count(); slot++) {
            long slotSeed = seed ^ SLOT_SEED * (slot + 1L);
            RandomSource slotRandom = RandomSource.create(slotSeed);
            float speed = Mth.lerp(slotRandom.nextFloat(),
                    style.minimumSpeed(), style.maximumSpeed());
            float initialDistance = slotRandom.nextFloat() * travelLength;
            float traveled = Math.max(0.0F, ageInTicks) * speed + initialDistance;
            long cycle = Mth.floor(traveled / travelLength);
            float life = Mth.frac(traveled / travelLength);

            RandomSource cycleRandom = RandomSource.create(slotSeed ^ CYCLE_SEED * (cycle + 1L));
            double angle = cycleRandom.nextDouble() * Mth.TWO_PI;
            float radius = Mth.sqrt(cycleRandom.nextFloat())
                    * style.maximumOffset() * visibility;
            float offsetX = Mth.cos((float) angle) * radius;
            float offsetY = Mth.sin((float) angle) * radius;
            float randomizedScale = Mth.lerp(cycleRandom.nextFloat(),
                    style.minimumScale(), style.maximumScale());

            float remaining = 1.0F - life;
            float shrink = smoothStep(remaining);
            float fadeIn = smoothStep(Mth.clamp(life / 0.08F, 0.0F, 1.0F));
            float alpha = style.alpha() * visibility * fadeIn * shrink;
            float halfWidth = style.halfWidth() * randomizedScale * shrink * visibility;
            float halfLength = style.halfLength() * style.longitudinalStretch()
                    * randomizedScale * shrink * visibility;
            if (alpha <= 0.01F || halfWidth <= 0.001F || halfLength <= 0.001F) {
                continue;
            }

            float spawnZ = cycleRandom.nextFloat() * renderLength * 0.85F;
            float centerZ = spawnZ + life * travelLength;
            if (centerZ >= renderLength) {
                continue;
            }
            float startZ = Math.max(0.0F, centerZ - halfLength);
            float endZ = Math.min(renderLength, centerZ + halfLength);
            if (endZ - startZ <= 0.002F) {
                continue;
            }

            float endpointFadeDistance = Math.max(halfLength * 4.0F, renderLength * 0.08F);
            alpha *= smoothStep(Mth.clamp(
                    (renderLength - centerZ) / endpointFadeDistance, 0.0F, 1.0F));
            if (alpha <= 0.01F) {
                continue;
            }

            int face = cycleRandom.nextInt(4);
            renderSquareFaceGrain(consumer, matrix, normalMatrix,
                    offsetX, offsetY, startZ, endZ, halfWidth, face,
                    red, green, blue, alpha);
        }
    }

    private static void renderSquareFaceGrain(VertexConsumer consumer, Matrix4f matrix,
                                              Matrix3f normalMatrix,
                                              float x, float y, float startZ, float endZ,
                                              float halfWidth, int face,
                                              float red, float green, float blue, float alpha) {
        switch (face) {
            case 0 -> emitDoubleSidedQuad(consumer, matrix,
                    transformNormal(normalMatrix, 0.0F, 1.0F, 0.0F),
                    x - halfWidth, y + halfWidth, startZ,
                    x - halfWidth, y + halfWidth, endZ,
                    x + halfWidth, y + halfWidth, endZ,
                    x + halfWidth, y + halfWidth, startZ,
                    red, green, blue, alpha);
            case 1 -> emitDoubleSidedQuad(consumer, matrix,
                    transformNormal(normalMatrix, 0.0F, -1.0F, 0.0F),
                    x + halfWidth, y - halfWidth, startZ,
                    x + halfWidth, y - halfWidth, endZ,
                    x - halfWidth, y - halfWidth, endZ,
                    x - halfWidth, y - halfWidth, startZ,
                    red, green, blue, alpha);
            case 2 -> emitDoubleSidedQuad(consumer, matrix,
                    transformNormal(normalMatrix, 1.0F, 0.0F, 0.0F),
                    x + halfWidth, y + halfWidth, startZ,
                    x + halfWidth, y + halfWidth, endZ,
                    x + halfWidth, y - halfWidth, endZ,
                    x + halfWidth, y - halfWidth, startZ,
                    red, green, blue, alpha);
            default -> emitDoubleSidedQuad(consumer, matrix,
                    transformNormal(normalMatrix, -1.0F, 0.0F, 0.0F),
                    x - halfWidth, y - halfWidth, startZ,
                    x - halfWidth, y - halfWidth, endZ,
                    x - halfWidth, y + halfWidth, endZ,
                    x - halfWidth, y + halfWidth, startZ,
                    red, green, blue, alpha);
        }
    }

    private static Vector3f transformNormal(Matrix3f normalMatrix, float x, float y, float z) {
        Vector3f normal = new Vector3f(x, y, z);
        normalMatrix.transform(normal);
        return normal;
    }

    private static void emitDoubleSidedQuad(VertexConsumer consumer, Matrix4f matrix, Vector3f normal,
                                            float x0, float y0, float z0,
                                            float x1, float y1, float z1,
                                            float x2, float y2, float z2,
                                            float x3, float y3, float z3,
                                            float red, float green, float blue, float alpha) {
        vertex(consumer, matrix, normal, x0, y0, z0, 0.0F, 1.0F, red, green, blue, alpha);
        vertex(consumer, matrix, normal, x1, y1, z1, 0.0F, 0.0F, red, green, blue, alpha);
        vertex(consumer, matrix, normal, x2, y2, z2, 1.0F, 0.0F, red, green, blue, alpha);
        vertex(consumer, matrix, normal, x3, y3, z3, 1.0F, 1.0F, red, green, blue, alpha);

        vertex(consumer, matrix, normal, x3, y3, z3, 1.0F, 1.0F, red, green, blue, alpha);
        vertex(consumer, matrix, normal, x2, y2, z2, 1.0F, 0.0F, red, green, blue, alpha);
        vertex(consumer, matrix, normal, x1, y1, z1, 0.0F, 0.0F, red, green, blue, alpha);
        vertex(consumer, matrix, normal, x0, y0, z0, 0.0F, 1.0F, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vector3f normal,
                               float x, float y, float z, float u, float v,
                               float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    public record Style(int count, float minimumLifetimeTicks, float maximumLifetimeTicks,
                        float maximumOffset, float halfWidth, float halfLength,
                        float longitudinalStretch, float minimumScale,
                        float maximumScale, float alpha,
                        float frameDurationTicks) {
    }

    public record ConveyorStyle(int count, float minimumSpeed, float maximumSpeed,
                                float maximumOffset, float halfWidth, float halfLength,
                                float longitudinalStretch, float minimumScale,
                                float maximumScale, float alpha,
                                float referenceTravelLength,
                                float beamCoverage,
                                float frameDurationTicks) {
    }
}
