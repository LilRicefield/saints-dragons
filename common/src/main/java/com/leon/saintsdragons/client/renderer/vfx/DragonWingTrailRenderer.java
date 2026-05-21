package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class DragonWingTrailRenderer {
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/particle/trail.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);
    private static final float WIDTH = 0.25F;

    private DragonWingTrailRenderer() {
    }

    public static void render(DragonWingTrail trail, MultiBufferSource bufferSource, PoseStack.Pose pose) {
        int count = trail.getPointCount();
        if (count < 2) {
            return;
        }

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);
        Matrix3f normal = pose.normal();
        int light = LightTexture.FULL_BRIGHT;
        int segments = count - 1;

        Vector3f[] points = new Vector3f[count];
        Vector3f[] rightVectors = new Vector3f[count];
        for (int i = 0; i < count; i++) {
            Vec3 world = trail.getPositionAt(i);
            if (world == null) {
                return;
            }
            points[i] = new Vector3f(
                    (float) (world.x - cameraPos.x),
                    (float) (world.y - cameraPos.y),
                    (float) (world.z - cameraPos.z)
            );
        }

        Vector3f fallbackRight = new Vector3f(WIDTH, 0.0F, 0.0F);
        for (int i = 0; i < count; i++) {
            Vector3f direction = new Vector3f();
            if (i > 0 && i < count - 1) {
                addNormalized(direction, points[i - 1], points[i]);
                addNormalized(direction, points[i], points[i + 1]);
            } else if (i == 0) {
                addNormalized(direction, points[0], points[1]);
            } else {
                addNormalized(direction, points[i - 1], points[i]);
            }

            if (direction.lengthSquared() < 1.0E-5F) {
                rightVectors[i] = new Vector3f(fallbackRight);
                continue;
            }
            direction.normalize();

            Vector3f cameraToPoint = new Vector3f(points[i]);
            if (cameraToPoint.lengthSquared() < 1.0E-5F) {
                cameraToPoint.set(0.0F, 0.0F, 1.0F);
            } else {
                cameraToPoint.normalize();
            }
            Vector3f right = new Vector3f(direction).cross(cameraToPoint);
            if (right.lengthSquared() < 1.0E-5F) {
                rightVectors[i] = new Vector3f(fallbackRight);
            } else {
                right.normalize().mul(WIDTH);
                rightVectors[i] = right;
                fallbackRight.set(right);
            }
        }

        for (int i = 0; i < segments; i++) {
            float u1 = i / (float) segments;
            float u2 = (i + 1) / (float) segments;
            float trailFade1 = i / (float) count;
            float trailFade2 = (i + 1) / (float) count;
            float a1 = Mth.clamp(trail.getAlphaAt(i) * trailFade1, 0.0F, 1.0F);
            float a2 = Mth.clamp(trail.getAlphaAt(i + 1) * trailFade2, 0.0F, 1.0F);

            Vector3f p1 = points[i];
            Vector3f p2 = points[i + 1];
            Vector3f r1 = rightVectors[i];
            Vector3f r2 = rightVectors[i + 1];

            vertex(consumer, normal, new Vector3f(p1).add(r1), u1, 0.0F, a1, light);
            vertex(consumer, normal, new Vector3f(p1).sub(r1), u1, 1.0F, a1, light);
            vertex(consumer, normal, new Vector3f(p2).sub(r2), u2, 1.0F, a2, light);
            vertex(consumer, normal, new Vector3f(p2).add(r2), u2, 0.0F, a2, light);

            vertex(consumer, normal, new Vector3f(p2).add(r2), u2, 0.0F, a2, light);
            vertex(consumer, normal, new Vector3f(p2).sub(r2), u2, 1.0F, a2, light);
            vertex(consumer, normal, new Vector3f(p1).sub(r1), u1, 1.0F, a1, light);
            vertex(consumer, normal, new Vector3f(p1).add(r1), u1, 0.0F, a1, light);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix3f normal, Vector3f pos,
                               float u, float v, float alpha, int light) {
        normal.transform(pos);
        consumer.vertex(pos.x(), pos.y(), pos.z())
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 1.0F, 0.0F, 0.0F)
                .endVertex();
    }

    private static void addNormalized(Vector3f target, Vector3f from, Vector3f to) {
        Vector3f direction = new Vector3f(to).sub(from);
        if (direction.lengthSquared() >= 1.0E-5F) {
            target.add(direction.normalize());
        }
    }
}
