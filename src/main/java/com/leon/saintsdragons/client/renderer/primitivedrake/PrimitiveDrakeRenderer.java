package com.leon.saintsdragons.client.renderer.primitivedrake;

import com.leon.saintsdragons.client.model.primitivedrake.PrimitiveDrakeModel;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;

@OnlyIn(Dist.CLIENT)
public class PrimitiveDrakeRenderer extends GeoEntityRenderer<PrimitiveDrakeEntity> {
    private BakedGeoModel lastBakedModel;
    public PrimitiveDrakeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PrimitiveDrakeModel());
    }
    @Override
    public void preRender(PoseStack poseStack,
                          PrimitiveDrakeEntity entity,
                          BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          VertexConsumer buffer,
                          boolean isReRender,
                          float partialTick,
                          int packedLight,
                          int packedOverlay,
                          float red, float green, float blue, float alpha) {

        float scale = 1.5f;
        poseStack.scale(scale, scale, scale);
        this.shadowRadius = 0.8f * scale;

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
