package com.leon.saintsdragons.client.renderer.amphithere;

import com.leon.saintsdragons.client.model.amphithere.AmphithereModel;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AmphithereRenderer extends GeoEntityRenderer<AmphithereEntity> {
    public AmphithereRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AmphithereModel());
    }

    @Override
    protected float getDeathMaxRotation(AmphithereEntity entity) {
        // Keep Amphithere upright so custom death animation plays without vanilla flop
        return 0.0F;
    }

    @Override
    public void preRender(PoseStack poseStack,
                          AmphithereEntity entity,
                          BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          VertexConsumer buffer,
                          boolean isReRender,
                          float partialTick,
                          int packedLight,
                          int packedOverlay,
                          float red, float green, float blue, float alpha) {

        float scale = 3.5f;
        poseStack.scale(scale, scale, scale);
        this.shadowRadius = 0.8f * scale;

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
    @Override
    public void render(@NotNull AmphithereEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

    }
}
