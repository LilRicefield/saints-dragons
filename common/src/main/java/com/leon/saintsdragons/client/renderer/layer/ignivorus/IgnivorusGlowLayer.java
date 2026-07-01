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
 * Emissive overlay for Ignivorus that pulses while the dragon is breathing fire
 * or charging a fireball. Mirrors the Raevyx beam glow behavior.
 */
public class IgnivorusGlowLayer extends GeoRenderLayer<Ignivorus> {
    private static final ResourceLocation GLOW_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/ignivorus/ignivorus_glow.png");
    private static final ResourceLocation FEMALE_GLOW_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/ignivorus/ignivorus_glow_female.png");
    private static final ResourceLocation CRIMSON_GLOW_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/ignivorus/crimson_ignivorus_glow.png");

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

        boolean isBreathingFire = animatable.isBreathingFire();
        boolean isChargingFireball = animatable.isChargingFireball();

        if (!isBreathingFire && !isChargingFireball) {
            return;
        }

        float ticks = animatable.tickCount + partialTick;
        float alpha;

        if (isChargingFireball) {
            // Fireball charging glow - intensity scales with charge level
            int chargeLevel = animatable.getFireballChargeLevel();
            float chargeIntensity = chargeLevel / 3.0f; // 0.33, 0.66, 1.0

            // Faster pulse at higher charge levels
            float pulseSpeed = 0.15f + (chargeLevel * 0.1f); // 0.25, 0.35, 0.45
            float pulse = 0.5f + 0.5f * Mth.sin(ticks * pulseSpeed);

            // Base alpha scales with charge, pulse adds variation
            alpha = chargeIntensity * (0.4f + 0.6f * pulse);

            // At max charge, add extra intensity with rapid flicker
            if (chargeLevel == 3) {
                float rapidPulse = 0.8f + 0.2f * Mth.sin(ticks * 0.8f);
                alpha = Math.min(1.0f, alpha * rapidPulse * 1.2f);
            }
        } else {
            // Fire breathing glow - original behavior
            float pulse = 0.5f + 0.5f * Mth.sin(ticks * 0.2f);
            float streamProgress = animatable.getFireBreathProgress() / 40.0f;
            alpha = Mth.clamp(streamProgress, 0.0f, 1.0f) * (0.35f + 0.65f * pulse);
        }

        if (alpha <= 0.01f) {
            return;
        }

        ResourceLocation glowTexture = getGlowTexture(animatable);
        RenderType glowType = RenderType.entityTranslucent(glowTexture);
        VertexConsumer glowBuffer = bufferSource.getBuffer(glowType);

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
                1.0f,
                1.0f,
                1.0f,
                alpha
        );
    }

    private ResourceLocation getGlowTexture(Ignivorus animatable) {
        if (animatable.getTextureVariant() == Ignivorus.VARIANT_CRIMSON) {
            return CRIMSON_GLOW_TEXTURE;
        }
        return animatable.isFemale() ? FEMALE_GLOW_TEXTURE : GLOW_TEXTURE;
    }
}
