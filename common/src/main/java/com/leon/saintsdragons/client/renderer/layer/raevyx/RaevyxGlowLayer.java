package com.leon.saintsdragons.client.renderer.layer.raevyx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Simple emissive glow layer that pulses while the rider holds the beam key (G).
 * Uses the dedicated raevyx_glow texture and RenderType.eyes for fullbright rendering.
 */
public class RaevyxGlowLayer extends GeoRenderLayer<Raevyx> {
    private static final ResourceLocation GLOW_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_glow.png");
    private static final ResourceLocation FEMALE_GLOW_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_female_glow.png");

    public RaevyxGlowLayer(GeoRenderer<Raevyx> renderer) {
        super(renderer);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, Raevyx animatable, BakedGeoModel bakedModel,
                       @NotNull RenderType renderType, @NotNull MultiBufferSource bufferSource,
                       @NotNull VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (!animatable.isBeamGlowActive()) {
            return;
        }

        float ticks = animatable.tickCount + partialTick;
        float pulseBase = 0.0F;  // Start from fully transparent
        float pulseSwing = 1.0F; // Go to fully opaque

        // Faster pulse: 0.25F = ~1.25 seconds per cycle (was 0.12F = ~2.6 seconds)
        float pulse = pulseBase + pulseSwing * (0.5f + 0.5f * Mth.sin(ticks * 0.25F));

        ResourceLocation texture = animatable.isFemale() ? FEMALE_GLOW_TEXTURE : GLOW_TEXTURE;
        RenderType glowType = RenderType.entityTranslucent(texture);
        VertexConsumer glowBuffer = bufferSource.getBuffer(glowType);

        getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                glowType,
                glowBuffer,
                partialTick,
                0xF000F0,  // Max light = fullbright emissive
                OverlayTexture.NO_OVERLAY,
                1.0f,
                1.0f,
                1.0f,
                pulse  // Alpha for pulsation
        );
    }
}
