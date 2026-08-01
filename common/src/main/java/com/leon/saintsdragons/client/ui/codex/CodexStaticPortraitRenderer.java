package com.leon.saintsdragons.client.ui.codex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

final class CodexStaticPortraitRenderer extends GeoObjectRenderer<CodexStaticPortraitRenderer.Portrait> {
    CodexStaticPortraitRenderer() {
        super(new PortraitModel());
    }

    @Override
    public void preRender(PoseStack poseStack, Portrait portrait, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
    }

    @Override
    public void actuallyRender(PoseStack poseStack, Portrait portrait, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        for (GeoBone bone : model.topLevelBones()) {
            renderRecursively(poseStack, portrait, bone, renderType, bufferSource, buffer,
                    true, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    static final class Portrait implements GeoAnimatable {
        private final UUID dragonId;
        private final ResourceLocation model;
        private final ResourceLocation texture;
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        Portrait(UUID dragonId, ResourceLocation model, ResourceLocation texture) {
            this.dragonId = dragonId;
            this.model = model;
            this.texture = texture;
        }

        UUID dragonId() {
            return dragonId;
        }

        ResourceLocation model() {
            return model;
        }

        ResourceLocation texture() {
            return texture;
        }

        boolean matches(ResourceLocation model, ResourceLocation texture) {
            return this.model.equals(model) && this.texture.equals(texture);
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public double getTick(Object object) {
            return 0.0D;
        }
    }

    private static final class PortraitModel extends GeoModel<Portrait> {
        @Override
        public ResourceLocation getModelResource(Portrait portrait) {
            return portrait.model();
        }

        @Override
        public ResourceLocation getTextureResource(Portrait portrait) {
            return portrait.texture();
        }

        @Override
        public ResourceLocation getAnimationResource(Portrait portrait) {
            return portrait.model();
        }
    }
}
