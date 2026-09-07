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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BeamStarFlashRenderer {
    private static final long SLOT_SEED = 0x9E3779B97F4A7C15L;
    private static final long CYCLE_SEED = 0xD1B54A32D192ED03L;

    private BeamStarFlashRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource,
                              ResourceLocation texture, float beamLength, float visibility,
                              float ageInTicks, long seed, Style style,
                              Vec3 beamStartWorld, Vec3 beamEndWorld,
                              float red, float green, float blue, boolean firstPersonView) {
        renderFlashes(poseStack, bufferSource, texture, null, 1.0F,
                beamLength, visibility, ageInTicks, seed, style, beamStartWorld, beamEndWorld,
                red, green, blue, firstPersonView);
    }

    public static void renderAnimated(PoseStack poseStack, MultiBufferSource bufferSource,
                                      ResourceLocation[] textures, float frameDurationTicks,
                                      float beamLength, float visibility, float ageInTicks,
                                      long seed, Style style, Vec3 beamStartWorld, Vec3 beamEndWorld,
                                      float red, float green, float blue, boolean firstPersonView) {
        if (textures == null || textures.length == 0) {
            return;
        }
        renderFlashes(poseStack, bufferSource, null, textures, frameDurationTicks,
                beamLength, visibility, ageInTicks, seed, style, beamStartWorld, beamEndWorld,
                red, green, blue, firstPersonView);
    }

    private static void renderFlashes(PoseStack poseStack, MultiBufferSource bufferSource,
                                     ResourceLocation texture, ResourceLocation[] textures,
                                     float frameDurationTicks, float beamLength, float visibility,
                                     float ageInTicks, long seed, Style style,
                                     Vec3 beamStartWorld, Vec3 beamEndWorld,
                                     float red, float green, float blue, boolean firstPersonView) {
        if ((texture == null && textures == null) || style == null
                || beamStartWorld == null || beamEndWorld == null
                || beamLength <= 0.05F || visibility <= 0.01F) {
            return;
        }

        float renderLength = beamLength * Mth.clamp(style.beamCoverage(), 0.0F, 1.0F);
        if (renderLength <= 0.002F) {
            return;
        }

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        Matrix3f worldToLocal = new Matrix3f(normalMatrix).invert();
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f ribbonRight = new Vector3f(camera.getLeftVector()).negate();
        worldToLocal.transform(ribbonRight);
        ribbonRight.z = 0.0F;
        if (ribbonRight.lengthSquared() <= 1.0E-6F) {
            ribbonRight.set(1.0F, 0.0F, 0.0F);
        }
        ribbonRight.normalize();
        // Match the beam: camera-right for the rider, camera-facing crossed
        // longitudinal planes for everyone else.
        if (!firstPersonView) {
            Vec3 view = camera.getPosition().subtract(beamStartWorld.add(beamEndWorld).scale(0.5D));
            Vector3f localView = new Vector3f((float) view.x, (float) view.y, (float) view.z);
            worldToLocal.transform(localView);
            if (localView.lengthSquared() > 1.0E-6F) {
                localView.normalize();
                float radial = Mth.sqrt(localView.x() * localView.x() + localView.y() * localView.y());
                if (radial > 1.0E-5F) {
                    Vector3f viewRight = new Vector3f(-localView.y(), localView.x(), 0.0F).normalize();
                    if (viewRight.dot(ribbonRight) < 0.0F) {
                        viewRight.negate();
                    }
                    ribbonRight.lerp(viewRight, smoothStep((radial - 0.05F) / 0.15F)).normalize();
                }
            }
        }
        Vector3f companionRight = new Vector3f(-ribbonRight.y(), ribbonRight.x(), 0.0F);
        Vector3f beamForward = new Vector3f(0.0F, 0.0F, 1.0F);
        float time = Math.max(0.0F, ageInTicks);
        float frameTicks = Math.max(0.05F, frameDurationTicks);

        for (int slot = 0; slot < style.count(); slot++) {
            long slotSeed = seed ^ SLOT_SEED * (slot + 1L);
            RandomSource slotRandom = RandomSource.create(slotSeed);
            float lifetime = Math.max(0.5F, Mth.lerp(slotRandom.nextFloat(),
                    style.minimumLifetimeTicks(), style.maximumLifetimeTicks()));
            if (textures != null) {
                // Animated flashes live for exactly one full sprite sequence.
                lifetime = textures.length * frameTicks;
            }
            float delay = Math.max(0.0F, Mth.lerp(slotRandom.nextFloat(),
                    style.minimumDelayTicks(), style.maximumDelayTicks()));
            float period = lifetime + delay;
            float cycleTime = time + slotRandom.nextFloat() * period;
            long cycle = Mth.floor(cycleTime / period);
            float timeInCycle = Mth.frac(cycleTime / period) * period;
            if (timeInCycle >= lifetime) {
                continue;
            }

            float life = timeInCycle / lifetime;
            RandomSource cycleRandom = RandomSource.create(slotSeed ^ CYCLE_SEED * (cycle + 1L));
            float randomizedScale = Mth.lerp(cycleRandom.nextFloat(),
                    style.minimumScale(), style.maximumScale());
            float startingSize = style.halfSize() * randomizedScale * visibility;
            float inset = Math.min(startingSize, renderLength * 0.5F);
            float centerZ = Mth.lerp(cycleRandom.nextFloat(), inset, renderLength - inset);
            float speed = Mth.lerp(cycleRandom.nextFloat(), style.minimumSpeed(), style.maximumSpeed());
            centerZ += timeInCycle * speed;
            if (centerZ >= renderLength) {
                continue;
            }
            float radialAngle = cycleRandom.nextFloat() * Mth.TWO_PI;
            float radialOffset = Math.max(0.0F, style.surfaceOffset()
                    + (cycleRandom.nextFloat() * 2.0F - 1.0F) * style.lateralJitter())
                    * visibility;
            float centerX = Mth.cos(radialAngle) * radialOffset;
            float centerY = Mth.sin(radialAngle) * radialOffset;
            float startingAngle = cycleRandom.nextFloat() * Mth.TWO_PI;
            float turns = Mth.lerp(cycleRandom.nextFloat(),
                    style.minimumTurns(), style.maximumTurns());
            if (cycleRandom.nextBoolean()) {
                turns = -turns;
            }

            // Let animated artwork control its own appearance and disappearance.
            float fadeIn = textures != null ? 1.0F : smoothStep(Mth.clamp(life / 0.12F, 0.0F, 1.0F));
            float fadeOut = textures != null ? 1.0F : smoothStep(1.0F - life);
            float shrink = textures != null ? 1.0F : 1.0F - smoothStep(life);
            float alpha = style.alpha() * visibility * fadeIn * fadeOut;
            float halfSize = startingSize * shrink;
            // Fade before the rotating quad reaches either end of the beam.
            float endFadeDistance = Math.max(startingSize * 2.0F, 0.01F);
            alpha *= smoothStep(centerZ / endFadeDistance)
                    * smoothStep((renderLength - centerZ) / endFadeDistance);
            if (alpha <= 0.01F || halfSize <= 0.001F) {
                continue;
            }

            // Animate each flash from its own birth time, in order; randomize
            // its timing and position, never the order of its sprite frames.
            ResourceLocation frameTexture = texture;
            if (textures != null) {
                int frame = Math.min(textures.length - 1, Mth.floor(timeInCycle / frameTicks));
                frameTexture = textures[frame];
            }
            if (frameTexture == null) {
                continue;
            }
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(frameTexture));
            float angle = startingAngle + turns * Mth.TWO_PI * life;
            renderPlanarStar(consumer, matrix, normalMatrix,
                    ribbonRight, beamForward,
                    centerX, centerY, centerZ,
                    halfSize, angle, red, green, blue, alpha);
            if (!firstPersonView) {
                renderPlanarStar(consumer, matrix, normalMatrix,
                        companionRight, beamForward,
                        centerX, centerY, centerZ,
                        halfSize, angle, red, green, blue, alpha);
            }
        }
    }

    private static void renderPlanarStar(VertexConsumer consumer, Matrix4f matrix,
                                            Matrix3f normalMatrix,
                                            Vector3f cameraRight, Vector3f cameraUp,
                                            float centerX, float centerY, float centerZ,
                                            float halfSize, float angle,
                                            float red, float green, float blue, float alpha) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        Vector3f rotatedRight = new Vector3f(cameraRight).mul(cos)
                .fma(sin, cameraUp);
        Vector3f rotatedUp = new Vector3f(cameraUp).mul(cos)
                .fma(-sin, cameraRight);
        // Match the ribbon's forward x right normal: (-right.y, right.x, 0).
        // Reversing this order makes stars shade opposite to the beam as it turns.
        Vector3f normal = new Vector3f(cameraUp).cross(cameraRight).normalize();
        normalMatrix.transform(normal).normalize();

        emitDoubleSidedQuad(consumer, matrix, normal,
                centerX - (rotatedRight.x() + rotatedUp.x()) * halfSize,
                centerY - (rotatedRight.y() + rotatedUp.y()) * halfSize,
                centerZ - (rotatedRight.z() + rotatedUp.z()) * halfSize,
                centerX - rotatedRight.x() * halfSize + rotatedUp.x() * halfSize,
                centerY - rotatedRight.y() * halfSize + rotatedUp.y() * halfSize,
                centerZ - rotatedRight.z() * halfSize + rotatedUp.z() * halfSize,
                centerX + (rotatedRight.x() + rotatedUp.x()) * halfSize,
                centerY + (rotatedRight.y() + rotatedUp.y()) * halfSize,
                centerZ + (rotatedRight.z() + rotatedUp.z()) * halfSize,
                centerX + rotatedRight.x() * halfSize - rotatedUp.x() * halfSize,
                centerY + rotatedRight.y() * halfSize - rotatedUp.y() * halfSize,
                centerZ + rotatedRight.z() * halfSize - rotatedUp.z() * halfSize,
                red, green, blue, alpha);
    }

    private static void emitDoubleSidedQuad(VertexConsumer consumer, Matrix4f matrix,
                                            Vector3f normal,
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

    public record Style(int count,
                        float minimumLifetimeTicks, float maximumLifetimeTicks,
                        float minimumDelayTicks, float maximumDelayTicks,
                        float halfSize, float surfaceOffset, float lateralJitter,
                        float minimumScale, float maximumScale,
                        float minimumTurns, float maximumTurns,
                        float beamCoverage, float alpha,
                        float minimumSpeed, float maximumSpeed) {
    }
}
