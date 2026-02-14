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
 * Emissive glow layer for Raevyx beam glow.
 */
public class RaevyxGlowLayer extends GeoRenderLayer<Raevyx> {
    private static final ResourceLocation GLOW_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_glow.png");
    private static final ResourceLocation FEMALE_GLOW_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_female_glow.png");
    private static final ResourceLocation NIGHT_GOLD_GLOW_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_night_gold_glow.png");
    private static final ResourceLocation NIGHT_GOLD_FEMALE_GLOW_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_night_gold_female_glow.png");

    public RaevyxGlowLayer(GeoRenderer<Raevyx> renderer) {
        super(renderer);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, Raevyx animatable, BakedGeoModel bakedModel,
                       @NotNull RenderType renderType, @NotNull MultiBufferSource bufferSource,
                       @NotNull VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        boolean beamGlow = animatable.isBeamGlowActive();
        if (!beamGlow) {
            return;
        }

        float ticks = animatable.tickCount + partialTick;
        float pulseBase = 0.0F;
        float pulseSwing = 1.0F;
        float pulse = pulseBase + pulseSwing * (0.5f + 0.5f * Mth.sin(ticks * 0.12F));
        float alpha = pulse;
        boolean nightGold = animatable.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        ResourceLocation texture;
        if (nightGold) {
            texture = animatable.isFemale() ? NIGHT_GOLD_FEMALE_GLOW_TEXTURE : NIGHT_GOLD_GLOW_TEXTURE;
        } else {
            texture = animatable.isFemale() ? FEMALE_GLOW_TEXTURE : GLOW_TEXTURE;
        }

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
                alpha
        );
    }

}
