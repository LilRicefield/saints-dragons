package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusUltimateAbility;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class IgnivorusSkyfallRaysRenderer {
    private static final int RAY_COUNT = 18;
    private static final float CHARGE_TICKS = 40.0F;
    private static final float COLLAPSE_TICKS = 4.0F;

    private IgnivorusSkyfallRaysRenderer() {
    }

    public static void render(Ignivorus dragon, Vec3 origin, PoseStack poses,
                              MultiBufferSource buffers, float partialTick) {
        if (origin == null || !dragon.isAlive() || dragon.isInvisible()) return;
        float elapsed = dragon.getSkyfallElapsedTicks(partialTick);
        float remaining = IgnivorusUltimateAbility.SKYFALL_EXPLOSION_TICK - elapsed;
        if (elapsed < 0.0F || remaining <= 0.0F || remaining >= CHARGE_TICKS) return;

        float progress = 1.0F - remaining / CHARGE_TICKS;
        float collapse = Mth.clamp(remaining / COLLAPSE_TICKS, 0.0F, 1.0F);
        float intensity = Mth.clamp(progress * 5.0F, 0.0F, 1.0F) * collapse;
        RandomSource random = RandomSource.create(dragon.getUUID().getLeastSignificantBits() ^ 432L);
        VertexConsumer buffer = buffers.getBuffer(RenderType.lightning());
        Vec3 offset = origin.subtract(dragon.getPosition(partialTick));
        poses.pushPose();
        poses.translate(offset.x, offset.y, offset.z);
        for (int i = 0; i < RAY_COUNT; i++) {
            poses.pushPose();
            poses.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            poses.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poses.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F + progress * 35.0F));
            float length = (10.0F + random.nextFloat() * 18.0F) * (0.3F + progress * 0.7F) * collapse;
            float width = (0.7F + random.nextFloat() * 1.1F) * (0.4F + progress * 0.6F);
            float appear = Mth.clamp((progress - i / (float) RAY_COUNT * 0.7F) * 6.0F, 0.0F, 1.0F);
            float alpha = intensity * appear * 0.8F;
            Matrix4f matrix = poses.last().pose();
            face(buffer, matrix, length, -0.8660254F * width, -0.5F * width,
                    0.8660254F * width, -0.5F * width, alpha);
            face(buffer, matrix, length, 0.8660254F * width, -0.5F * width,
                    0.0F, width, alpha);
            face(buffer, matrix, length, 0.0F, width,
                    -0.8660254F * width, -0.5F * width, alpha);
            poses.popPose();
        }
        poses.popPose();
    }

    private static void face(VertexConsumer buffer, Matrix4f matrix, float length,
                             float x1, float z1, float x2, float z2, float alpha) {
        buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(1.0F, 0.88F, 0.55F, alpha).endVertex();
        buffer.vertex(matrix, x1, length, z1).color(1.0F, 0.32F, 0.025F, 0.0F).endVertex();
        buffer.vertex(matrix, x2, length, z2).color(1.0F, 0.32F, 0.025F, 0.0F).endVertex();
        buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(1.0F, 0.88F, 0.55F, alpha).endVertex();
    }
}
