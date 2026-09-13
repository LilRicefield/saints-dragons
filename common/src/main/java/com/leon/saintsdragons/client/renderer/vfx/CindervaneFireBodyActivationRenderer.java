package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public abstract class CindervaneFireBodyActivationRenderer extends RenderType {
    private static final ResourceLocation[] CIRCLE = new ResourceLocation[12];
    static {
        for (int i = 0; i < CIRCLE.length; i++) {
            CIRCLE[i] = SaintsDragonsCommon.rl("textures/particle/circle_thinning" + i + ".png");
        }
    }

    private static final RenderType SHELL = create("saintsdragons_cindervane_fire_body_sphere",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 16384, false, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new TextureStateShard(SaintsDragonsCommon.rl("textures/particle/blank.png"), false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private CindervaneFireBodyActivationRenderer(String name, VertexFormat format, VertexFormat.Mode mode,
                                           int size, boolean crumbling, boolean sorting, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sorting, setup, clear);
    }

    public static void render(Cindervane dragon, PoseStack poses, MultiBufferSource buffers, float partialTick) {
        if (!dragon.isAlive() || dragon.isInvisible()) return;
        float elapsed = dragon.getFireBodyStartAge(partialTick);
        if (elapsed < 0.0F || elapsed >= 6.0F) return;
        float progress = elapsed / 6.0F;
        float radius = Mth.lerp(1.0F - (1.0F - progress) * (1.0F - progress), 0.75F, 10.0F);
        float fadeIn = Mth.clamp(progress / 0.15F, 0.0F, 1.0F);
        float fadeOut = 1.0F - progress;
        float alpha = 0.45F * fadeIn * fadeOut * fadeOut;
        VertexConsumer buffer = buffers.getBuffer(SHELL);
        poses.pushPose();
        poses.translate(0.0D, dragon.getBbHeight() * 0.5D, 0.0D);
        for (int ring = 0; ring < 16; ring++) {
            float latitude0 = -Mth.HALF_PI + Mth.PI * ring / 16.0F;
            float latitude1 = -Mth.HALF_PI + Mth.PI * (ring + 1) / 16.0F;
            for (int segment = 0; segment < 32; segment++) {
                float longitude0 = Mth.TWO_PI * segment / 32.0F;
                float longitude1 = Mth.TWO_PI * (segment + 1) / 32.0F;
                vertex(buffer, poses.last(), latitude0, longitude0, radius, alpha, 0, 0);
                vertex(buffer, poses.last(), latitude1, longitude0, radius, alpha, 0, 1);
                vertex(buffer, poses.last(), latitude1, longitude1, radius, alpha, 1, 1);
                vertex(buffer, poses.last(), latitude0, longitude1, radius, alpha, 1, 0);
            }
        }
        poses.popPose();
        ResourceLocation texture = CIRCLE[Math.min(CIRCLE.length - 1, (int) (elapsed / 0.5F))];
        MultiBufferSource circleBuffers = ignored -> buffers.getBuffer(BeamRenderTypes.translucent(texture));
        BillboardFlashRenderer.renderQuad(poses, circleBuffers, texture,
                0.0F, dragon.getBbHeight() * 0.5F, 0.0F,
                radius * 1.1F, 0.0F, 1.0F, 0.65F, 0.18F, fadeIn * fadeOut);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float latitude, float longitude,
                               float radius, float alpha, float u, float v) {
        float nx = Mth.cos(latitude) * Mth.cos(longitude);
        float ny = Mth.sin(latitude);
        float nz = Mth.cos(latitude) * Mth.sin(longitude);
        buffer.vertex(pose.pose(), nx * radius, ny * radius, nz * radius)
                .color(1.0F, 0.65F, 0.18F, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(pose.normal(), nx, ny, nz).endVertex();
    }
}
