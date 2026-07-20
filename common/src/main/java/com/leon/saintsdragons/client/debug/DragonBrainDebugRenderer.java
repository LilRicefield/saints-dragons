package com.leon.saintsdragons.client.debug;

import com.leon.saintsdragons.common.network.MessageDragonBrainDebug;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class DragonBrainDebugRenderer {
    private static final Color ATTACK_TARGET = new Color(1.0F, 0.12F, 0.08F);
    private static final Color MOB_TARGET = new Color(1.0F, 0.55F, 0.08F);
    private static final Color WALK_TARGET = new Color(1.0F, 0.85F, 0.1F);
    private static final Color LOOK_TARGET = new Color(0.25F, 1.0F, 0.95F);
    private static final Color MOVEMENT_INTENT = new Color(1.0F, 0.2F, 0.9F);
    private static final Color HOME = new Color(0.35F, 1.0F, 0.35F);
    private static final Color LAST_SEEN_TARGET = new Color(1.0F, 0.45F, 0.2F);
    private static final Color INVESTIGATION_TARGET = new Color(0.95F, 0.35F, 1.0F);
    private static final Color HEARD_STIMULUS = new Color(1.0F, 0.95F, 0.3F);
    private static final Color HEARD_TARGET = new Color(0.4F, 1.0F, 0.55F);
    private static final Color OTHER = new Color(0.75F, 0.65F, 1.0F);

    private DragonBrainDebugRenderer() {
    }

    public static void render(PoseStack poseStack, Vec3 cameraPosition) {
        MessageDragonBrainDebug snapshot = DragonBrainDebugClient.getSnapshot();
        Minecraft minecraft = Minecraft.getInstance();
        if (snapshot == null || minecraft.level == null) {
            return;
        }

        Entity dragon = minecraft.level.getEntity(snapshot.entityId());
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        Vec3 origin = dragon == null ? null : dragon.getBoundingBox().getCenter();
        for (MessageDragonBrainDebug.Marker marker : snapshot.markers()) {
            Vec3 position = marker.position();
            if (position == null) {
                continue;
            }
            Color color = color(marker.kind());
            Entity markedEntity = marker.entityId() < 0 ? null : minecraft.level.getEntity(marker.entityId());
            if (markedEntity != null) {
                LevelRenderer.renderLineBox(
                        poseStack, consumer, markedEntity.getBoundingBox().inflate(0.08D),
                        color.red(), color.green(), color.blue(), 1.0F);
            } else {
                double radius = "ATTACK_TARGET".equals(marker.kind()) ? 0.45D : 0.28D;
                LevelRenderer.renderLineBox(
                        poseStack, consumer,
                        AABB.ofSize(position, radius * 2.0D, radius * 2.0D, radius * 2.0D),
                        color.red(), color.green(), color.blue(), 1.0F);
            }
            if (origin != null) {
                renderLine(poseStack, consumer, origin, position, color);
            }
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static Color color(String kind) {
        return switch (kind) {
            case "ATTACK_TARGET" -> ATTACK_TARGET;
            case "MOB_TARGET" -> MOB_TARGET;
            case "WALK_TARGET" -> WALK_TARGET;
            case "LOOK_TARGET" -> LOOK_TARGET;
            case "MOVEMENT_INTENT", "TACTICAL_LANDING" -> MOVEMENT_INTENT;
            case "HOME", "ROOST_SLEEP" -> HOME;
            case "LAST_SEEN_TARGET" -> LAST_SEEN_TARGET;
            case "INVESTIGATION_TARGET" -> INVESTIGATION_TARGET;
            case "HEARD_STIMULUS" -> HEARD_STIMULUS;
            case "HEARD_TARGET" -> HEARD_TARGET;
            default -> OTHER;
        };
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
        Matrix4f positions = pose.pose();
        Matrix3f normals = pose.normal();
        consumer.vertex(positions, (float)from.x, (float)from.y, (float)from.z)
                .color(color.red(), color.green(), color.blue(), 1.0F)
                .normal(normals, (float)direction.x, (float)direction.y, (float)direction.z)
                .endVertex();
        consumer.vertex(positions, (float)to.x, (float)to.y, (float)to.z)
                .color(color.red(), color.green(), color.blue(), 1.0F)
                .normal(normals, (float)direction.x, (float)direction.y, (float)direction.z)
                .endVertex();
    }

    private record Color(float red, float green, float blue) {
    }
}
