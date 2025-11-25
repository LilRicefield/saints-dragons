package com.leon.saintsdragons.client.renderer.layer.ignivorus;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
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
 * Emissive overlay for Ignivorus that pulses while the dragon is breathing fire.
 * Mirrors the Raevyx beam glow behavior but keys off the fire-breathing state.
 */
public class IgnivorusGlowLayer extends GeoRenderLayer<Ignivorus> {
    private static final ResourceLocation GLOW_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/ignivorus/ignivorus_glow.png");
    private static final ResourceLocation FEMALE_GLOW_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/ignivorus/ignivorus_glow_female.png");

    public IgnivorusGlowLayer(GeoRenderer<Ignivorus> renderer) {
        super(renderer);
    }

    @Override
    public void render(@NotNull PoseStack poseStack,
                       Ignivorus animatable,
                       BakedGeoModel bakedModel,
                       @NotNull RenderType renderType,
                       @NotNull MultiBufferSource bufferSource,
                       @NotNull VertexConsumer buffer,
                       float partialTick,
                       int packedLight,
                       int packedOverlay) {

        if (!animatable.isBreathingFire()) {
            return;
        }

        float ticks = animatable.tickCount + partialTick;
        float pulse = 0.5f + 0.5f * Mth.sin(ticks * 0.2f); // Slightly slower pulse than Raevyx
        float streamProgress = animatable.getFireBreathProgress() / 40.0f;
        float alpha = Mth.clamp(streamProgress, 0.0f, 1.0f) * (0.35f + 0.65f * pulse);

        if (alpha <= 0.01f) {
            return;
        }

        ResourceLocation glowTexture = animatable.isFemale() ? FEMALE_GLOW_TEXTURE : GLOW_TEXTURE;
        RenderType glowType = RenderType.entityTranslucent(glowTexture);
        VertexConsumer glowBuffer = bufferSource.getBuffer(glowType);

        int packedColor = (int)(alpha * 255) << 24 | 0xFFFFFF;
        getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                glowType,
                glowBuffer,
                partialTick,
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                packedColor
        );
    }
}
