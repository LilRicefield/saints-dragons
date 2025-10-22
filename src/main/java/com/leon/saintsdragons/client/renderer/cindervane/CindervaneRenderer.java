package com.leon.saintsdragons.client.renderer.cindervane;

import com.leon.saintsdragons.client.model.cindervane.CindervaneModel;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CindervaneRenderer extends GeoEntityRenderer<Cindervane> {
    public CindervaneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CindervaneModel());
    }

    @Override
    protected float getDeathMaxRotation(Cindervane entity) {
        // Keep Amphithere upright so custom death animation plays without vanilla flop
        return 0.0F;
    }

    @Override
    public void preRender(PoseStack poseStack,
                          Cindervane entity,
                          BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          VertexConsumer buffer,
                          boolean isReRender,
                          float partialTick,
                          int packedLight,
                          int packedOverlay,
                          float red, float green, float blue, float alpha) {

        float scale = 1.0f;
        poseStack.scale(scale, scale, scale);
        this.shadowRadius = 0.8f * scale;

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
    @Override
    public void render(@NotNull Cindervane entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

    }
}
