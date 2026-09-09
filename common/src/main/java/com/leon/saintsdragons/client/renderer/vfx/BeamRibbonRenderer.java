package com.leon.saintsdragons.client.renderer.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BeamRibbonRenderer {
    private static final int MIN_SEGMENTS = 12;
    private static final int MAX_SEGMENTS = 48;

    private BeamRibbonRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource,
                              ResourceLocation texture, float beamLength,
                              float visibility, float ageInTicks,
                              Vec3 beamStartWorld, Vec3 beamEndWorld, Style style,
                              float red, float green, float blue,
                              float widthScale, boolean firstPersonView) {
        if (texture == null || style == null || beamStartWorld == null || beamEndWorld == null
                || beamLength <= 0.002F || visibility <= 0.001F) {
            return;
        }

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        Matrix3f worldToLocal = new Matrix3f(normalMatrix).invert();
        Vec3 midpoint = beamStartWorld.add(beamEndWorld).scale(0.5D);
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 viewDirection = camera.getPosition().subtract(midpoint);
        Vector3f localView = new Vector3f(
                (float) viewDirection.x,
                (float) viewDirection.y,
                (float) viewDirection.z);
        worldToLocal.transform(localView);
        Vector3f fallbackRight = new Vector3f(camera.getLeftVector()).negate();
        worldToLocal.transform(fallbackRight);
        fallbackRight.set(fallbackRight.x(), fallbackRight.y(), 0.0F);
        if (fallbackRight.lengthSquared() <= 1.0E-6F) {
            fallbackRight.set(1.0F, 0.0F, 0.0F);
        }
        fallbackRight.normalize();

        Vector3f localRight = new Vector3f(fallbackRight);
        if (!firstPersonView && localView.lengthSquared() > 1.0E-6F) {
            localView.normalize();
            float radial = Mth.sqrt(localView.x() * localView.x()
                    + localView.y() * localView.y());
            if (radial > 1.0E-5F) {
                Vector3f viewRight = new Vector3f(-localView.y(), localView.x(), 0.0F)
                        .normalize();
                if (viewRight.dot(fallbackRight) < 0.0F) {
                    viewRight.negate();
                }
                float cameraFacingBlend = smoothStep(Mth.clamp(
                        (radial - 0.05F) / 0.15F, 0.0F, 1.0F));
                localRight.lerp(viewRight, cameraFacingBlend).normalize();
            }
        }

        Vector3f localNormal = new Vector3f(-localRight.y, localRight.x, 0.0F);
        normalMatrix.transform(localNormal).normalize();
        Vector3f companionRight = new Vector3f(-localRight.y(), localRight.x(), 0.0F);
        Vector3f companionNormal = new Vector3f(-companionRight.y(), companionRight.x(), 0.0F);
        normalMatrix.transform(companionNormal).normalize();
        // Vanilla bypasses directional lighting; shader packs retain the entity pass.
        VertexConsumer consumer = bufferSource.getBuffer(BeamRenderTypes.translucent(texture));

        float halfWidth = Math.max(0.001F, style.halfWidth() * widthScale) * visibility;
        // A repeat count of two stretches each full texture across half the current beam.
        float tileSpan = 1.0F / Math.max(0.001F, style.textureCycles());
        float tileOverlap = Mth.clamp(style.tileOverlapFraction(), 0.001F, 0.499F);
        float tileStep = tileSpan * (1.0F - tileOverlap);
        float scrollDistance = Mth.frac(Math.max(0.0F, ageInTicks)
                * style.scrollCyclesPerTick()) * tileStep;
        float fadeFraction = Mth.clamp(style.edgeFadeFraction(), 0.001F, 0.499F);
        float baseAlpha = Mth.clamp(style.alpha() * visibility, 0.0F, 1.0F);
        int segments = Mth.clamp(Mth.ceil(beamLength * 0.75F), MIN_SEGMENTS, MAX_SEGMENTS);

        int lastTile = Mth.ceil(1.0F / tileStep) + 1;
        for (int tile = -2; tile <= lastTile; tile++) {
            float tileStart = tile * tileStep + scrollDistance;
            float tileEnd = tileStart + tileSpan;
            float clippedStart = Math.max(0.0F, tileStart);
            float clippedEnd = Math.min(1.0F, tileEnd);
            if (clippedEnd - clippedStart <= 1.0E-6F) {
                continue;
            }

            // Even short tiles need interior vertices; both seam endpoints fade to zero.
            int tileSegments = Math.max(4,
                    Mth.ceil((clippedEnd - clippedStart) * segments));
            for (int segment = 0; segment < tileSegments; segment++) {
                float startProgress = Mth.lerp(segment / (float) tileSegments,
                        clippedStart, clippedEnd);
                float endProgress = Mth.lerp((segment + 1) / (float) tileSegments,
                        clippedStart, clippedEnd);
                float startU = (startProgress - tileStart) / tileSpan;
                float endU = (endProgress - tileStart) / tileSpan;
                float startTileFade = tileEdgeFade(startU, tileOverlap);
                float endTileFade = tileEdgeFade(endU, tileOverlap);
                emitRibbonSegmentPair(consumer, matrix,
                        localNormal, localRight, companionNormal, companionRight,
                        halfWidth, beamLength, startProgress, endProgress,
                        startU, endU, startTileFade, endTileFade,
                        fadeFraction, red, green, blue, baseAlpha,
                        style.companionPlane() && !firstPersonView);
            }
        }
    }

    private static void emitRibbonSegmentPair(VertexConsumer consumer, Matrix4f matrix,
                                              Vector3f normal, Vector3f right,
                                              Vector3f companionNormal, Vector3f companionRight,
                                              float halfWidth, float beamLength,
                                              float startProgress, float endProgress,
                                              float startU, float endU,
                                              float startTileFade, float endTileFade,
                                              float fadeFraction,
                                              float red, float green, float blue,
                                              float baseAlpha, boolean companionPlane) {
        emitRibbonSegment(consumer, matrix, normal, right,
                halfWidth, beamLength, startProgress, endProgress,
                startU, endU, startTileFade, endTileFade,
                fadeFraction, red, green, blue, baseAlpha);
        if (companionPlane) {
            emitRibbonSegment(consumer, matrix, companionNormal, companionRight,
                    halfWidth, beamLength, startProgress, endProgress,
                    startU, endU, startTileFade, endTileFade,
                    fadeFraction, red, green, blue, baseAlpha);
        }
    }

    private static void emitRibbonSegment(VertexConsumer consumer, Matrix4f matrix,
                                          Vector3f normal, Vector3f right,
                                          float halfWidth, float beamLength,
                                          float startProgress, float endProgress,
                                          float startU, float endU,
                                          float startTileFade, float endTileFade,
                                          float fadeFraction,
                                          float red, float green, float blue,
                                          float baseAlpha) {
        if (endProgress - startProgress <= 1.0E-6F) {
            return;
        }

        float startZ = beamLength * startProgress;
        float endZ = beamLength * endProgress;
        float startAlpha = baseAlpha * edgeFade(startProgress, fadeFraction) * startTileFade;
        float endAlpha = baseAlpha * edgeFade(endProgress, fadeFraction) * endTileFade;
        emitQuad(consumer, matrix, normal, right,
                halfWidth, startZ, endZ, startU, endU,
                red, green, blue, startAlpha, endAlpha);
    }

    private static float edgeFade(float progress, float fadeFraction) {
        float fadeIn = smoothStep(Mth.clamp(progress / fadeFraction, 0.0F, 1.0F));
        float fadeOut = smoothStep(Mth.clamp((1.0F - progress) / fadeFraction, 0.0F, 1.0F));
        return fadeIn * fadeOut;
    }

    private static float tileEdgeFade(float textureProgress, float overlapFraction) {
        float fadeIn = smoothStep(Mth.clamp(textureProgress / overlapFraction, 0.0F, 1.0F));
        float fadeOut = smoothStep(Mth.clamp(
                (1.0F - textureProgress) / overlapFraction, 0.0F, 1.0F));
        return fadeIn * fadeOut;
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static void emitQuad(VertexConsumer consumer, Matrix4f matrix, Vector3f normal,
                                 Vector3f right, float halfWidth,
                                 float startZ, float endZ, float startU, float endU,
                                 float red, float green, float blue,
                                 float startAlpha, float endAlpha) {
        float x = right.x() * halfWidth;
        float y = right.y() * halfWidth;

        vertex(consumer, matrix, normal, -x, -y, startZ,
                startU, 1.0F, red, green, blue, startAlpha);
        vertex(consumer, matrix, normal, -x, -y, endZ,
                endU, 1.0F, red, green, blue, endAlpha);
        vertex(consumer, matrix, normal, x, y, endZ,
                endU, 0.0F, red, green, blue, endAlpha);
        vertex(consumer, matrix, normal, x, y, startZ,
                startU, 0.0F, red, green, blue, startAlpha);
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

    public record Style(float halfWidth, float textureCycles,
                        float scrollCyclesPerTick, float edgeFadeFraction,
                        float tileOverlapFraction, float alpha,
                        boolean companionPlane) {
    }
}
