package com.leon.saintsdragons.client.debug;

import com.leon.saintsdragons.common.network.MessageDragonPathDebug;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;

public final class DragonPathDebugRenderer {
    private static final Color SELECTED = new Color(1.0F, 1.0F, 1.0F);
    private static final Color CURRENT = new Color(1.0F, 0.9F, 0.1F);
    private static final Color TRAVERSED = new Color(1.0F, 0.45F, 0.05F);
    private static final Color GROUND_PATH = new Color(0.2F, 1.0F, 0.25F);
    private static final Color AIR_PATH = new Color(0.75F, 0.35F, 1.0F);
    private static final Color SWIM_PATH = new Color(0.1F, 0.85F, 1.0F);
    private static final Color MOVEMENT_TARGET = new Color(1.0F, 0.2F, 0.9F);
    private static final Color SWIM_TARGET = new Color(0.15F, 0.35F, 1.0F);
    private static final Color SWIM_ENDPOINT = new Color(0.85F, 1.0F, 1.0F);
    private static final Color REJECTED_TARGET = new Color(1.0F, 0.05F, 0.05F);
    private static final Color COMBAT_TARGET = new Color(1.0F, 0.2F, 0.1F);

    private DragonPathDebugRenderer() {
    }

    public static void render(PoseStack poseStack, Vec3 cameraPosition) {
        MessageDragonPathDebug snapshot = DragonPathDebugClient.getSnapshot();
        Minecraft minecraft = Minecraft.getInstance();
        if (snapshot == null || minecraft.level == null) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        Entity selected = minecraft.level.getEntity(snapshot.entityId());
        if (selected != null) {
            renderBox(poseStack, consumer, selected.getBoundingBox().inflate(0.05D), SELECTED);
        }

        Color navigationColor = "AIR".equals(snapshot.locomotionMode()) ? AIR_PATH : GROUND_PATH;
        renderPath(
                poseStack,
                consumer,
                snapshot.navigationNodes(),
                snapshot.navigationFirstIndex(),
                snapshot.navigationNextIndex(),
                navigationColor
        );
        renderPath(
                poseStack,
                consumer,
                snapshot.swimNodes(),
                snapshot.swimFirstIndex(),
                snapshot.swimNextIndex(),
                SWIM_PATH
        );

        renderMarker(poseStack, consumer, snapshot.movementTarget(), 0.42D, MOVEMENT_TARGET);
        renderMarker(poseStack, consumer, snapshot.swimTarget(), 0.36D, SWIM_TARGET);
        renderMarker(poseStack, consumer, snapshot.swimEndpoint(), 0.30D, SWIM_ENDPOINT);
        renderMarker(poseStack, consumer, snapshot.rejectedTarget(), 0.48D, REJECTED_TARGET);
        renderMarker(poseStack, consumer, snapshot.combatTarget(), 0.42D, COMBAT_TARGET);

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void renderPath(PoseStack poseStack,
                                   VertexConsumer consumer,
                                   List<Vec3> nodes,
                                   int firstIndex,
                                   int nextIndex,
                                   Color pathColor) {
        for (int i = 0; i < nodes.size(); i++) {
            int absoluteIndex = firstIndex + i;
            Color color = nodeColor(absoluteIndex, nextIndex, pathColor);
            Vec3 node = nodes.get(i);
            renderMarker(poseStack, consumer, node, 0.18D, color);
            if (i > 0) {
                int previousAbsoluteIndex = absoluteIndex - 1;
                Color lineColor = previousAbsoluteIndex < nextIndex ? TRAVERSED : pathColor;
                renderLine(poseStack, consumer, nodes.get(i - 1), node, lineColor);
            }
        }
    }

    private static Color nodeColor(int index, int nextIndex, Color pathColor) {
        if (index < nextIndex) {
            return TRAVERSED;
        }
        if (index == nextIndex) {
            return CURRENT;
        }
        return pathColor;
    }

    private static void renderMarker(PoseStack poseStack,
                                     VertexConsumer consumer,
                                     Vec3 position,
                                     double radius,
                                     Color color) {
        if (position == null) {
            return;
        }
        renderBox(poseStack, consumer, AABB.ofSize(position, radius * 2.0D, radius * 2.0D, radius * 2.0D), color);
    }

    private static void renderBox(PoseStack poseStack, VertexConsumer consumer, AABB box, Color color) {
        LevelRenderer.renderLineBox(
                poseStack,
                consumer,
                box,
                color.red,
                color.green,
                color.blue,
                1.0F
        );
    }

    private static void renderLine(PoseStack poseStack,
                                   VertexConsumer consumer,
                                   Vec3 from,
                                   Vec3 to,
                                   Color color) {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }
        direction = direction.normalize();

        PoseStack.Pose pose = poseStack.last();
        Matrix4f positionMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        consumer.vertex(positionMatrix, (float) from.x, (float) from.y, (float) from.z)
                .color(color.red, color.green, color.blue, 1.0F)
                .normal(normalMatrix, (float) direction.x, (float) direction.y, (float) direction.z)
                .endVertex();
        consumer.vertex(positionMatrix, (float) to.x, (float) to.y, (float) to.z)
                .color(color.red, color.green, color.blue, 1.0F)
                .normal(normalMatrix, (float) direction.x, (float) direction.y, (float) direction.z)
                .endVertex();
    }

    private record Color(float red, float green, float blue) {
    }
}
