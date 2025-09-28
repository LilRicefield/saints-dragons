package com.leon.saintsdragons.client.renderer.riftdrake;

import com.leon.saintsdragons.client.model.riftdrake.RiftDrakeModel;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class RiftDrakeRenderer extends GeoEntityRenderer<RiftDrakeEntity> {

    public RiftDrakeRenderer(EntityRendererProvider.Context context) {
        super(context, new RiftDrakeModel());
        this.shadowRadius = 0.8f;
    }

    @Override
    public void preRender(@NotNull PoseStack poseStack,
                          RiftDrakeEntity entity,
                          BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          com.mojang.blaze3d.vertex.VertexConsumer buffer,
                          boolean isReRender,
                          float partialTick,
                          int packedLight,
                          int packedOverlay,
                          float red, float green, float blue, float alpha) {
        float scale = 4.5f;
        poseStack.scale(scale, scale, scale);
        this.shadowRadius = 0.8f * scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
