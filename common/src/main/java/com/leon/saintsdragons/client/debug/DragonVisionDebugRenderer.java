package com.leon.saintsdragons.client.debug;

import com.leon.saintsdragons.common.network.MessageDragonVisionDebug;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonVisionProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class DragonVisionDebugRenderer {
    private static final int CYAN = 0x35E7EB, BLUE = 0x477EFF, GREEN = 0x45FF65,
            RED = 0xFF4545, AMBER = 0xFFBB38, GRAY = 0xAAAAAA, MAGENTA = 0xF05BFF;

    private DragonVisionDebugRenderer() {}

    public static void render(PoseStack stack, Vec3 camera) {
        var snapshot = DragonVisionDebugClient.getSnapshot();
        Minecraft minecraft = Minecraft.getInstance();
        if (snapshot == null || minecraft.level == null) return;
        var buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        stack.pushPose();
        stack.translate(-camera.x, -camera.y, -camera.z);
        if (snapshot.shows(MessageDragonVisionDebug.SHAPE)) {
            box(stack, lines, snapshot.eye(), 0.15, CYAN);
            line(stack, lines, snapshot.eye(), snapshot.eye().add(snapshot.forward().scale(4)), 0xFFFFFF);
            boundary(stack, lines, snapshot, false, CYAN);
            boundary(stack, lines, snapshot, true, BLUE);
        }
        if (snapshot.shows(MessageDragonVisionDebug.RAYS)) {
            for (var ray : snapshot.rays()) {
                line(stack, lines, ray.from(), ray.to(), ray.clear() ? GREEN : RED);
                if (!ray.clear()) box(stack, lines, ray.to(), 0.08, RED);
            }
        }
        if (snapshot.shows(MessageDragonVisionDebug.AWARENESS)) {
            Vec3 right = DragonVisionProfile.right(snapshot.forward());
            for (var marker : snapshot.markers()) {
                int color = marker.age() > 4 || !"visible".equals(marker.reason()) ? GRAY
                        : marker.recognized() ? GREEN : AMBER;
                box(stack, lines, marker.position(), 0.2, color);
                Vec3 start = marker.position().add(0, 0.65, 0).subtract(right.scale(0.6));
                line(stack, lines, start, start.add(right.scale(1.2)), GRAY);
                line(stack, lines, start.add(0, 0.04, 0), start.add(0, 0.04, 0)
                        .add(right.scale(1.2 * marker.awareness())), color);
            }
        }
        var projectile = snapshot.projectile();
        if (snapshot.shows(MessageDragonVisionDebug.PROJECTILES) && projectile != null) {
            line(stack, lines, projectile.previous(), projectile.observed(), GREEN);
            line(stack, lines, projectile.observed(), projectile.predicted(), AMBER);
            line(stack, lines, projectile.observed(), projectile.origin(), MAGENTA);
            box(stack, lines, projectile.observed(), 0.16, GREEN);
            sphere(stack, lines, projectile.origin(), projectile.uncertainty(), MAGENTA);
        }
        stack.popPose();
        buffers.endBatch(RenderType.lines());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        if (snapshot.shows(MessageDragonVisionDebug.AWARENESS)) {
            for (var marker : snapshot.markers()) {
                Vec3 p = marker.position();
                String text = Math.round(marker.awareness() * 100) + "% "
                        + (marker.recognized() ? "recognized " : "") + marker.reason() + " " + marker.age() + "t";
                DebugRenderer.renderFloatingText(stack, buffers, text, p.x, p.y + 1, p.z, 0xFFFFFFFF);
            }
        }
        if (snapshot.shows(MessageDragonVisionDebug.PROJECTILES) && projectile != null) {
            Vec3 p = projectile.origin();
            DebugRenderer.renderFloatingText(stack, buffers, String.format(Locale.ROOT, "Estimated source: %.0f%% +/-%.1fm",
                    projectile.confidence() * 100, projectile.uncertainty()), p.x, p.y + 1, p.z, 0xFFFF88FF);
        }
        buffers.endBatch();
    }

    private static void boundary(PoseStack stack, VertexConsumer lines, MessageDragonVisionDebug s,
                                 boolean retained, int color) {
        for (int depth = 1; depth <= 8; depth++) {
            double distance = s.profile().range() * depth / 8;
            double previous = s.profile().range() * (depth - 1) / 8;
            double horizontal = s.profile().horizontal(distance, retained);
            double vertical = s.profile().vertical(distance, retained);
            if (depth % 2 == 0) {
                for (int step = 0; step < 24; step++) {
                    double a = -1 + step / 12.0, b = -1 + (step + 1) / 12.0;
                    for (int side : new int[]{-1, 1}) {
                        line(stack, lines, point(s, distance, a * horizontal, side * vertical),
                                point(s, distance, b * horizontal, side * vertical), color);
                        line(stack, lines, point(s, distance, side * horizontal, a * vertical),
                                point(s, distance, side * horizontal, b * vertical), color);
                    }
                }
            }
            for (int yaw : new int[]{-1, 0, 1}) {
                for (int pitch : new int[]{-1, 0, 1}) {
                    if (yaw == 0 && pitch == 0) continue;
                    line(stack, lines, point(s, previous, yaw * s.profile().horizontal(previous, retained),
                                    pitch * s.profile().vertical(previous, retained)),
                            point(s, distance, yaw * horizontal, pitch * vertical), color);
                }
            }
        }
    }

    private static Vec3 point(MessageDragonVisionDebug s, double distance, double yaw, double pitch) {
        return DragonVisionProfile.point(s.eye(), s.forward(), distance, yaw, pitch);
    }

    private static void sphere(PoseStack stack, VertexConsumer lines, Vec3 center, double radius, int color) {
        for (int plane = 0; plane < 3; plane++) {
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI / 24, b = (i + 1) * Math.PI / 24;
                line(stack, lines, center.add(circle(plane, a, radius)), center.add(circle(plane, b, radius)), color);
            }
        }
    }

    private static Vec3 circle(int plane, double angle, double radius) {
        double a = Math.cos(angle) * radius, b = Math.sin(angle) * radius;
        return plane == 0 ? new Vec3(a, 0, b) : plane == 1 ? new Vec3(a, b, 0) : new Vec3(0, a, b);
    }

    private static void box(PoseStack stack, VertexConsumer lines, Vec3 at, double radius, int color) {
        LevelRenderer.renderLineBox(stack, lines, AABB.ofSize(at, radius * 2, radius * 2, radius * 2),
                (color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
    }

    private static void line(PoseStack stack, VertexConsumer lines, Vec3 from, Vec3 to, int color) {
        Vec3 direction = to.subtract(from).normalize();
        if (direction.lengthSqr() < 1.0E-8) return;
        var pose = stack.last();
        for (Vec3 p : new Vec3[]{from, to}) {
            lines.vertex(pose.pose(), (float)p.x, (float)p.y, (float)p.z)
                    .color(color >> 16 & 255, color >> 8 & 255, color & 255, 255)
                    .normal(pose.normal(), (float)direction.x, (float)direction.y, (float)direction.z).endVertex();
        }
    }
}
