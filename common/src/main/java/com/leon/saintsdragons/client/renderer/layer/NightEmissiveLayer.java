package com.leon.saintsdragons.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public abstract class NightEmissiveLayer<T extends LivingEntity & GeoEntity> extends GeoRenderLayer<T> {
    private static final long DUSK_START = 12000L;
    private static final long NIGHT_FULL = 13000L;
    private static final long NIGHT_FADE = 22500L;
    private static final long DAWN_END = 23500L;

    protected NightEmissiveLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(@NotNull PoseStack poseStack,
                       T animatable,
                       BakedGeoModel bakedModel,
                       @NotNull RenderType renderType,
                       @NotNull MultiBufferSource bufferSource,
                       @NotNull VertexConsumer buffer,
                       float partialTick,
                       int packedLight,
                       int packedOverlay) {
        if (animatable.isBaby()) {
            return;
        }

        float alpha = getNightAlpha(animatable);
        if (alpha <= 0.01F) {
            return;
        }

        ResourceLocation texture = getEmissiveTexture(animatable);
        if (texture == null) {
            return;
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
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                alpha
        );
    }

    @Nullable
    protected abstract ResourceLocation getEmissiveTexture(T animatable);

    private static float getNightAlpha(LivingEntity animatable) {
        long dayTime = animatable.level().getDayTime() % 24000L;
        if (dayTime < DUSK_START || dayTime > DAWN_END) {
            return 0.0F;
        }
        if (dayTime < NIGHT_FULL) {
            return Mth.clamp((dayTime - DUSK_START) / (float) (NIGHT_FULL - DUSK_START), 0.0F, 1.0F);
        }
        if (dayTime > NIGHT_FADE) {
            return Mth.clamp((DAWN_END - dayTime) / (float) (DAWN_END - NIGHT_FADE), 0.0F, 1.0F);
        }
        return 1.0F;
    }
}
